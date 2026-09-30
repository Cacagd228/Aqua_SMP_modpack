package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.api.misc.ManaHelper;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblyGate;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblyNbt;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblyRecipe;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblyRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getVec3;

/**
 * Вливание Эфира — pours aether into the workpiece.
 *
 * <p>Stack: {@code [Vec3 depot, Number mana]}, with the depot on top
 * ({@code args[0]}), matching {@code OpChargeVessel}.
 *
 * <p><b>This rune does not append a step id.</b> The other four append a fixed
 * symbol, but how much aether goes in is chosen by the player at cast time, so
 * a step id could not describe it — a recipe could never say "then 5000 mana",
 * only "then <something> mana". So the aether lives purely as a quantity on the
 * item ({@code meowhex:mana}) and the recipe's {@code mana} field is the gate.
 *
 * <p>That makes this rune able to <em>finish</em> an assembly as well as feed
 * one: once the accumulated aether reaches a recipe's requirement and the step
 * list already matches that recipe exactly, the result is produced. So both
 * orderings work — the activators last, or the infusion last — which is what
 * makes a recipe like "hammer, saw, then infuse 5000" and "hammer, saw" +
 * infusion read the same to the player.
 */
public class OpInfuseAether implements SpellAction {

    public static final OpInfuseAether INSTANCE = new OpInfuseAether();

    /** Signature wqqqqqadqw (EAST). */
    public static final at.petrak.hexcasting.api.casting.math.HexPattern PATTERN =
            at.petrak.hexcasting.api.casting.math.HexPattern.fromAngles("wqqqqqadqw",
                    at.petrak.hexcasting.api.casting.math.HexDir.EAST);

    /** Ceiling per cast, so one spell cannot pour a whole player's reserve. */
    private static final long MAX_MANA_PER_CAST = 10_000L;
    /** Commission on top, as in OpChargeVessel. */
    private static final double COMMISSION_RATE = 1.1;
    private static final long BASE_COST = MediaConstants.DUST_UNIT;

    private OpInfuseAether() {
    }

    @Override
    public int getArgc() {
        return 2;
    }

    @Override
    public boolean hasCastingSound(CastingEnvironment env) {
        return true;
    }

    @Override
    public boolean awardsCastingStat(CastingEnvironment env) {
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation cont) {
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env,
                                      net.minecraft.nbt.CompoundTag userdata) {
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls
                .executeWithUserdata(this, args, env, userdata);
    }

    @Override
    public Result execute(List<? extends Iota> args, CastingEnvironment env) {
        // Feature switch, before any state is read: see AssemblyGate for why
        // this rune stays registered while disabled.
        if (!AssemblyGate.enabled()) {
            sneakyThrow(new AssemblyMishap.Disabled());
        }

        Vec3 depotVec;
        long requestedMana;
        try {
            depotVec = getVec3(args, 0, getArgc());
            requestedMana = (long) ((DoubleIota) args.get(1)).getDouble();
        } catch (Throwable t) {
            return null; // default mishap for a bad iota
        }

        if (requestedMana <= 0 || requestedMana > MAX_MANA_PER_CAST) {
            sneakyThrow(new AssemblyMishap.BadAmount(requestedMana, MAX_MANA_PER_CAST));
        }

        try {
            env.assertVecInRange(depotVec);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
        if (!env.isVecInWorld(depotVec)) {
            sneakyThrow(new AssemblyMishap.NotAWorkpiece());
        }

        ServerLevel world = env.getWorld();
        BlockPos pos = BlockPos.containing(depotVec);
        if (!DepotHelper.isDepot(world, pos)) {
            sneakyThrow(AssemblyMishap.badDepot(pos));
        }
        ItemStack workpiece = DepotHelper.getStack(world, pos);
        if (workpiece == null || workpiece.isEmpty()) {
            sneakyThrow(new AssemblyMishap.NotAWorkpiece());
        }

        // The infusion is only meaningful for a workpiece some recipe starts
        // from. A bare item is a common typo — the player pointed at the wrong
        // depot — and quietly writing aether onto it would look like it worked.
        if (AssemblyRecipes.forInput(workpiece).isEmpty()) {
            sneakyThrow(new AssemblyMishap.NotAWorkpiece());
        }

        long playerCostMana = (long) Math.ceil(requestedMana * COMMISSION_RATE);
        long playerCostMedia = playerCostMana * AssemblyNbt.MANA_TO_MEDIA;

        // A non-player caster (a circle, say) has no mana pool to draw on, so
        // the balance check has to be skipped or it would NPE. Such a caster
        // simply gets the aether free, which matches how other mana-touching
        // ops behave in circles.
        ServerPlayer caster = env.getCaster();
        if (caster != null && !ManaHelper.hasInfiniteMana(caster)) {
            double currentMana = ManaHelper.getMana(caster);
            double costMana = ManaHelper.manaCostOfMedia(caster, playerCostMedia);
            if (currentMana < costMana) {
                sneakyThrow(new AssemblyMishap.NotEnoughAether((long) currentMana, playerCostMana));
            }
        }

        // Aether scales with the pile: pouring into 64 parts costs 64x.
        int units = workpiece.getCount();
        long totalMana = requestedMana * units;
        long totalMedia = totalMana * AssemblyNbt.MANA_TO_MEDIA;

        Vec3 centre = Vec3.atCenterOf(pos);
        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(centre, 1.2, 40),
                ParticleSpray.burst(centre, 0.6, 20)
        );

        // The framework deducts `playerCostMedia` from the caster; the item is
        // credited the full amount, and the difference is the commission.
        return new Result(
                new InfuseSpell(pos, totalMedia, units),
                playerCostMedia + BASE_COST,
                particles,
                0
        );
    }

    public static class InfuseSpell implements RenderedSpell {
        private final BlockPos pos;
        private final long media;
        private final int units;

        public InfuseSpell(BlockPos pos, long media, int units) {
            this.pos = pos.immutable();
            this.media = media;
            this.units = units;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            ItemStack workpiece = DepotHelper.getStack(world, pos);
            if (workpiece == null || workpiece.isEmpty()) {
                return;
            }
            long now = AssemblyNbt.mana(workpiece) + media;
            List<String> steps = AssemblyNbt.steps(workpiece);

            // This infusion may be what completes the assembly: the step list is
            // already a full recipe and the aether now meets its requirement.
            AssemblyRecipe done = finishedRecipe(workpiece, steps, now);
            if (done != null) {
                ItemStack result = done.assemble(
                        new AssemblyRecipe.InputHolder(workpiece, now), world.registryAccess());
                AssemblyNbt.clear(result);
                AssemblyNbt.setMana(result, now);
                DepotHelper.setStack(world, pos, result);
                return;
            }

            ItemStack updated = workpiece.copy();
            AssemblyNbt.setMana(updated, now);
            DepotHelper.setStack(world, pos, updated);
        }

        /**
         * The recipe this workpiece has just become complete for, or null.
         *
         * <p>A null step list can never match, since every recipe has at least
         * one step — which is the right answer: infusing aether into a
         * workpiece nobody has started doing anything to yet should bank the
         * aether, not fire off a result.
         */
        private AssemblyRecipe finishedRecipe(ItemStack workpiece,
                                              List<String> steps, long mana) {
            if (steps.isEmpty()) {
                return null;
            }
            long haveMana = AssemblyNbt.manaToManaUnits(mana);
            AssemblyRecipe best = null;
            for (AssemblyRecipe recipe : AssemblyRecipes.all()) {
                if (!recipe.steps().equals(steps) || !recipe.input().test(workpiece)) {
                    continue;
                }
                if (haveMana < recipe.mana()) {
                    continue;
                }
                if (best == null || recipe.steps().size() > best.steps().size()) {
                    best = recipe;
                }
            }
            return best;
        }
    }
}

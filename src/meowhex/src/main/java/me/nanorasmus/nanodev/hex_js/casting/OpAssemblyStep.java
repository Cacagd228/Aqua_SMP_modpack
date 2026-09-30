package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.misc.MediaConstants;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblyGate;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblyNbt;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblyRecipe;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblyRecipes;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblySteps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getVec3;

/**
 * The four activator runes: Слияние Сущности, Поглощение Даров, Пощищение
 * Сути and Вбирание Жертвы.
 *
 * <p>One class, four singletons, differing only in the step id they write. The
 * user's framing — "4 different types, but not hugely important" — is the whole
 * design: a rune is a marker pen, and the recipe decides what the marks mean.
 * Keeping them as one class means a fix to the assembly mechanic lands on all
 * four at once.
 *
 * <p>Stack, per the fork's convention that {@code args[0]} is the
 * <em>top</em> of the stack (as {@code OpOvidsDistillation} does): the top iota
 * is depot <b>B</b>, the consumable, and the one below is depot <b>A</b>, the
 * workpiece. So the pattern reads bottom-to-top as [workpiece, consumable].
 *
 * <p>Execution splits the way the framework requires: {@link #execute} is pure
 * and validates everything, and only {@code cast} mutates the world. The
 * validation order matters — the recipe check happens before the consumable is
 * touched, so a rejected step leaves both depots exactly as they were.
 */
public class OpAssemblyStep implements SpellAction {

    /** Слияние Сущности. Signature qaqwawaq (EAST). */
    public static final OpAssemblyStep MERGE = new OpAssemblyStep(AssemblySteps.MERGE);
    /** Поглощение Даров. Signature qaqwawaqa (EAST). */
    public static final OpAssemblyStep ABSORB = new OpAssemblyStep(AssemblySteps.ABSORB);
    /** Пощищение Сути. Signature qaqwawaqw (EAST). */
    public static final OpAssemblyStep PURIFY = new OpAssemblyStep(AssemblySteps.PURIFY);
    /** Вбирание Жертвы. Signature qaqwawaqq (EAST). */
    public static final OpAssemblyStep SACRIFICE = new OpAssemblyStep(AssemblySteps.SACRIFICE);

    /** Base 5 dust, plus one extra dust per 16 items processed. */
    private static final long BASE_COST = 5L * MediaConstants.DUST_UNIT;

    private final String step;

    private OpAssemblyStep(String step) {
        this.step = step;
    }

    /** The step id this rune writes into a workpiece. */
    public String step() {
        return step;
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
        // Feature switch, checked before anything else so a disabled build
        // refuses cleanly instead of half-running. The rune stays registered
        // either way — see AssemblyGate for why unregistering it would crash
        // the Patchouli book.
        if (!AssemblyGate.enabled()) {
            sneakyThrow(new AssemblyMishap.Disabled());
        }

        // args[0] is the top of the stack: depot B, the consumable.
        // args[1] is below it: depot A, the workpiece.
        Vec3 consumableVec;
        Vec3 workpieceVec;
        try {
            consumableVec = getVec3(args, 0, getArgc());
            workpieceVec = getVec3(args, 1, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        try {
            env.assertVecInRange(consumableVec);
            env.assertVecInRange(workpieceVec);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        if (!env.isVecInWorld(consumableVec) || !env.isVecInWorld(workpieceVec)) {
            sneakyThrow(new AssemblyMishap.NotAWorkpiece());
        }

        ServerLevel world = env.getWorld();
        BlockPos consumablePos = BlockPos.containing(consumableVec);
        BlockPos workpiecePos = BlockPos.containing(workpieceVec);

        if (workpiecePos.equals(consumablePos)) {
            sneakyThrow(new AssemblyMishap.NotAWorkpiece());
        }
        if (!DepotHelper.isDepot(world, consumablePos)) {
            sneakyThrow(AssemblyMishap.badDepot(consumablePos));
        }
        if (!DepotHelper.isDepot(world, workpiecePos)) {
            sneakyThrow(AssemblyMishap.badDepot(workpiecePos));
        }

        ItemStack consumable = DepotHelper.getStack(world, consumablePos);
        ItemStack workpiece = DepotHelper.getStack(world, workpiecePos);
        if (workpiece == null || workpiece.isEmpty()) {
            sneakyThrow(new AssemblyMishap.NotAWorkpiece());
        }
        if (consumable == null || consumable.isEmpty()) {
            sneakyThrow(new AssemblyMishap.NotAWorkpiece());
        }

        // One unit of consumable per unit of workpiece. A short pile is refused
        // rather than partially applied, so the two depots never disagree
        // about how many parts moved.
        if (consumable.getCount() < workpiece.getCount()) {
            sneakyThrow(new AssemblyMishap.NotEnoughConsumable(consumable.getCount(), workpiece.getCount()));
        }
        int units = workpiece.getCount();

        List<String> existing = AssemblyNbt.steps(workpiece);

        // Throws BadStep when the new sequence leads nowhere. Nothing has been
        // written or consumed yet, so a rejection is a clean no-op.
        AssemblyRecipes.Advance outcome = AssemblyRecipes.advance(workpiece, existing, step);

        if (outcome.finished() && outcome.recipe() != null) {
            // Only the final step is gated on aether. Intermediate steps are
            // free to overcharge, and the recipe's own `mana` is the only
            // thing that decides whether the assembly is allowed to finish.
            long have = AssemblyNbt.manaToManaUnits(AssemblyNbt.mana(workpiece));
            long needed = outcome.recipe().mana();
            if (have < needed) {
                sneakyThrow(new AssemblyMishap.NotEnoughAether(have, needed));
            }
        }

        long cost = BASE_COST + BASE_COST / 4L * (units / 16);

        Vec3 from = Vec3.atCenterOf(consumablePos);
        Vec3 to = Vec3.atCenterOf(workpiecePos);
        List<ParticleSpray> particles = List.of(
                ParticleSpray.burst(to, 1.0, 40),
                ParticleSpray.cloud(from, 0.5, 20),
                ParticleSpray.cloud(to, 0.5, 20)
        );

        return new Result(
                new AssemblyStepSpell(workpiecePos, consumablePos, step, units),
                cost,
                particles,
                0
        );
    }

    /**
     * Applies the step. Re-validates against current depot contents before
     * writing, because between {@code execute} and {@code cast} the world can
     * have changed underneath us — the same discipline
     * {@code OpOvidsDistillation.DepotSpell} uses.
     */
    public static class AssemblyStepSpell implements RenderedSpell {
        private final BlockPos workpiecePos;
        private final BlockPos consumablePos;
        private final String step;
        private final int units;

        public AssemblyStepSpell(BlockPos workpiecePos, BlockPos consumablePos, String step, int units) {
            this.workpiecePos = workpiecePos.immutable();
            this.consumablePos = consumablePos.immutable();
            this.step = step;
            this.units = units;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            ItemStack workpiece = DepotHelper.getStack(world, workpiecePos);
            ItemStack consumable = DepotHelper.getStack(world, consumablePos);
            if (workpiece == null || workpiece.isEmpty() || consumable == null || consumable.isEmpty()) {
                return;
            }
            if (consumable.getCount() < units) {
                return;
            }

            // Recompute against what is actually in the depots now; if the
            // sequence no longer validates, do nothing at all rather than
            // half-applying it.
            List<String> existing = AssemblyNbt.steps(workpiece);
            AssemblyRecipes.Advance current;
            try {
                current = AssemblyRecipes.advance(workpiece, existing, step);
            } catch (Throwable ignored) {
                return;
            }

            // Consume the consumable first, mirroring DepotSpell: if the write
            // below were to fail we have already given up B rather than
            // duplicating it.
            ItemStack remaining = consumable.copy();
            remaining.shrink(units);
            if (!DepotHelper.setStack(world, consumablePos, remaining)) {
                return;
            }

            if (current.finished() && current.recipe() != null) {
                finish(world, workpiecePos, workpiece, current.recipe());
            } else {
                writeStep(world, workpiecePos, workpiece, existing);
            }

            pour(world, consumablePos, workpiecePos);
        }

        /** Intermediate step: append the id and put the workpiece back. */
        private void writeStep(ServerLevel world, BlockPos pos, ItemStack workpiece, List<String> existing) {
            ItemStack updated = workpiece.copy();
            AssemblyNbt.setSteps(updated, AssemblyNbt.append(existing, step));
            DepotHelper.setStack(world, pos, updated);
        }

        /** Final step: produce the recipe's output, carrying the aether across. */
        private void finish(ServerLevel world, BlockPos pos, ItemStack workpiece, AssemblyRecipe recipe) {
            ItemStack result = recipe.assemble(
                    new AssemblyRecipe.InputHolder(workpiece, AssemblyNbt.mana(workpiece)),
                    world.registryAccess());

            long media = AssemblyNbt.mana(workpiece);
            AssemblyNbt.clear(result);
            AssemblyNbt.setMana(result, media);

            int total = result.getCount();
            int max = result.getMaxStackSize();
            if (total <= max) {
                DepotHelper.setStack(world, pos, result);
                return;
            }
            ItemStack main = result.copy();
            main.setCount(max);
            DepotHelper.setStack(world, pos, main);
            int remaining = total - max;
            Vec3 centre = Vec3.atCenterOf(pos).add(0, 0.5, 0);
            while (remaining > 0) {
                int c = Math.min(remaining, max);
                ItemStack part = result.copy();
                part.setCount(c);
                world.addFreshEntity(new ItemEntity(world, centre.x, centre.y, centre.z, part));
                remaining -= c;
            }
        }

        /**
         * The "pour" visual: a stream of particles along the arc from depot B
         * to depot A.
         *
         * <p>Deliberately <em>not</em> a real {@link ItemEntity}. The depot
         * already shrank by the exact amount, so spawning a physical item
         * alongside it would hand the player a second copy to pick up. An
         * earlier draft did exactly that; particles cannot duplicate, so
         * phase 1 gets its motion from those. A real item arc would need a
         * custom client-synced entity that is discarded on arrival — the
         * "phase 2 tween" the plan defers.
         */
        private void pour(ServerLevel world, BlockPos from, BlockPos to) {
            Vec3 start = Vec3.atCenterOf(from).add(0, 0.5, 0);
            Vec3 end = Vec3.atCenterOf(to).add(0, 0.5, 0);
            Vec3 delta = end.subtract(start);
            // Sample a few points along the arc so it reads as travel rather
            // than two unrelated puffs.
            for (int i = 1; i <= 3; i++) {
                double t = i / 4.0;
                Vec3 point = start.add(delta.multiply(t, t, t)).add(0, 0.25 * t, 0);
                world.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                        point.x, point.y, point.z, 6, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }
}

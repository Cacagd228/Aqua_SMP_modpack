package at.petrak.hexcasting.api.casting.eval.env;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.PatternShapeMatch;
import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus;
import at.petrak.hexcasting.api.casting.circles.CircleExecutionState;
import at.petrak.hexcasting.common.blocks.circles.BlockManaVessel;
import at.petrak.hexcasting.api.casting.eval.CastResult;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.MishapEnvironment;
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.casting.mishaps.MishapDisallowedSpell;
import at.petrak.hexcasting.api.mod.HexConfig;
import at.petrak.hexcasting.api.pigment.FrozenPigment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv.SENTINEL_RADIUS;

public class CircleCastEnv extends CastingEnvironment {
    protected final CircleExecutionState execState;

    public CircleCastEnv(ServerLevel world, CircleExecutionState execState) {
        super(world);
        this.execState = execState;
    }

    @Override
    public @Nullable ServerPlayer getCaster() {
        return this.execState.getCaster(this.world);
    }

    public @Nullable BlockEntityAbstractImpetus getImpetus() {
        var entity = this.world.getBlockEntity(execState.impetusPos);

        if (entity instanceof BlockEntityAbstractImpetus)
            return (BlockEntityAbstractImpetus) entity;
        return null;
    }

    public CircleExecutionState circleState() {
        return execState;
    }

    @Override
    public MishapEnvironment getMishapEnvironment() {
        return new CircleMishapEnv(this.world, this.execState);
    }

    @Override
    public void precheckAction(PatternShapeMatch match) throws Mishap {
        super.precheckAction(match);

        ResourceLocation key = actionKey(match);

        if (!HexConfig.server().isActionAllowedInCircles(key)) {
            throw new MishapDisallowedSpell("disallowed_circle");
        }
    }

    @Override
    public void postExecution(CastResult result) {
        super.postExecution(result);

        // we always want to play this sound one at a time
        var sound = result.getSound().sound();
        if (sound != null) {
            var soundPos = this.execState.currentPos;
            this.world.playSound(null, soundPos, sound, SoundSource.PLAYERS, 1f, 1f);
        }

        // TODO: this is gonna bite us in the bum someday
        // we check whether we should cut the execution in BlockSlate, but post the mishap here;
        // although everything should be pretty immutable here it's something to keep in mind
        // classic time-of-check/time-of-use
        var imp = this.getImpetus();
        if (imp != null) {
            for (var sideEffect : result.getSideEffects()) {
                if (sideEffect instanceof OperatorSideEffect.DoMishap doMishap) {
                    postCircleMishap(imp, doMishap.getMishap(), doMishap.getErrorCtx());
                } else if (sideEffect instanceof OperatorSideEffect.ConsumeMedia consume) {
                    // ConsumeMedia failure does NOT produce DoMishap by itself:
                    // the spell is silently skipped (AttemptSpell never runs) but the
                    // resolution stays EVALUATED, so BlockSlate would Continue.
                    // Turn it into a regular circle mishap (same display path,
                    // same particles, same halt as every other circle error).
                    var vessel = this.execState.currentVessel;
                    long need = consume.getAmount();
                    Mishap mishap;
                    if (vessel == null) {
                        mishap = new at.petrak.hexcasting.api.casting.mishaps.circle.MishapCircleNoVessel(
                            this.execState.currentPos);
                    } else if (vessel.getStoredMedia() < need) {
                        var fmt = new java.text.DecimalFormat("###,###.##");
                        mishap = new at.petrak.hexcasting.api.casting.mishaps.circle.MishapCircleVesselMedia(
                            fmt.format(need / at.petrak.hexcasting.api.misc.ManaHelper.MEDIA_PER_MANA),
                            fmt.format(vessel.getStoredMedia() / at.petrak.hexcasting.api.misc.ManaHelper.MEDIA_PER_MANA));
                    } else {
                        continue;
                    }
                    at.petrak.hexcasting.api.casting.math.HexPattern pattern = null;
                    if (result.getCast() instanceof at.petrak.hexcasting.api.casting.iota.PatternIota patternIota) {
                        pattern = patternIota.getPattern();
                    }
                    postCircleMishap(imp, mishap, new Mishap.Context(pattern, null));
                }
            }
        }
    }

    /**
     * Single choke point for circle errors — the same thing DoMishap does:
     * formatted message on the impetus, mishap particles, halt the execution.
     */
    private void postCircleMishap(BlockEntityAbstractImpetus imp, Mishap mishap, Mishap.Context errorCtx) {
        var msg = mishap.errorMessageWithName(this, errorCtx);
        if (msg != null) {
            imp.postMishap(msg);
        }
        var spray = mishap.particleSpray(this);
        var color = mishap.accentColor(this, errorCtx);
        spray.sprayParticles(this.world, color);
        spray.sprayParticles(this.world, new FrozenPigment(
            new ItemStack(at.petrak.hexcasting.common.lib.HexItems.DYE_PIGMENTS.get(net.minecraft.world.item.DyeColor.RED)),
            net.minecraft.Util.NIL_UUID));
        // Signal the circle to halt on mishap
        this.execState.mishapOccurred = true;
    }

    @Override
    public Vec3 mishapSprayPos() {
        return Vec3.atCenterOf(this.execState.currentPos);
    }

    @Override
    public long extractMediaEnvironment(long cost) {
        var execState = this.execState;
        var vessel = execState.currentVessel;
        if (vessel == null) {
            // No vessel = mishap (cost not paid)
            return cost;
        }

        long available = vessel.getStoredMedia();
        if (available < cost) {
            // All-or-nothing: don't drain the last drops for a spell that
            // won't fire. postExecution already flagged mishap + halt.
            return cost;
        }

        vessel.extractMedia(cost, false);
        return 0;
    }

    @Override
    public boolean isVecInRangeEnvironment(Vec3 vec) {
        var caster = this.execState.getCaster(this.world);
        if (caster != null) {
            var sentinel = HexAPI.instance().getSentinel(caster);
            if (sentinel != null
                && sentinel.extendsRange()
                && caster.level().dimension() == sentinel.dimension()
                && vec.distanceToSqr(sentinel.position()) <= SENTINEL_RADIUS * SENTINEL_RADIUS
            ) {
                return true;
            }
        }

        return this.execState.bounds.contains(vec);
    }

    @Override
    public boolean hasEditPermissionsAtEnvironment(BlockPos pos) {
        return true;
    }

    @Override
    public InteractionHand getCastingHand() {
        return InteractionHand.MAIN_HAND;
    }

    @Override
    protected List<ItemStack> getUsableStacks(StackDiscoveryMode mode) {
        return new ArrayList<>(); // TODO: Could do something like get items in inventories adjacent to the circle?
    }

    @Override
    protected List<HeldItemInfo> getPrimaryStacks() {
        return List.of(); // TODO: Adjacent inv!
    }

    @Override
    public boolean replaceItem(Predicate<ItemStack> stackOk, ItemStack replaceWith, @Nullable InteractionHand hand) {
        return false; // TODO: Adjacent inv!
    }

    @Override
    public FrozenPigment getPigment() {
        var impetus = this.getImpetus();
        if (impetus == null)
            return FrozenPigment.DEFAULT.get();
        return impetus.getPigment();
    }

    @Override
    public @Nullable FrozenPigment setPigment(@Nullable FrozenPigment pigment) {
        var impetus = this.getImpetus();
        if (impetus == null)
            return null;
        return impetus.setPigment(pigment);
    }

    @Override
    public void produceParticles(ParticleSpray particles, FrozenPigment pigment) {
        particles.sprayParticles(this.world, pigment);
    }

    @Override
    public void printMessage(Component message) {
        var impetus = getImpetus();
        if (impetus == null)
            return;
        impetus.postPrint(message);
    }
}

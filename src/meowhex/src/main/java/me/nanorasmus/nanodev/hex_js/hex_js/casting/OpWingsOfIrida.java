package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.misc.MediaConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Wings of Irida — transfers one whole stack from one Create Depot to another.
 * <p>
 * Arguments: [destination_depot_vec, source_depot_vec] (top is destination, below is source).
 * Both must be create:depot blocks. Destination depot must be empty (slot 0).
 * Cost: правка баланса — 10 маны за блок расстояния (1 мана = 1000 media).
 */
public class OpWingsOfIrida implements SpellAction {

    public static final OpWingsOfIrida INSTANCE = new OpWingsOfIrida();
    private OpWingsOfIrida() {}

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

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation cont) {
        return SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        // args[0] is the source depot A; args[1] is the destination depot B.
        BlockPos srcPos = BlockPos.containing(OperatorUtils.getVec3(args, 0, getArgc()));
        BlockPos destPos = BlockPos.containing(OperatorUtils.getVec3(args, 1, getArgc()));

        try {
            env.assertPosInRange(srcPos);
        } catch (Throwable t) { sneakyThrow(t); }
        try {
            env.assertPosInRange(destPos);
        } catch (Throwable t) { sneakyThrow(t); }

        if (!isDepotAt(env.getWorld(), srcPos)) {
            sneakyThrow(new OvidMishap("Source position is not a Create Depot: "
                + srcPos.toShortString() + " (" + blockDesc(env.getWorld(), srcPos) + ")"));
        }
        if (!isDepotAt(env.getWorld(), destPos)) {
            sneakyThrow(new OvidMishap("Destination position is not a Create Depot: "
                + destPos.toShortString() + " (" + blockDesc(env.getWorld(), destPos) + ")"));
        }

        // Use DepotHelper for reliable interaction with Create depots
        ServerLevel serverLevel = (env.getWorld() instanceof ServerLevel s) ? s : null;
        if (serverLevel == null) {
            sneakyThrow(new OvidMishap("Wings of Irida requires a server-level world"));
        }

        if (!DepotHelper.isDepot(serverLevel, srcPos) || !DepotHelper.isDepot(serverLevel, destPos)) {
            sneakyThrow(new OvidMishap("Not a valid Create Depot"));
        }

        ItemStack srcStack = DepotHelper.getStack(serverLevel, srcPos);
        ItemStack destStack = DepotHelper.getStack(serverLevel, destPos);

        if (srcStack == null || destStack == null) {
            sneakyThrow(new OvidMishap("Failed to read depot inventory"));
        }

        if (!destStack.isEmpty()) {
            sneakyThrow(new MishapDepotOccupied(destPos));
        }

        if (srcStack.isEmpty()) {
            sneakyThrow(new OvidMishap("Source depot is empty"));
        }

        Vec3 destCenter = new Vec3(destPos.getX() + 0.5, destPos.getY() + 0.5, destPos.getZ() + 0.5);
        Vec3 srcCenter = new Vec3(srcPos.getX() + 0.5, srcPos.getY() + 0.5, srcPos.getZ() + 0.5);
        List<ParticleSpray> particles = List.of(
                ParticleSpray.burst(srcCenter, 1.0, 20),
                ParticleSpray.burst(destCenter, 1.0, 20)
        );

        // Правка баланса: 10 маны за блок расстояния между депо.
        double dist = Math.sqrt(srcPos.distSqr(destPos));
        if (dist < 1.0) dist = 1.0;
        long mediaCost = (long) (dist * 10L * 1000L);
        return new Result(new Spell(srcPos, destPos), mediaCost, particles, 0);
    }

    private boolean isDepotAt(Level world, BlockPos pos) {
        var key = BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock());
        return key != null && "create".equals(key.getNamespace()) && "depot".equals(key.getPath());
    }

    private static String blockDesc(Level world, BlockPos pos) {
        if (!world.hasChunkAt(pos)) {
            return "chunk not loaded";
        }
        var key = BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock());
        return "found " + key;
    }

    public static class Spell implements RenderedSpell {
        private final BlockPos srcPos;
        private final BlockPos destPos;

        public Spell(BlockPos srcPos, BlockPos destPos) {
            this.srcPos = srcPos;
            this.destPos = destPos;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = (env.getWorld() instanceof ServerLevel s) ? s : null;
            if (world == null) return;

            ItemStack srcStack = DepotHelper.getStack(world, srcPos);
            ItemStack destStack = DepotHelper.getStack(world, destPos);
            if (srcStack == null || destStack == null) return;
            if (srcStack.isEmpty()) return;
            if (!destStack.isEmpty()) return;

            // Transfer via DepotHelper for reliability
            boolean cleared = DepotHelper.clearStack(world, srcPos);
            boolean set = DepotHelper.setStack(world, destPos, srcStack);

            if (cleared && set) {
                // Block entities already updated by DepotHelper
            }
        }
    }
}

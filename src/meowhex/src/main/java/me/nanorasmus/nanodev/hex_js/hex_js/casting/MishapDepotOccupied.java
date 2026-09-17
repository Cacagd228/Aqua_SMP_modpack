package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.pigment.FrozenPigment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MishapDepotOccupied extends Mishap {
    private final BlockPos pos;

    public MishapDepotOccupied(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public @NotNull FrozenPigment accentColor(@NotNull CastingEnvironment env, @NotNull Context context) {
        return dyeColor(DyeColor.ORANGE);
    }

    @Override
    public @NotNull Component errorMessage(@NotNull CastingEnvironment env, @NotNull Context context) {
        return Component.translatable("hexcasting.mishap.depot.occupied", pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public void execute(@NotNull CastingEnvironment env, @NotNull Context context, @NotNull List<Iota> stack) {
    }

    @Override
    public ParticleSpray particleSpray(CastingEnvironment env) {
        return ParticleSpray.burst(Vec3.atCenterOf(pos), 1.0, 20);
    }
}

package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.pigment.FrozenPigment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SilenceMishap extends Mishap {
    @Override
    public @NotNull FrozenPigment accentColor(@NotNull CastingEnvironment env, @NotNull Context context) {
        return dyeColor(DyeColor.PURPLE);
    }

    @Override
    public @NotNull Component errorMessage(@NotNull CastingEnvironment env, @NotNull Context context) {
        return Component.literal("Безмолвие").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public void execute(@NotNull CastingEnvironment env, @NotNull Context context, @NotNull List<Iota> stack) {}
}

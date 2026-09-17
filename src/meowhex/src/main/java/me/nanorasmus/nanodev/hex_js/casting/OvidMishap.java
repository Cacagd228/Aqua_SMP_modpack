package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.pigment.FrozenPigment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Mishap for Ovid's Distillation with custom message.
 */
public class OvidMishap extends Mishap {
    private final Component component;

    public OvidMishap(String message) {
        this(Component.literal(message));
    }

    public OvidMishap(Component component) {
        this.component = component;
    }

    @Override
    public @NotNull FrozenPigment accentColor(@NotNull CastingEnvironment env, @NotNull Context context) {
        return dyeColor(DyeColor.PURPLE);
    }

    @Override
    public @NotNull Component errorMessage(@NotNull CastingEnvironment env, @NotNull Context context) {
        return component;
    }

    @Override
    public void execute(@NotNull CastingEnvironment env, @NotNull Context context, @NotNull List<Iota> stack) {
    }
}

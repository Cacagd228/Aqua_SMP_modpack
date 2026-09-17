package me.nanorasmus.nanodev.hex_js.effect;

import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation;
import at.petrak.hexcasting.api.casting.mishaps.MishapImmuneEntity;
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota;
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs;
import net.minecraft.world.entity.player.Player;

public class SilenceHelper {
    // Durations in ticks (20 ticks = 1 sec)
    public static int durationFor(Mishap mishap) {
        String name = mishap.getClass().getSimpleName();
        if (mishap instanceof MishapInvalidIota || mishap instanceof MishapNotEnoughArgs) {
            return 5 * 20;
        }
        // Check for media mishap via class name contains "Media"
        if (name.toLowerCase().contains("media") || name.toLowerCase().contains("enlightenment")) {
            return 10 * 20;
        }
        if (mishap instanceof MishapImmuneEntity || mishap instanceof MishapBadLocation) {
            return 15 * 20;
        }
        // Great spell or other severe
        if (name.toLowerCase().contains("great") || name.toLowerCase().contains("enlightenment")) {
            return 20 * 20;
        }
        // Default
        return 8 * 20;
    }

    public static void applySilence(Player player, Mishap mishap) {
        if (player == null || player.level().isClientSide) return;
        if (mishap instanceof me.nanorasmus.nanodev.hex_js.casting.SilenceMishap) return;

        int dur = durationFor(mishap);
        var holder = HexEffects.SILENCE;
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(holder, dur, 0, false, true, true));
    }
}

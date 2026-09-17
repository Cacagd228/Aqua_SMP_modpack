package me.nanorasmus.nanodev.hex_js.client;

import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@OnlyIn(Dist.CLIENT)
public class SilenceOverlay {

    private static boolean wasShowing = false;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        MobEffectInstance inst = player.getEffect(HexEffects.SILENCE);
        if (inst == null) {
            // Эффект кончился — один раз гасим залипший actionbar.
            if (wasShowing) {
                wasShowing = false;
                try {
                    mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false);
                } catch (Throwable ignored) {}
            }
            return;
        }
        wasShowing = true;

        // Постоянный actionbar «Безмолвие», пока висит эффект
        // (а не только после неудачной попытки каста).
        try {
            mc.gui.setOverlayMessage(
                    net.minecraft.network.chat.Component.literal("Безмолвие")
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                    false);
        } catch (Throwable ignored) {}

        GuiGraphics gui = event.getGuiGraphics();
        int w = gui.guiWidth();
        int h = gui.guiHeight();

        // Pulse for living border — not static worldborder square
        long time = System.currentTimeMillis();
        float pulse = 0.85f + 0.15f * (float) Math.sin(time / 350.0);
        // Fade out in last 40 ticks
        float remaining = inst.getDuration() / 20f;
        float fade = remaining < 2f ? Math.max(0, remaining / 2f) : 1f;
        int baseAlpha = (int) (170 * pulse * fade);

        int outer = (baseAlpha << 24) | 0x9A5CC8; // amethyst outer
        int inner = (0 << 24) | 0x9A5CC8; // transparent inward

        int thickness = 14;
        int feather = 10; // gradient feather inward
        int depth = thickness + feather;

        // Top / bottom: one smooth vertical fade outer -> transparent.
        // (Раньше полоса гасла в 0, а растушёвка начиналась с 0.45 — ступенька.)
        gui.fillGradient(0, 0, w, depth, outer, inner);
        gui.fillGradient(0, h - depth, w, h, inner, outer);
        // Left / right: per-column horizontal fade, same curve as top/bottom.
        // (Раньше бока были плоскими плашками на полную альфу — резкие прямоугольники.)
        int sideTop = depth;
        int sideBottom = h - depth;
        for (int x = 0; x < depth; x++) {
            float t = 1f - (float) x / depth;
            int col = (((int) (baseAlpha * t)) << 24) | 0x9A5CC8;
            gui.fill(x, sideTop, x + 1, sideBottom, col);
            gui.fill(w - 1 - x, sideTop, w - x, sideBottom, col);
        }

        // Soft vignette inside — not a solid square overlay, barely visible purple haze
        int vignetteAlpha = (int) (18 * fade);
        int vignette = (vignetteAlpha << 24) | 0x6A3FA0;
        gui.fill(0, 0, w, h, vignette);
    }
}

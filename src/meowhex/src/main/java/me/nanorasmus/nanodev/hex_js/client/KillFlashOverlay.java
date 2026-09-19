package me.nanorasmus.nanodev.hex_js.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Красная вспышка после PvP-фрага: выглядит как виньетка worldborder
 * (красные края + лёгкая красная дымка), но рисуется своим оверлеем,
 * поэтому никаких стен рамки не видно. Огибающая: быстрое появление,
 * короткое удержание, плавное исчезновение (~0.9 c суммарно).
 */
@OnlyIn(Dist.CLIENT)
public class KillFlashOverlay {
    private static final long ATTACK_MS = 150L;
    private static final long HOLD_MS = 300L;
    private static final long RELEASE_MS = 450L;

    private static volatile long triggerMs = -1L;

    public static void trigger() {
        triggerMs = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        long start = triggerMs;
        if (start < 0) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        long dt = System.currentTimeMillis() - start;
        long total = ATTACK_MS + HOLD_MS + RELEASE_MS;
        if (dt >= total) {
            triggerMs = -1L;
            return;
        }

        float a;
        if (dt < ATTACK_MS) {
            a = dt / (float) ATTACK_MS;
        } else if (dt < ATTACK_MS + HOLD_MS) {
            a = 1f;
        } else {
            a = 1f - (dt - ATTACK_MS - HOLD_MS) / (float) RELEASE_MS;
        }

        GuiGraphics gui = event.getGuiGraphics();
        int w = gui.guiWidth();
        int h = gui.guiHeight();

        int baseAlpha = (int) (150 * a);
        int outer = (baseAlpha << 24) | 0xFF2222;
        int inner = 0x00FF2222;

        int thickness = 18;
        int feather = 26;
        int depth = thickness + feather;

        gui.fillGradient(0, 0, w, depth, outer, inner);
        gui.fillGradient(0, h - depth, w, h, inner, outer);
        int sideTop = depth;
        int sideBottom = h - depth;
        for (int x = 0; x < depth; x++) {
            float t = 1f - (float) x / depth;
            int col = (((int) (baseAlpha * t)) << 24) | 0xFF2222;
            gui.fill(x, sideTop, x + 1, sideBottom, col);
            gui.fill(w - 1 - x, sideTop, w - x, sideBottom, col);
        }

        // Лёгкая красная дымка по всему экрану.
        int haze = (((int) (16 * a)) << 24) | 0xFF2222;
        gui.fill(0, 0, w, h, haze);
    }
}

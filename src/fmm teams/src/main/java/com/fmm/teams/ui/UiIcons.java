// Vendored icon helper pattern from Panoptic (LGPL-3.0-only, Mokich).
// TODO: add assets/fmm_teams/textures/gui/icons.png (128x64, 16px cells) —
// currently falls back to pixel icons so the mod builds/runs without the texture.
package com.fmm.teams.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class UiIcons {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("fmm_teams", "textures/gui/icons.png");

    private UiIcons() {}

    public static void check(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y + 3, x + 7, y + 4, c);
        g.fill(x + 2, y + 4, x + 3, y + 6, c);
    }

    public static void cross(GuiGraphics g, int x, int y, int c) {
        for (int i = 0; i < 7; i++) {
            g.fill(x + i, y + i, x + i + 1, y + i + 1, c);
            g.fill(x + 6 - i, y + i, x + 7 - i, y + i + 1, c);
        }
    }

    public static void blitCell(GuiGraphics g, int x, int y, int idx, int c) {
        int a = c >>> 24;
        if (a == 0) {
            a = 255;
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F,
                (c & 0xFF) / 255.0F, a / 255.0F);
        g.blit(RenderType::guiTextured, TEX, x - 2, y - 2, idx % 8 * 16, idx / 8 * 16, 16, 16, 128, 64);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}

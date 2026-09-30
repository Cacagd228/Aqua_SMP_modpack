// Vendored icon helper pattern from Panoptic (LGPL-3.0-only, Mokich).
// Icons are drawn as pixels; a spritesheet would need assets/fmm_teams/textures/gui/icons.png
// (128x64, 16px cells) plus a blit call, which is not wired up yet.
package com.fmm.teams.ui;

import net.minecraft.client.gui.GuiGraphics;

public final class UiIcons {
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
}

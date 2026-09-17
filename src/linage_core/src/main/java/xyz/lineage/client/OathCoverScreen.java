package xyz.lineage.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import xyz.lineage.vendored.panoptic.api.ui.GuiStyle;
import xyz.lineage.LineageCore;

/** A plain-spoken covenant, set in riveted Panoptic plate. */
@OnlyIn(Dist.CLIENT)
public class OathCoverScreen extends Screen {
    private final boolean ascend;
    private int[] takeBox = new int[4];

    public OathCoverScreen(boolean ascend) {
        super(Component.translatable("gui." + LineageCore.MOD_ID + ".covenant"));
        this.ascend = ascend;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        renderBackground(gfx, mouseX, mouseY, delta);
        int cx = width / 2;
        int cy = height / 2;
        int x1 = cx - 190;
        int y1 = cy - 85;
        int x2 = cx + 190;
        int y2 = cy + 85;
        GuiStyle.panel(gfx, x1, y1, x2, y2);
        GuiStyle.panelHeader(gfx, font, x1, y1, x2, title.getString().toUpperCase(), null);
        gfx.drawCenteredString(font, Component.translatable("gui." + LineageCore.MOD_ID + ".covenant_lore1"), cx, y1 + 52, GuiStyle.TEXT);
        gfx.drawCenteredString(font, Component.translatable("gui." + LineageCore.MOD_ID + ".covenant_lore2"), cx, y1 + 70, GuiStyle.MUTED);
        GuiStyle.divider(gfx, x1 + 12, x2 - 12, y1 + 88);
        takeBox = new int[]{cx - 140, y2 - 44, cx + 140, y2 - 20};
        boolean hov = mouseX >= takeBox[0] && mouseX < takeBox[2] && mouseY >= takeBox[1] && mouseY < takeBox[3];
        GuiStyle.button(gfx, font, takeBox[0], takeBox[1], takeBox[2], takeBox[3],
            Component.translatable("gui." + LineageCore.MOD_ID + ".covenant_take").getString(), hov, true);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && x >= takeBox[0] && x < takeBox[2] && y >= takeBox[1] && y < takeBox[3]) {
            accept();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean keyPressed(int code, int scan, int mods) {
        if (code == 257 || code == 32) {
            accept();
            return true;
        }
        return super.keyPressed(code, scan, mods);
    }

    private void accept() {
        var game = net.minecraft.client.Minecraft.getInstance();
        if (game.player != null) {
            game.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
        }
        game.setScreen(new ChronicleScreen(false, ascend));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}

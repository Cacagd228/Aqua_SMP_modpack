package com.colonizer.colonycard.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Кнопка в стиле самого паспорта: пергаментная плашка, чернильная рамка,
 * сургучная точка-индикатор активного состояния.
 *
 * <p>Собрана на {@link CardStyle}, поэтому рядом с разворотом не выбивается из
 * документа, в отличие от ванильной {@code Button}.
 */
public class DocButton extends AbstractWidget {

    private static final int BG_HOVER = 0xFFF6E9CB;
    private static final int BG_IDLE = 0xFFEBDCBB;
    private static final int BG_ACTIVE = 0xFFF0D9AE;

    private final Runnable onPress;
    private Component label;
    private boolean active;

    public DocButton(int x, int y, int w, int h, Component label, boolean active, Runnable onPress) {
        super(x, y, w, h, label);
        this.label = label;
        this.active = active;
        this.onPress = onPress;
    }

    public void setLabel(Component label) {
        this.label = label;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress();
    }

    public void onPress() {
        this.onPress.run();
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hover = isHovered();
        int bg = active ? (hover ? BG_ACTIVE : BG_IDLE) : (hover ? BG_HOVER : BG_IDLE);
        int x1 = getX();
        int y1 = getY();
        int x2 = x1 + width;
        int y2 = y1 + height;

        g.fill(x1 + 2, y1 + 3, x2 + 2, y2 + 3, 0x40000000);
        CardStyle.plate(g, x1, y1, x2, y2, bg, CardStyle.PARCHMENT_DEEP, CardStyle.INK_LINE);
        CardStyle.rect(g, x1 + 2, y1 + 2, x2 - 2, y2 - 2, hover ? CardStyle.INK_FAINT : CardStyle.PARCHMENT_EDGE);

        // Сургучная точка слева: активный режим помечен печатью.
        if (active) {
            CardStyle.disc(g, x1 + 9, y2 / 2, 3, CardStyle.SEAL_RED_DARK);
            CardStyle.disc(g, x1 + 9, y2 / 2, 2, CardStyle.SEAL_RED);
        } else {
            CardStyle.disc(g, x1 + 9, y2 / 2, 2, CardStyle.INK_FAINT);
        }

        var font = Minecraft.getInstance().font;
        g.drawString(font, label, x1 + 18, y1 + (height - 8) / 2, CardStyle.INK, false);

        if (hover) {
            g.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, CardStyle.INK_FAINT);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, label);
    }
}

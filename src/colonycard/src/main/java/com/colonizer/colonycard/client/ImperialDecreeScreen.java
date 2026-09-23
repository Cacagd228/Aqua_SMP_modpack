package com.colonizer.colonycard.client;

import com.colonizer.colonycard.client.ui.CardStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/**
 * Бумажная грамота — имперский декрет о награждении.
 *
 * <p>Тот же бумажный стиль, что у карты колониста ({@link CardStyle#page}),
 * но одиночный лист с соотношением сторон А4 (210×297).
 */
public class ImperialDecreeScreen extends Screen {

    /** A4: 210×297. */
    private static final int PAGE_W = 210;
    private static final int PAGE_H = 297;
    private static final int PAD = 12;

    private final String recipient;
    private long appearStart;

    public ImperialDecreeScreen(String recipient) {
        super(Component.translatable("colonycard.decree.title"));
        this.recipient = recipient != null ? recipient : "";
    }

    @Override
    protected void init() {
        super.init();
        if (appearStart == 0) {
            appearStart = System.nanoTime();
        }
    }

    private float appear() {
        if (appearStart == 0) {
            return 1.0F;
        }
        float t = Mth.clamp((System.nanoTime() - appearStart) / 1.6E8F, 0.0F, 1.0F);
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv;
    }

    private String recipientName() {
        if (!recipient.isEmpty()) {
            return recipient;
        }
        if (this.minecraft != null && this.minecraft.player != null) {
            return this.minecraft.player.getGameProfile().getName();
        }
        return "";
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(g);

        float fit = Math.min((this.width - 4.0F) / PAGE_W, (this.height - 4.0F) / PAGE_H);
        fit = Mth.clamp(fit, 0.4F, 2.2F);
        float sc = fit * (0.96F + 0.04F * appear());
        float cx = this.width / 2.0F;
        float cy = this.height / 2.0F;

        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(sc, sc, 1.0F);
        g.pose().translate(-PAGE_W / 2.0F, -PAGE_H / 2.0F, 0);

        drawPage(g);

        g.pose().popPose();
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawPage(GuiGraphics g) {
        CardStyle.page(g, 0, 0, PAGE_W, PAGE_H);

        int ix = PAD;
        int iw = PAGE_W - PAD * 2;
        int cx = PAGE_W / 2;

        // Герб / имперская печать — заглушка: сургучная печать без надписей.
        int sealY = 36;
        CardStyle.waxSeal(g, cx, sealY, 14);
        g.drawCenteredString(this.font, "✦", cx, sealY - 3, CardStyle.SEAL_CREAM);
        int y = sealY + 20;
        CardStyle.docRule(g, ix + 30, ix + iw - 30, y);
        y += 8;

        // Заголовок в стиле карты колониста.
        CardStyle.pageTitle(g, this.font, cx, y, iw / 2, I18n.get("colonycard.decree.title"));
        y += 20;

        // Основной текст.
        y += drawCenteredWrapped(g, I18n.get("colonycard.decree.body1"), cx, y, iw, CardStyle.INK);
        y += 8;

        // Имя колониста — с линейками сверху/снизу как торжественная строка.
        CardStyle.docRule(g, ix + 20, ix + iw - 20, y);
        y += 7;
        String name = recipientName();
        if (name.isEmpty()) {
            name = "—";
        }
        for (FormattedCharSequence line : this.font.split(Component.literal(name), iw - 20)) {
            int lw = this.font.width(line);
            // Имя чуть выделяем: рисуем дважды со сдвигом на пиксель для полужирности.
            g.drawString(this.font, line, cx - lw / 2, y, CardStyle.INK, false);
            g.drawString(this.font, line, cx - lw / 2 + 1, y, CardStyle.INK, false);
            y += 11;
        }
        y += 3;
        CardStyle.docRule(g, ix + 20, ix + iw - 20, y);
        y += 9;

        y += drawCenteredWrapped(g, I18n.get("colonycard.decree.body2"), cx, y, iw, CardStyle.INK);
        y += 6;

        // Подвал — обычный текст, прижат к низу листа.
        int footer2 = PAGE_H - PAD - 8;
        int footer1 = footer2 - 10;
        drawCenteredLine(g, I18n.get("colonycard.decree.footer1"), cx, footer1, iw, CardStyle.INK);
        drawCenteredLine(g, I18n.get("colonycard.decree.footer2"), cx, footer2, iw, CardStyle.INK);
    }

    /** Одна центрированная строка обычным начертанием. */
    private void drawCenteredLine(GuiGraphics g, String text, int cx, int y, int w, int color) {
        for (FormattedCharSequence line : this.font.split(Component.literal(text), w)) {
            g.drawString(this.font, line, cx - this.font.width(line) / 2, y, color, false);
            y += 10;
        }
    }

    /** Центрированный перенос по ширине. Возвращает занятую высоту. */
    private int drawCenteredWrapped(GuiGraphics g, String text, int cx, int y, int w, int color) {
        int h = 0;
        for (FormattedCharSequence line : this.font.split(Component.literal(text), w)) {
            g.drawString(this.font, line, cx - this.font.width(line) / 2, y + h, color, false);
            h += 10;
        }
        return h;
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    protected void renderMenuBackground(GuiGraphics g) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package com.colonizer.colonycard.client;

import com.colonizer.colonycard.client.ui.CardStyle;
import com.colonizer.colonycard.data.ColonistData;
import com.colonizer.colonycard.data.ColonistPools;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Имперский документ колониста — только сам разворот, без обложки и вкладок.
 *
 * <p>Две пергаментные страницы рисуются в локальных координатах разворота
 * и масштабируются ×1.2 через pose ({@link #DOC_SCALE}), поэтому ховер-проверки
 * идут по обратно-преобразованным координатам мыши.
 *
 * <ul>
 *   <li>левая страница «Личное дело»: квадратное фото (голова игрока), справа ровно 3 поля
 *       (Имя / Дата прибытия / Цель прибытия), ниже ровно 4 поля 2×2
 *       (Особые приметы / Статус / Место выдачи / Выдал);</li>
 *   <li>правая страница: 6 наград в виде почтовых марок 2×3 на всю страницу.</li>
 * </ul>
 */
public class ColonistCardScreen extends Screen {

    /** Масштаб интерфейса документа. */
    private static final float DOC_SCALE = 1.2F;

    private static final int PAGE_W = 213;
    private static final int PAGE_H = 251;
    private static final int PAGE_GAP = 6;
    private static final int PAGE_PAD = 8;

    private static final int SPREAD_W = PAGE_W * 2 + PAGE_GAP;
    private static final int SPREAD_H = PAGE_H;

    private static final int PHOTO = 64;

    private long appearStart;
    private String hoverTip;

    private ColonistData data = ClientColonistCache.get();

    public ColonistCardScreen() {
        super(Component.translatable("colonycard.screen.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.data = ClientColonistCache.get();
        if (appearStart == 0) {
            appearStart = System.nanoTime();
        }
    }

    public void onDataUpdated(ColonistData newData) {
        this.data = newData;
    }

    private float appear() {
        if (appearStart == 0) {
            return 1.0F;
        }
        float t = Mth.clamp((System.nanoTime() - appearStart) / 1.6E8F, 0.0F, 1.0F);
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(g);
        hoverTip = null;

        float sc = (0.96F + 0.04F * appear()) * DOC_SCALE;
        float cx = this.width / 2.0F;
        float cy = this.height / 2.0F;
        // Мышь в локальных координатах разворота для ховер-проверок.
        float hx = (mouseX - cx) / sc + SPREAD_W / 2.0F;
        float hy = (mouseY - cy) / sc + SPREAD_H / 2.0F;

        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(sc, sc, 1.0F);
        g.pose().translate(-SPREAD_W / 2.0F, -SPREAD_H / 2.0F, 0);

        if (data.initialized()) {
            drawSpread(g, hx, hy);
        } else {
            g.drawCenteredString(this.font, "...", SPREAD_W / 2, SPREAD_H / 2, CardStyle.INK_FAINT);
        }

        g.pose().popPose();
        super.render(g, mouseX, mouseY, partialTick);
        if (hoverTip != null) {
            g.renderTooltip(this.font, Component.literal(hoverTip), mouseX, mouseY);
        }
    }

    // ---- разворот (локальные координаты, origin слева сверху) ----

    private void drawSpread(GuiGraphics g, float mouseX, float mouseY) {
        int lx = 0;
        int rx = PAGE_W + PAGE_GAP;

        // Красная обложка — небольшой задник за страницами.
        CardStyle.cover(g, -10, -10, SPREAD_W + 10, SPREAD_H + 10);

        CardStyle.page(g, lx, 0, lx + PAGE_W, PAGE_H);
        CardStyle.page(g, rx, 0, rx + PAGE_W, PAGE_H);
        CardStyle.spine(g, lx + PAGE_W + PAGE_GAP / 2, 0, PAGE_H);

        String fileNo = fileNo();
        drawLeftPage(g, mouseX, mouseY, lx, PAGE_W, PAGE_H, fileNo);
        drawRightPage(g, mouseX, mouseY, rx, PAGE_W, PAGE_H, fileNo);
    }

    private String fileNo() {
        long n = 1000 + Math.abs((data.arrivalDate() / 1000) % 9000);
        return I18n.get("colonycard.page.file_no") + " " + n;
    }

    // ---- левая страница: «Личное дело» ----

    private void drawLeftPage(GuiGraphics g, float mouseX, float mouseY,
                              int lx, int pw, int ph, String fileNo) {
        int py = 0;
        int ix = lx + PAGE_PAD;
        int iw = pw - PAGE_PAD * 2;
        int cx = lx + pw / 2;

        g.drawString(this.font, fileNo, ix, py + 6, CardStyle.INK_FAINT, false);
        String folio = "1";
        g.drawString(this.font, folio, lx + pw - PAGE_PAD - font.width(folio), py + 6, CardStyle.INK_FAINT, false);
        CardStyle.pageTitle(g, this.font, cx, py + 14, 92, I18n.get("colonycard.page.personal"));

        // Верхний блок: квадратное фото + ровно 3 поля справа.
        int y0 = py + 38;
        g.fill(ix - 2, y0 - 2, ix + PHOTO + 2, y0 + PHOTO + 2, CardStyle.INK_LINE);
        g.fill(ix, y0, ix + PHOTO, y0 + PHOTO, CardStyle.PARCHMENT_DEEP);
        if (this.minecraft != null && this.minecraft.player != null) {
            PlayerFaceRenderer.draw(g, this.minecraft.player.getSkin().texture(), ix, y0, PHOTO);
        }
        String photoCap = I18n.get("colonycard.label.portrait");
        g.drawCenteredString(this.font, photoCap, ix + PHOTO / 2, y0 + PHOTO + 4, CardStyle.INK_FAINT);

        int fx = ix + PHOTO + 8;
        int fw = iw - PHOTO - 8;
        String name = this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getGameProfile().getName() : data.colonizerName();
        String date = data.arrivalDate() > 0
                ? new SimpleDateFormat("dd.MM.yyyy").format(new Date(data.arrivalDate()))
                : "-";
        String goal = data.arrivalGoal().isEmpty() ? "-" : resolve(data.arrivalGoal());

        int fy = y0;
        fy += CardStyle.docField(g, this.font, fx, fy, fw,
                I18n.get("colonycard.label.colonizer_name"), name, CardStyle.INK, 1) + 3;
        fy += CardStyle.docField(g, this.font, fx, fy, fw,
                I18n.get("colonycard.label.arrival_date"), date, CardStyle.INK, 1) + 3;
        fy += CardStyle.docField(g, this.font, fx, fy, fw,
                I18n.get("colonycard.label.arrival_goal"), goal, CardStyle.INK, 3);

        int topBlockBottom = Math.max(y0 + PHOTO + 12, fy + 2);
        CardStyle.docRule(g, ix, ix + iw, topBlockBottom);
        int y2 = topBlockBottom + 8;

        // Нижний блок: ровно 4 поля сеткой 2×2.
        int gap = 8;
        int cellW = (iw - gap) / 2;
        Status status = status();
        CardStyle.docField(g, this.font, ix, y2, cellW,
                I18n.get("colonycard.label.traits"), traitsSummary(), CardStyle.INK, 2);
        CardStyle.docField(g, this.font, ix + cellW + gap, y2, cellW,
                I18n.get("colonycard.label.status"), I18n.get(status.key()), status.color(), 2);
        int y3 = y2 + 40;
        CardStyle.docField(g, this.font, ix, y3, cellW,
                I18n.get("colonycard.label.issue_place"), I18n.get("colonycard.issue.place"), CardStyle.INK, 2);
        CardStyle.docField(g, this.font, ix + cellW + gap, y3, cellW,
                I18n.get("colonycard.label.issued_by"), I18n.get("colonycard.issued.by"), CardStyle.INK, 2);

        // Печать канцелярии внизу страницы.
        int sealY = y3 + 52;
        CardStyle.waxSeal(g, cx, sealY, 10);
        g.drawCenteredString(this.font, "K", cx, sealY - 3, CardStyle.SEAL_CREAM);
    }

    // ---- правая страница: «Официальные отметки и награды» ----

    private void drawRightPage(GuiGraphics g, float mouseX, float mouseY,
                               int rx, int pw, int ph, String fileNo) {
        int py = 0;
        int ix = rx + PAGE_PAD;
        int iw = pw - PAGE_PAD * 2;
        int cx = rx + pw / 2;

        String folio = "2";
        g.drawString(this.font, folio, rx + pw - PAGE_PAD - font.width(folio), py + 6, CardStyle.INK_FAINT, false);
        CardStyle.pageTitle(g, this.font, cx, py + 14, 92, I18n.get("colonycard.page.official"));

        // Награды в виде марок 2×3 — на всю страницу.
        int gy = py + 28;
        long unlocked = 0;
        for (int i = 0; i < ColonistData.REWARD_COUNT; i++) {
            if (data.hasReward(i)) {
                unlocked++;
            }
        }
        g.drawString(this.font, I18n.get("colonycard.label.stamps"), ix, gy, CardStyle.INK, false);
        String count = unlocked + "/" + ColonistData.REWARD_COUNT;
        g.drawString(this.font, count, ix + iw - font.width(count), gy, CardStyle.INK_FAINT, false);
        gy += 12;

        int gap = 6;
        int sw = (iw - gap) / 2;
        int sh = 60;
        for (int i = 0; i < ColonistData.REWARD_COUNT; i++) {
            int col = i % 2;
            int row = i / 3;
            int sx = ix + col * (sw + gap);
            int sy = gy + row * (sh + gap);
            drawStamp(g, mouseX, mouseY, sx, sy, sw, sh, i);
        }
    }

    private void drawStamp(GuiGraphics g, float mouseX, float mouseY, int sx, int sy, int sw, int sh, int index) {
        var def = ColonistPools.REWARDS[index];
        boolean unlocked = data.hasReward(index);
        boolean hover = hoverRect(mouseX, mouseY, sx, sy, sx + sw, sy + sh);
        CardStyle.stamp(g, sx, sy, sw, sh, unlocked, hover);

        ItemStack stack = new ItemStack(def.icon());
        int iconX = sx + sw / 2 - 8;
        int iconY = sy + 4;
        g.renderItem(stack, iconX, iconY);
        if (!unlocked) {
            g.fill(iconX, iconY, iconX + 16, iconY + 16, 0x6B5A4A3A);
        } else {
            // штемпель гашения — красный ромб в углу марки
            CardStyle.diamond(g, sx + sw - 8, sy + 4, CardStyle.SEAL_RED);
        }

        String name = I18n.get(def.nameKey());
        var lines = this.font.split(Component.literal(name), sw - 8);
        int n = Math.min(2, Math.max(1, lines.size()));
        int color = unlocked ? CardStyle.INK : CardStyle.INK_FAINT;
        for (int i = 0; i < n; i++) {
            int lw = font.width(lines.get(i));
            g.drawString(this.font, lines.get(i), sx + sw / 2 - lw / 2, sy + 23 + i * 9, color, false);
        }
        if (!unlocked) {
            String lock = I18n.get("colonycard.reward.locked");
            int lw = font.width(lock);
            if (lw <= sw - 8) {
                g.drawString(this.font, lock, sx + sw / 2 - lw / 2, sy + sh - 11, CardStyle.INK_FAINT, false);
            }
        }
        if (hover) {
            hoverTip = unlocked ? name : name + " (" + I18n.get("colonycard.reward.locked") + ")";
        }
    }

    // ---- helpers ----

    private record Status(String key, int color) {}

    private Status status() {
        int v = Mth.clamp(data.loyalty(), -100, 100);
        if (v >= 60) {
            return new Status("colonycard.status.devoted", 0xFF2F6B33);
        }
        if (v >= 20) {
            return new Status("colonycard.status.loyal", 0xFF2F6B33);
        }
        if (v > -20) {
            return new Status("colonycard.status.neutral", CardStyle.INK);
        }
        if (v > -60) {
            return new Status("colonycard.status.watched", CardStyle.SEAL_RED);
        }
        return new Status("colonycard.status.unreliable", CardStyle.SEAL_RED);
    }

    private static String resolve(String key) {
        return I18n.exists(key) ? I18n.get(key) : key;
    }

    /** Приметы одной строкой через запятую — для компактной ячейки левого разворота. */
    private String traitsSummary() {
        List<String> traits = data.traits();
        if (traits.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < traits.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(resolve(traits.get(i)));
        }
        return sb.toString();
    }

    private static boolean hoverRect(float mx, float my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
    }

    private String trim(String s, int w) {
        if (w <= 6) {
            return "";
        }
        if (this.font.width(s) <= w) {
            return s;
        }
        return this.font.plainSubstrByWidth(s, w - 6) + "...";
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

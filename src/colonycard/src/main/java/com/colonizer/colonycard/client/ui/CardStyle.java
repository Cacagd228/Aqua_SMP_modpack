package com.colonizer.colonycard.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Panoptic-style UI kit, vendored into ColonyCard.
 *
 * <p>API-совместим с {@code net.mokich.panoptic.api.ui.GuiStyle}:
 * та же палитра (тёмное дерево + латунь + пергамент), те же примитивы
 * {@code panel / panelHeader / plate / button / slot / divider / row / rect},
 * тот же паттерн {@code T(color)} для будущего темирования.
 *
 * <p>Почему вендор, а не зависимость: Panoptic распространяется только как
 * мод (jar без maven-артефакта), поэтому жёсткая зависимость
 * {@code net.mokich.panoptic:api} сломала бы сборку пак-сборки.
 * Если Panoptic лежит рядом в дев-окружении — замени импорты
 * {@code CardStyle -> GuiStyle} 1:1, сигнатуры совпадают.
 */
public final class CardStyle {
    private CardStyle() {}

    // ---- palette: 1:1 Panoptic (тёплое тёмное дерево + латунь) ----
    public static int BG = 0xF2241E15;
    public static int BG2 = 0xF21A150D;
    public static int PANEL = 0xFF221C13;
    public static int PANEL2 = 0xFF17130C;
    public static int BORDER = 0xFF63532F;
    public static int BORDER_T = 0xFFDCBC78;
    public static int BORDER_B = 0xFF8C6C33;
    public static int TITLE = 0xFF2E2718;
    public static int HEADER = 0xFF282115;
    public static int TEXT = 0xFFF2EDE1;
    public static int MUTED = 0xFFACA188;
    public static int DIM = 0xFF766C52;
    public static int ACCENT = 0xFFE8C06C;
    public static int ROWHOVER = 0x1AE8C06C;
    public static int SEARCH_BG = 0xFF17130B;

    public static int PLATE_HI = 0xFF4A3A1D;
    public static int PLATE_LO = 0xFF231C11;
    public static int RIVET = 0xFF8C6C33;

    // Акценты под смысл (лояльность / награды) — в гамме Panoptic.
    public static final int POSITIVE = 0xFF8FB58A; // приглушённый зелёный
    public static final int NEGATIVE = 0xFFC96A5C; // приглушённый ржаво-красный
    public static final int NEUTRAL = 0xFFD8C79A;  // пергаментно-жёлтый
    public static final int REWARD_ON = 0xFFD98E5A; // терракота/медь
    public static final int REWARD_OFF = 0xFF6B6152; // пепел

    // ---- imperial document palette: светлый пергамент + чернила + сургуч ----
    public static final int PARCHMENT = 0xFFF2E2BE;
    public static final int PARCHMENT_DEEP = 0xFFE4C896;
    public static final int PARCHMENT_EDGE = 0xFFB08D57;
    public static final int INK = 0xFF3A2A18;
    public static final int INK_FAINT = 0xFF8A7355;
    public static final int INK_LINE = 0xFF6B5233;
    public static final int SEAL_RED = 0xFFB03A2E;
    public static final int SEAL_RED_DARK = 0xFF7E241C;
    public static final int SEAL_CREAM = 0xFFF6E7C8;
    public static final int STAMP_BG = 0xFFF8EDD2;

    // ---- обложка паспорта: красный коленкор + золотое тиснение ----
    public static final int COVER_RED = 0xFF7E2121;
    public static final int COVER_DEEP = 0xFF571313;
    public static final int COVER_GOLD = 0xFFD9B45C;

    /** Тема-хook как в Panoptic: сейчас identity, позже сюда ложится тинт. */
    public static int T(int argb) {
        return argb;
    }

    public static void niceBox(GuiGraphics g, int x1, int y1, int x2, int y2, int bg, int bgB, int bTop, int bBot) {
        g.fillGradient(x1 + 1, y1 + 1, x2 - 1, y2 - 1, bg, bgB);
        g.fillGradient(x1, y1 + 1, x1 + 1, y2 - 1, bTop, bBot);
        g.fillGradient(x2 - 1, y1 + 1, x2, y2 - 1, bTop, bBot);
        g.fill(x1 + 1, y1, x2 - 1, y1 + 1, bTop);
        g.fill(x1 + 1, y2 - 1, x2 - 1, y2, bBot);
        g.fill(x1, y1, x1 + 1, y1 + 1, bTop);
        g.fill(x2 - 1, y1, x2, y1 + 1, bTop);
        g.fill(x1, y2 - 1, x1 + 1, y2, bBot);
        g.fill(x2 - 1, y2 - 1, x2, y2, bBot);
    }

    public static void box(GuiGraphics g, int x1, int y1, int x2, int y2) {
        niceBox(g, x1, y1, x2, y2, BG, BG2, BORDER_T, BORDER_B);
    }

    public static void rect(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }

    public static void plate(GuiGraphics g, int x1, int y1, int x2, int y2, int top, int bot, int border) {
        rect(g, x1, y1, x2, y2, border);
        g.fillGradient(x1 + 1, y1 + 1, x2 - 1, y2 - 1, top, bot);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, mix(top, T(0xFFFFE7B0), 0.26F));
        g.fill(x1 + 1, y1 + 1, x1 + 2, y2 - 1, mix(top, T(0xFFFFE7B0), 0.10F));
        g.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, mix(bot, T(0xFF000000), 0.14F));
    }

    /** Панель Panoptic: тень + латунная пластина + 4 заклёпки. */
    public static void panel(GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, T(0x90000000));
        plate(g, x1, y1, x2, y2, T(0xF22B2418), T(0xF21D1810), BORDER_B);
        rivet(g, x1 + 3, y1 + 3);
        rivet(g, x2 - 5, y1 + 3);
        rivet(g, x1 + 3, y2 - 5);
        rivet(g, x2 - 5, y2 - 5);
    }

    /** Заголовок панели: латунная плашка + акцент-полоса + правый dim-текст. */
    public static void panelHeader(GuiGraphics g, Font font, int x1, int y1, int x2, String title, String right) {
        g.fillGradient(x1 + 1, y1 + 1, x2 - 1, y1 + 14, T(0xFF3A2F1B), T(0xFF241D11));
        g.fill(x1 + 1, y1 + 14, x2 - 1, y1 + 15, BORDER_B);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, T(0x30FFE7B0));
        g.fill(x1 + 4, y1 + 4, x1 + 6, y1 + 12, ACCENT);
        g.drawString(font, title, x1 + 10, y1 + 4, ACCENT, false);
        if (right != null) {
            g.drawString(font, right, x2 - 6 - font.width(right), y1 + 4, DIM, false);
        }
    }

    public static void rivet(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 2, y + 2, RIVET);
        g.fill(x, y, x + 1, y + 1, T(0xFFD8B87A));
    }

    public static void button(GuiGraphics g, Font font, int x1, int y1, int x2, int y2,
                              String label, boolean hovered, boolean enabled) {
        button(g, font, x1, y1, x2, y2, label, hovered, enabled, false);
    }

    public static void button(GuiGraphics g, Font font, int x1, int y1, int x2, int y2,
                              String label, boolean hovered, boolean enabled, boolean active) {
        int top;
        int bot;
        int border;
        if (!enabled) {
            top = T(0xFF2A2418);
            bot = T(0xFF201A11);
            border = T(0xFF463B26);
        } else if (active) {
            top = T(0xFF634E27);
            bot = T(0xFF44351B);
            border = ACCENT;
        } else if (hovered) {
            top = T(0xFF554327);
            bot = T(0xFF382C18);
            border = ACCENT;
        } else {
            top = T(0xFF453824);
            bot = T(0xFF2C2416);
            border = T(0xFF7A6438);
        }
        plate(g, x1, y1, x2, y2, top, bot, border);
        int color = !enabled ? DIM : hovered || active ? ACCENT : TEXT;
        g.drawCenteredString(font, label, (x1 + x2) / 2, y1 + (y2 - y1 - 8) / 2 + 1, color);
    }

    public static void slot(GuiGraphics g, int x, int y, int size) {
        g.fill(x, y, x + size, y + size, T(0xFF0F0C07));
        g.fill(x, y, x + size, y + 1, T(0xFF120E08));
        g.fill(x, y, x + 1, y + size, T(0xFF120E08));
        g.fill(x + size - 1, y + 1, x + size, y + size, T(0xFF4A3A1D));
        g.fill(x + 1, y + size - 1, x + size, y + size, T(0xFF4A3A1D));
        g.fill(x + 1, y + 1, x + size - 1, y + 2, T(0x22000000));
    }

    public static void slot(GuiGraphics g, int x, int y) {
        slot(g, x, y, 18);
    }

    public static void divider(GuiGraphics g, int x1, int x2, int y) {
        g.fill(x1, y, x2, y + 1, T(0xFF120E08));
        g.fill(x1, y + 1, x2, y + 2, T(0x1AFFE7B0));
    }

    public static void row(GuiGraphics g, int x1, int y1, int x2, int y2, boolean hovered, boolean selected) {
        if (selected) {
            g.fillGradient(x1, y1, x2, y2, T(0x44E8C06C), T(0x22E8C06C));
            g.fill(x1, y1, x1 + 2, y2, ACCENT);
        } else if (hovered) {
            g.fillGradient(x1, y1, x2, y2, T(0x22FFE7B0), T(0x10FFE7B0));
        }
    }

    public static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        int[] spans = Disc.spans(r);
        for (int dy = -r; dy <= r; dy++) {
            int hw = spans[dy + r];
            g.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, color);
        }
    }

    public static int mix(int a, int b, float t) {
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) (aa + (ba - aa) * t) << 24 | (int) (ar + (br - ar) * t) << 16
                | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }

    // ---- imperial document primitives (паспортный разворот) ----

    /** Страница документа: светлый пергамент + двойная рамка. */
    public static void page(GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.fill(x1, y1, x2, y2, PARCHMENT_EDGE);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, PARCHMENT);
        g.fillGradient(x1 + 1, y1 + 1, x2 - 1, y1 + 8, 0xFFFFFFFF, 0x00FFFFFF);
        g.fillGradient(x1 + 1, y2 - 8, x2 - 1, y2 - 1, 0x00A06B3D, 0x5AA06B3D);
        rect(g, x1 + 3, y1 + 3, x2 - 3, y2 - 3, INK_LINE);
    }

    /** Корешок разворота: тень сшива + двойная вертикальная линейка. */
    public static void spine(GuiGraphics g, int x, int y1, int y2) {
        g.fill(x - 2, y1, x - 1, y2, 0xFF8A6B42);
        g.fill(x - 1, y1, x, y2, 0xFF5A4226);
        g.fill(x, y1, x + 1, y2, PARCHMENT_EDGE);
        g.fill(x + 1, y1, x + 2, y2, INK_LINE);
    }

    /** Обложка паспорта: красный задник чуть больше разворота + золотое тиснение. */
    public static void cover(GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0x90000000);
        g.fill(x1, y1, x2, y2, COVER_DEEP);
        g.fillGradient(x1 + 1, y1 + 1, x2 - 1, y2 - 1, COVER_RED, COVER_DEEP);
        // фактура коленкора: редкие тёмные вкрапления
        for (int yy = y1 + 3; yy < y2 - 2; yy += 4) {
            g.fill(x1 + 2, yy, x2 - 2, yy + 1, 0x1A000000);
        }
        rect(g, x1 + 3, y1 + 3, x2 - 3, y2 - 3, COVER_GOLD);
        // уголки тиснения
        int c = 7;
        g.fill(x1 + 3, y1 + 3, x1 + 3 + c, y1 + 5, COVER_GOLD);
        g.fill(x1 + 3, y1 + 3, x1 + 5, y1 + 3 + c, COVER_GOLD);
        g.fill(x2 - 3 - c, y1 + 3, x2 - 3, y1 + 5, COVER_GOLD);
        g.fill(x2 - 5, y1 + 3, x2 - 3, y1 + 3 + c, COVER_GOLD);
        g.fill(x1 + 3, y2 - 5, x1 + 3 + c, y2 - 3, COVER_GOLD);
        g.fill(x1 + 3, y2 - 3 - c, x1 + 5, y2 - 3, COVER_GOLD);
        g.fill(x2 - 3 - c, y2 - 5, x2 - 3, y2 - 3, COVER_GOLD);
        g.fill(x2 - 5, y2 - 3 - c, x2 - 3, y2 - 3, COVER_GOLD);
    }

    /** Заголовок страницы: центрированный титул с ромбами по бокам. */
    public static void pageTitle(GuiGraphics g, Font font, int cx, int y, int halfRule, String title) {
        int tw = font.width(title);
        g.drawString(font, title, cx - tw / 2, y, INK, false);
        int ruleY = y + 4;
        g.fill(cx - halfRule, ruleY, cx - tw / 2 - 8, ruleY + 1, INK_LINE);
        g.fill(cx + tw / 2 + 8, ruleY, cx + halfRule, ruleY + 1, INK_LINE);
        diamond(g, cx - tw / 2 - 5, ruleY - 1, INK_LINE);
        diamond(g, cx + tw / 2 + 4, ruleY - 1, INK_LINE);
    }

    /** Горизонтальная разделительная линейка документа с ромбом по центру. */
    public static void docRule(GuiGraphics g, int x1, int x2, int y) {
        g.fill(x1, y, x2, y + 1, INK_LINE);
        diamond(g, (x1 + x2) / 2, y - 1, INK_LINE);
    }

    public static void diamond(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 1, y, x + 2, y + 3, color);
        g.fill(x, y + 1, x + 3, y + 2, color);
    }

    /**
     * Поле документа: подпись сверху, значение под ней, подчёркивание.
     * @return занятая высота.
     */
    public static int docField(GuiGraphics g, Font font, int x, int y, int w,
                               String label, String value, int valueColor, int maxLines) {
        g.drawString(font, label, x, y, INK_FAINT, false);
        int vy = y + 10;
        var lines = font.split(Component.literal(value), w);
        int n = Math.min(Math.max(1, lines.size()), Math.max(1, maxLines));
        for (int i = 0; i < n; i++) {
            g.drawString(font, lines.get(i), x, vy + i * 9, valueColor, false);
        }
        int h = 10 + n * 9 + 2;
        g.fill(x, y + h, x + w, y + h + 1, INK_LINE);
        return h + 2;
    }

    /** Сургучная печать: тёмный обод + красный диск + внутреннее кольцо + блик. */
    public static void waxSeal(GuiGraphics g, int cx, int cy, int r) {
        disc(g, cx, cy, r, SEAL_RED_DARK);
        disc(g, cx, cy, r - 1, SEAL_RED);
        disc(g, cx, cy, r - 5, SEAL_RED_DARK);
        disc(g, cx, cy, r - 6, SEAL_RED);
        g.fill(cx - r / 2, cy - r / 2 - 1, cx - r / 2 + 4, cy - r / 2, 0x66FFFFFF);
        g.fill(cx - r / 2, cy - r / 2, cx - r / 2 + 2, cy - r / 2 + 1, 0x66FFFFFF);
    }

    /**
     * Почтовая марка: зубчатый край, внутренняя рамка, сепия-вуаль если заблокирована.
     * Иконку и подписи рисует вызывающий код во внутренней области (отступ 5px).
     */
    public static void stamp(GuiGraphics g, int x, int y, int w, int h, boolean unlocked, boolean hovered) {
        g.fill(x, y, x + w, y + h, unlocked ? STAMP_BG : PARCHMENT_DEEP);
        // зубцы перфорации
        int step = 5, tooth = 3;
        for (int px = x; px < x + w; px += step) {
            int ex = Math.min(px + tooth, x + w);
            g.fill(px, y - 1, ex, y + 1, PARCHMENT);
            g.fill(px, y + h - 1, ex, y + h + 1, PARCHMENT);
        }
        for (int py = y; py < y + h; py += step) {
            int ey = Math.min(py + tooth, y + h);
            g.fill(x - 1, py, x + 1, ey, PARCHMENT);
            g.fill(x + w - 1, py, x + w + 1, ey, PARCHMENT);
        }
        rect(g, x + 1, y + 1, x + w - 1, y + h - 1, INK_LINE);
        if (hovered) {
            rect(g, x, y, x + w, y + h, unlocked ? SEAL_RED : INK_FAINT);
        }
        if (!unlocked) {
            g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0x6B5A4A3A);
        }
    }

    /** Иконка лупы в поле поиска. */
    public static void searchIcon(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 2, y + 8, color);
        g.fill(x, y + 6, x + 8, y + 8, color);
        g.fill(x + 6, y + 2, x + 8, y + 6, color);
    }

    /** Вертикальная полоса прокрутки. */
    public static void scrollBar(GuiGraphics g, int x, int y1, int y2, int contentH, int scroll, boolean focused, int mx, int my) {
        int trackH = y2 - y1;
        if (contentH <= trackH) {
            return;
        }
        int thumbH = Math.max(16, trackH * trackH / contentH);
        int thumbY = y1 + (trackH - thumbH) * scroll / (contentH - trackH);
        boolean hover = mx >= x - 1 && mx <= x + 4 && my >= y1 && my <= y2;
        g.fill(x - 1, y1, x + 4, y2, hover || focused ? 0x30FFFFFF : 0x10FFFFFF);
        g.fill(x, thumbY, x + 3, thumbY + thumbH, hover || focused ? 0x80FFFFFF : 0x40FFFFFF);
    }
}

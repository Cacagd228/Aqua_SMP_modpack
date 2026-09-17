package me.nanorasmus.nanodev.hex_js.client.render;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.render.RenderLib;
import kotlin.Pair;
import me.nanorasmus.nanodev.hex_js.helpers.PatternTextUtils;
import me.nanorasmus.nanodev.hex_js.helpers.PatternTextUtils.MarkedSpan;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec2;

import java.util.List;

/**
 * Glyph-aware text measuring for pattern markers.
 *
 * <p>The renderer ({@code MixinTextDrawerPatSty}) substitutes each
 * {@code <dir,sig>} marker with a drawn glyph and advances by a custom width.
 * Every place that <i>measures</i> field text with plain font metrics
 * (click mapping, cursor/highlight placement, visible-window cuts, scrolling)
 * must use these helpers instead, otherwise the caret lands in the wrong spot
 * and cut-off markers degrade into a lone {@code "<"}.
 *
 * <p>The advance replicates the drawer math exactly; keep them in sync.
 */
public final class PatternGlyphMetrics {
    private PatternGlyphMetrics() {
    }

    /** Advance the drawer applies per glyph (drawer: {@code x += patWidth * scale + 1}). */
    public static float advanceOf(HexPattern pattern) {
        Pair<Float, List<Vec2>> pair = RenderLib.getCenteredPattern(pattern, 128f, 128f, 16f);
        List<Vec2> dots = pair.getSecond();
        List<Vec2> zappy = RenderLib.makeZappy(
            dots, RenderLib.findDupIndices(pattern.positions()),
            10, 0.8f, 0f, 0f,
            RenderLib.DEFAULT_READABILITY_OFFSET, RenderLib.DEFAULT_LAST_SEGMENT_LEN_PROP, 0.0);
        if (zappy.isEmpty()) {
            return 1f;
        }
        float minY = 1000000f;
        float maxY = -1000000f;
        float minX = 1000000f;
        float maxX = -1000000f;
        for (Vec2 p : zappy) {
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
        }
        float patWidth = maxX - minX;
        float patHeight = maxY - minY;
        float scale = (9f - (1.8f * 0.75f)) / Math.max(patHeight, 48);
        return patWidth * scale + 1f;
    }

    /** Width as rendered: plain runs measured normally, markers by glyph advance. */
    public static float glyphWidth(Font font, String s, List<MarkedSpan> spans) {        float w = 0f;
        int i = 0;
        for (MarkedSpan m : spans) {
            w += font.width(s.substring(i, m.start()));
            w += advanceOf(m.pattern());
            i = m.end();
        }
        w += font.width(s.substring(i));
        return w;
    }

    public static float glyphWidth(Font font, String s) {
        return glyphWidth(font, s, PatternTextUtils.markedSpans(s));
    }

    /**
     * Longest prefix with glyph width {@code <= pixels}, never cutting inside
     * a marker (backs off before it). Mirrors {@code plainSubstrByWidth}.
     */
    public static String glyphSubstr(Font font, String s, int pixels) {
        int end = 0;
        float used = 0f;
        for (MarkedSpan m : PatternTextUtils.markedSpans(s)) {
            String head = s.substring(end, m.start());
            int room = (int) Math.floor(pixels - used);
            String fit = room < 0 ? "" : font.plainSubstrByWidth(head, room);
            end += fit.length();
            if (fit.length() < head.length()) {
                return s.substring(0, end);
            }
            float adv = advanceOf(m.pattern());
            if (used + font.width(head) + adv > pixels) {
                return s.substring(0, end);
            }
            used += font.width(head) + adv;
            end = m.end();
        }
        String tail = s.substring(end);
        int room = (int) Math.floor(pixels - used);
        if (room < 0) {
            return s.substring(0, end);
        }
        return s.substring(0, end + font.plainSubstrByWidth(tail, room).length());
    }

    /**
     * Longest fitting suffix, never cutting inside a marker. Mirrors
     * {@code plainSubstrByWidth} with reverse order.
     */
    public static String glyphSubstrReverse(Font font, String s, int pixels) {
        List<MarkedSpan> spans = PatternTextUtils.markedSpans(s);
        int start = s.length();
        float used = 0f;
        for (int k = spans.size() - 1; k >= 0; k--) {
            MarkedSpan m = spans.get(k);
            String tail = s.substring(m.end(), start);
            int room = (int) Math.floor(pixels - used);
            String fit = room < 0 ? "" : font.plainSubstrByWidth(tail, room, true);
            if (fit.length() < tail.length()) {
                return s.substring(start - fit.length());
            }
            used += font.width(tail);
            float adv = advanceOf(m.pattern());
            if (used + adv > pixels) {
                return s.substring(m.end());
            }
            used += adv;
            start = m.start();
        }
        String head = s.substring(0, start);
        int room = (int) Math.floor(pixels - used);
        if (room < 0) {
            return s.substring(start);
        }
        String fit = font.plainSubstrByWidth(head, room, true);
        return s.substring(start - fit.length());
    }

}

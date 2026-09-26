package me.nanorasmus.nanodev.hex_js.client.render;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.render.RenderLib;
import kotlin.Pair;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global cache for pattern glyph geometry.
 *
 * <p>Previously every glyph paid the full pipeline per frame ({@code getCenteredPattern}
 * + {@code findDupIndices} + {@code makeZappy} with 3x Simplex noise per sub-point),
 * and again per width measurement. Chat re-renders every visible line every frame,
 * so a few hundred runes collapsed the framerate.
 *
 * <p>This cache computes the expensive parts once per distinct {@link HexPattern}
 * (frozen noise, {@code speed=0}) and reuses them. Animated styles
 * (italic/obfuscated) still run {@code makeZappy} per frame, but reuse the cached
 * centered dots + dup indices instead of recomputing them. Visuals are unchanged:
 * bounds/advance replicate the previous formulas exactly.
 */
public final class PatternGlyphCache {
    private PatternGlyphCache() {
    }

    private static final float RENDER_SIZE = 128f;
    private static final float LINE_WIDTH = 1.8f;
    private static final int MAX_ENTRIES = 2048;

    private static final ConcurrentHashMap<HexPattern, Cached> CACHE = new ConcurrentHashMap<>();

    public static final class Cached {
        public final List<Vec2> dots;
        public final Set<Integer> dupIndices;
        /** Wiggly (variance 0.8, frozen) — used for bounds + advance, as before. */
        public final List<Vec2> zappyStill;
        /** Straight (variance 0, frozen) — what a normal non-italic glyph draws. */
        public final List<Vec2> zappyStatic;
        public final List<Vec2> pathfinderDots;
        public final boolean empty;
        // Bounds of zappyStill.
        public final float minX;
        public final float maxX;
        public final float minY;
        public final float maxY;
        public final float patWidthF;
        public final float patHeightF;
        public final int patWidthI;
        public final int patHeightI;
        /** Drawer scale: (9 - 1.8*0.75) / max(patHeightI, 48). */
        public final float drawerScale;
        /** Drawer advance: patWidthI * drawerScale + 1. */
        public final float drawerAdvance;
        /** Metrics advance: patWidthF * ((9 - 1.8*0.75) / max(patHeightF, 48)). */
        public final float metricsAdvance;

        private Cached(List<Vec2> dots, Set<Integer> dupIndices, List<Vec2> zappyStill,
                       List<Vec2> zappyStatic, float minX, float maxX, float minY, float maxY) {
            this.dots = dots;
            this.dupIndices = dupIndices;
            this.zappyStill = zappyStill;
            this.zappyStatic = zappyStatic;
            this.pathfinderDots = dots;
            this.empty = zappyStill == null || zappyStill.isEmpty();
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.patWidthF = maxX - minX;
            this.patHeightF = maxY - minY;
            this.patWidthI = (int) this.patWidthF;
            this.patHeightI = (int) this.patHeightF;
            float base = 9f - (LINE_WIDTH * 0.75f);
            this.drawerScale = base / Math.max(this.patHeightI, 48);
            this.drawerAdvance = this.patWidthI * this.drawerScale + 1f;
            this.metricsAdvance = this.patWidthF * (base / Math.max(this.patHeightF, 48));
        }
    }

    public static Cached get(HexPattern pattern) {
        Cached hit = CACHE.get(pattern);
        if (hit != null) {
            return hit;
        }
        Cached computed = compute(pattern);
        if (CACHE.size() > MAX_ENTRIES) {
            CACHE.clear();
        }
        Cached prev = CACHE.putIfAbsent(pattern, computed);
        return prev != null ? prev : computed;
    }

    /** Metrics advance; returns 1f for empty patterns (previous behavior). */
    public static float advanceOf(HexPattern pattern) {
        if (pattern == null) {
            return 1f;
        }
        Cached c = get(pattern);
        if (c.empty) {
            return 1f;
        }
        return c.metricsAdvance;
    }

    private static Cached compute(HexPattern pattern) {
        Pair<Float, List<Vec2>> pair = RenderLib.getCenteredPattern(pattern, RENDER_SIZE, RENDER_SIZE, 16f);
        List<Vec2> dots = pair.getSecond();
        Set<Integer> dup = RenderLib.findDupIndices(pattern.positions());
        List<Vec2> still = RenderLib.makeZappy(
            dots, dup,
            10, 0.8f, 0f, 0f,
            RenderLib.DEFAULT_READABILITY_OFFSET, RenderLib.DEFAULT_LAST_SEGMENT_LEN_PROP, 0.0);
        // Straight variant for the common static draw (variance 0). Computed once here
        // instead of per-frame per-glyph; result matches the old per-frame animated call.
        List<Vec2> statik = RenderLib.makeZappy(
            dots, dup,
            10, 0f, 0f, 0f,
            RenderLib.DEFAULT_READABILITY_OFFSET, RenderLib.DEFAULT_LAST_SEGMENT_LEN_PROP, 0.0);
        // With variance 0 every sub-point is exactly collinear with its segment, so the
        // ~10x subdivision only adds coplanar triangles in drawLineSeq (joins on straight
        // points emit nothing). Drop them: same pixels, ~10x fewer vertices per frame.
        statik = decimateCollinear(statik);
        float minX = 1000000f;
        float maxX = -1000000f;
        float minY = 1000000f;
        float maxY = -1000000f;
        for (Vec2 p : still) {
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }
        if (still.isEmpty()) {
            minX = maxX = minY = maxY = 0f;
        }
        return new Cached(
            Collections.unmodifiableList(dots),
            Collections.unmodifiableSet(dup),
            Collections.unmodifiableList(still),
            Collections.unmodifiableList(statik),
            minX, maxX, minY, maxY);
    }

    /**
     * Removes intermediate points that lie on the straight line between their neighbors.
     * First and last points are always kept (line caps anchor on them). A point is dropped
     * only when it continues the same direction (cross ~= 0 and dot &gt; 0), so corners and
     * go-and-return joints are preserved.
     */
    private static List<Vec2> decimateCollinear(List<Vec2> points) {
        int n = points.size();
        if (n <= 2) {
            return points;
        }
        ArrayList<Vec2> out = new ArrayList<>(n);
        Vec2 a = points.get(0);
        out.add(a);
        for (int i = 1; i < n - 1; i++) {
            Vec2 b = points.get(i);
            Vec2 c = points.get(i + 1);
            float abx = b.x - a.x;
            float aby = b.y - a.y;
            float bcx = c.x - b.x;
            float bcy = c.y - b.y;
            float lenSq = (abx * abx + aby * aby) * (bcx * bcx + bcy * bcy);
            boolean drop = false;
            if (lenSq > 0f) {
                float cross = abx * bcy - aby * bcx;
                float dot = abx * bcx + aby * bcy;
                // Relative epsilon: exact-collinear (variance 0) intermediates have cross ~= 1e-7.
                drop = dot > 0f && cross * cross <= 1e-12f * lenSq;
            } else {
                // Zero-length step (exact duplicate): collapsing it also avoids NaN
                // normals in drawLineSeq.
                drop = true;
            }
            if (drop) {
                continue;
            }
            out.add(b);
            a = b;
        }
        out.add(points.get(n - 1));
        return out;
    }
}

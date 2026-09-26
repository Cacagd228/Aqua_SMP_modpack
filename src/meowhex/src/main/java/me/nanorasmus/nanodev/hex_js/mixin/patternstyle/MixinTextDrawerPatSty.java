package me.nanorasmus.nanodev.hex_js.mixin.patternstyle;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.render.RenderLib;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import me.nanorasmus.nanodev.hex_js.client.render.PatternGlyphCache;
import me.nanorasmus.nanodev.hex_js.client.render.PatternStyle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws an actual pattern glyph instead of a character when the incoming style
 * carries a {@link HexPattern} (ported from HexGloop's text-pattern drawer).
 */
@Mixin(targets = "net.minecraft.client.gui.Font$StringRenderOutput")
public abstract class MixinTextDrawerPatSty {

    @Shadow
    float x;
    @Shadow
    float y;
    @Shadow
    @Final
    private Matrix4f pose;

    @Inject(method = "accept(ILnet/minecraft/network/chat/Style;I)Z", at = @At("HEAD"), cancellable = true)
    private void hexjs$drawPatternGlyph(int index, Style style, int codepoint, CallbackInfoReturnable<Boolean> cir) {
        PatternStyle pStyle = (PatternStyle) style;
        if (pStyle.getPattern() == null) {
            return;
        }
        HexPattern pattern = pStyle.getPattern();

        float speed = 0f;
        float variance = 0f;
        if (style.isItalic() && !style.isObfuscated()) {
            speed = 0.05f;
            variance = 0.2f;
        } else if (style.isObfuscated() && !style.isItalic()) {
            speed = 0.1f;
            variance = 0.8f;
        } else if (style.isObfuscated()) {
            speed = 0.15f;
            variance = 3f;
        }

        PatternGlyphCache.Cached cached = PatternGlyphCache.get(pattern);
        if (cached.empty) {
            return;
        }

        final List<Vec2> zappyAnimated;
        final boolean isStatic = speed == 0f && variance == 0f;
        if (isStatic) {
            // Static fast-path: frozen straight geometry, no noise / center recompute.
            zappyAnimated = cached.zappyStatic;
        } else {
            zappyAnimated = RenderLib.makeZappy(
                cached.dots, cached.dupIndices,
                10, variance, speed, 0f,
                RenderLib.DEFAULT_READABILITY_OFFSET, RenderLib.DEFAULT_LAST_SEGMENT_LEN_PROP, 0.0);
        }
        List<Vec2> pathfinderDots = cached.pathfinderDots;
        if (zappyAnimated.isEmpty()) {
            return;
        }

        int patWidth = cached.patWidthI;
        int patHeight = cached.patHeightI;

        float scale = cached.drawerScale;
        float lineWidth = 1.8f;
        float dotWidth = 0.6f;
        float startingDotWidth = 1.25f;
        float lineScale = 1f;
        if (scale / 0.5f < 0.5f) lineScale /= 1.5f;
        if (scale / 0.5f < 0.25f) lineScale /= 1.5f;
        if (scale / 0.5f < 0.125f) lineScale /= 1.5f;
        if (scale / 0.5f < 0.0625f) lineScale /= 1.5f;

        if ((style.isStrikethrough() && style.getColor() != null && style.getColor().getValue() == 0xFFFFFF)
            || style.getColor() == null) {
            lineScale *= 0.75f;
        }

        List<Vec2> zappyPoints = new ArrayList<>(zappyAnimated.size());
        for (Vec2 p : zappyAnimated) {
            zappyPoints.add(new Vec2(scale * p.x + this.x + scale * patWidth / 2f,
                scale * p.y + this.y + scale * patHeight / 2f));
        }

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE_MINUS_SRC_ALPHA);

        int color = 0xFFFFFFFF;
        if (style.getColor() != null) {
            color = style.getColor().getValue();
        }

        Matrix4f mat = new Matrix4f(this.pose);
        mat.translate(0f, 0f, 0.011f);

        if (isStatic && me.nanorasmus.nanodev.hex_js.client.HexJsClientConfig.SIMPLE_STATIC_GLYPHS.get()) {
            hexjs$drawLineStrip(mat, zappyPoints, color);
            this.x += patWidth * scale + 1f;
            cir.setReturnValue(true);
            return;
        }

        List<Vec2> drawnDots = new ArrayList<>(pathfinderDots.size());
        for (Vec2 p : pathfinderDots) {
            drawnDots.add(new Vec2(scale * p.x + this.x + scale * patWidth / 2f,
                scale * p.y + this.y + scale * patHeight / 2f));
        }

        int innerLight = (color & 0x00FFFFFF) | 0xC8000000;
        int innerDark = (color & 0x00FFFFFF) | 0x80000000;

        RenderLib.drawLineSeq(mat, zappyPoints, lineWidth * lineScale, 0, 0xFFFFFFFF, 0xFFFFFFFF);
        RenderLib.drawLineSeq(mat, zappyPoints, lineWidth * 0.4f * lineScale, 0.01f, innerDark, innerLight);

        Matrix4f dotMat = new Matrix4f(mat);
        dotMat.translate(0f, 0f, -0.98f);

        // Alpha is 0 when not bold: previously still issued a draw call, now skipped.
        if (style.isBold()) {
            RenderLib.drawSpot(dotMat, zappyPoints.get(0), startingDotWidth * lineScale,
                FastColor.ARGB32.red(color) / 255f, FastColor.ARGB32.green(color) / 255f,
                FastColor.ARGB32.blue(color) / 255f, 0.7f);
        }

        dotMat.translate(0f, 0f, 0.005f);
        hexjs$drawDotsBatched(dotMat, drawnDots, dotWidth * lineScale, 0.82f, 0.8f, 0.8f, 0.5f);

        this.x += patWidth * scale + 1f;
        cir.setReturnValue(true);
    }

    /**
     * Minimal static glyph: one colored line strip, one GPU submission, no caps/dots.
     * Same screen polyline and advance as the full path, so layout is untouched.
     */
    private static void hexjs$drawLineStrip(Matrix4f mat, List<Vec2> points, int color) {
        if (points.size() < 2) {
            return;
        }
        float r = FastColor.ARGB32.red(color) / 255f;
        float g = FastColor.ARGB32.green(color) / 255f;
        float b = FastColor.ARGB32.blue(color) / 255f;
        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (Vec2 p : points) {
            buf.addVertex(mat, p.x, p.y, 0f).setColor(r, g, b, 1f);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
    }

    /**
     * Same pixels as one {@link RenderLib#drawSpot} per dot, but a single buffer/draw call
     * per glyph instead of one per dot. Chat patterns have ~N dots each, so this removes
     * most of the tiny GPU submissions in the hot path.
     */
    private static void hexjs$drawDotsBatched(Matrix4f mat, List<Vec2> dots, float radius,
                                              float r, float g, float b, float a) {
        if (dots.isEmpty() || radius <= 0f || a <= 0f) {
            return;
        }
        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        final int steps = 6;
        final float tau = (float) (Math.PI * 2.0);
        for (Vec2 dot : dots) {
            float cx = dot.x;
            float cy = dot.y;
            float px = Mth.cos(0f) * radius + cx;
            float py = Mth.sin(0f) * radius + cy;
            for (int i = 1; i <= steps; i++) {
                float theta = i / (float) steps * tau;
                float rx = Mth.cos(theta) * radius + cx;
                float ry = Mth.sin(theta) * radius + cy;
                buf.addVertex(mat, cx, cy, 1f).setColor(r, g, b, a);
                buf.addVertex(mat, px, py, 1f).setColor(r, g, b, a);
                buf.addVertex(mat, rx, ry, 1f).setColor(r, g, b, a);
                px = rx;
                py = ry;
            }
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
    }
}
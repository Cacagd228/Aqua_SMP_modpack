package me.nanorasmus.nanodev.hex_js.mixin.patternstyle;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.render.RenderLib;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import kotlin.Pair;
import me.nanorasmus.nanodev.hex_js.client.render.PatternStyle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FastColor;
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

    private static final float RENDER_SIZE = 128f;

    @Inject(method = "accept(ILnet/minecraft/network/chat/Style;I)Z", at = @At("HEAD"), cancellable = true)
    private void hexjs$drawPatternGlyph(int index, Style style, int codepoint, CallbackInfoReturnable<Boolean> cir) {
        PatternStyle pStyle = (PatternStyle) style;
        if (pStyle.getPattern() == null) {
            return;
        }
        HexPattern pattern = pStyle.getPattern();
        Pair<Float, List<Vec2>> pair = RenderLib.getCenteredPattern(pattern, RENDER_SIZE, RENDER_SIZE, 16f);
        List<Vec2> dots = pair.getSecond();

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

        List<Vec2> zappyAnimated = RenderLib.makeZappy(
            dots, RenderLib.findDupIndices(pattern.positions()),
            10, variance, speed, 0f,
            RenderLib.DEFAULT_READABILITY_OFFSET, RenderLib.DEFAULT_LAST_SEGMENT_LEN_PROP, 0.0);
        List<Vec2> zappyStill = pStyle.getZappyPoints();
        List<Vec2> pathfinderDots = pStyle.getPathfinderDots();
        if (zappyStill == null || pathfinderDots == null || zappyStill.isEmpty() || zappyAnimated.isEmpty()) {
            return;
        }

        float minY = 1000000f;
        float maxY = -1000000f;
        float minX = 1000000f;
        float maxX = -1000000f;
        for (Vec2 p : zappyStill) {
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
        }

        float lineWidth = 1.8f;
        float dotWidth = 0.6f;
        float startingDotWidth = 1.25f;

        int patWidth = (int) (maxX - minX);
        int patHeight = (int) (maxY - minY);

        float scale = (9f - (lineWidth * 0.75f)) / Math.max(patHeight, 48);
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
        List<Vec2> drawnDots = new ArrayList<>(pathfinderDots.size());
        for (Vec2 p : pathfinderDots) {
            drawnDots.add(new Vec2(scale * p.x + this.x + scale * patWidth / 2f,
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
        int innerLight = (color & 0x00FFFFFF) | 0xC8000000;
        int innerDark = (color & 0x00FFFFFF) | 0x80000000;

        Matrix4f mat = new Matrix4f(this.pose);
        mat.translate(0f, 0f, 0.011f);

        RenderLib.drawLineSeq(mat, zappyPoints, lineWidth * lineScale, 0, 0xFFFFFFFF, 0xFFFFFFFF);
        RenderLib.drawLineSeq(mat, zappyPoints, lineWidth * 0.4f * lineScale, 0.01f, innerDark, innerLight);

        Matrix4f dotMat = new Matrix4f(mat);
        dotMat.translate(0f, 0f, -0.98f);

        RenderLib.drawSpot(dotMat, zappyPoints.get(0), startingDotWidth * lineScale,
            FastColor.ARGB32.red(color) / 255f, FastColor.ARGB32.green(color) / 255f,
            FastColor.ARGB32.blue(color) / 255f, style.isBold() ? 0.7f : 0f);

        dotMat.translate(0f, 0f, 0.005f);
        for (Vec2 dot : drawnDots) {
            RenderLib.drawSpot(dotMat, dot, dotWidth * lineScale, 0.82f, 0.8f, 0.8f, 0.5f);
        }

        this.x += patWidth * scale + 1f;
        cir.setReturnValue(true);
    }
}
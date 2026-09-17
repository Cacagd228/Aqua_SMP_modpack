package me.nanorasmus.nanodev.hex_js.mixin.patternstyle;

import me.nanorasmus.nanodev.hex_js.client.render.PatternGlyphMetrics;
import me.nanorasmus.nanodev.hex_js.helpers.PatternTextUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

/**
 * Makes pattern markers ({@code <dir,sig>}) atomic in every text field.
 * The renderer ({@code MixinParsePatternFormatting}) substitutes each marker
 * with a single glyph, so the caret, selection and deletion must treat it as
 * one symbol — otherwise editing tears it back into raw text. Spans that
 * don't parse to a real pattern stay plain editable text.
 */
@Mixin(EditBox.class)
public abstract class MixinEditBoxAtomicMarkers {

    @Shadow
    private String value;
    @Shadow
    private int cursorPos;
    @Shadow
    private int highlightPos;
    @Shadow
    private Predicate<String> filter;

    @Shadow
    public abstract void setValue(String text);

    @Shadow
    public abstract void moveCursorTo(int pos, boolean select);

    @ModifyVariable(method = "setCursorPosition(I)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int hexjs$snapCursorPosition(int pos) {
        return snapToMarkerBoundary(this.value, this.cursorPos, pos);
    }

    @ModifyVariable(method = "setHighlightPos(I)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int hexjs$snapHighlightPos(int pos) {
        return snapToMarkerBoundary(this.value, this.cursorPos, pos);
    }

    @Inject(method = "deleteCharsToPos(I)V", at = @At("HEAD"), cancellable = true)
    private void hexjs$deleteWholeMarker(int target, CallbackInfo ci) {
        if (this.highlightPos != this.cursorPos || this.value.isEmpty() || target == this.cursorPos) {
            return;
        }
        // Expand the deletion range over every overlapped marker so glyphs
        // always disappear whole (incl. Ctrl+Backspace landing on the space
        // inside "<dir sig>").
        int lo = Math.min(target, this.cursorPos);
        int hi = Math.max(target, this.cursorPos);
        int newLo = lo;
        int newHi = hi;
        for (int[] span : PatternTextUtils.markerSpans(this.value)) {
            if (span[0] < newHi && span[1] > newLo) {
                newLo = Math.min(newLo, span[0]);
                newHi = Math.max(newHi, span[1]);
            }
        }
        if (newLo != lo || newHi != hi) {
            String s = new StringBuilder(this.value).delete(newLo, newHi).toString();
            if (this.filter.test(s)) {
                this.setValue(s);
                this.moveCursorTo(newLo, false);
            }
            ci.cancel();
        }
    }

    private static int snapToMarkerBoundary(String value, int from, int to) {
        if (value == null || to <= 0 || to >= value.length()) {
            return to;
        }
        for (int[] span : PatternTextUtils.markerSpans(value)) {
            if (to > span[0] && to < span[1]) {
                return to > from ? span[1] : span[0];
            }
        }
        return to;
    }

    // -- Glyph-aware measuring: the field renders substituted glyphs, so plain
    // font metrics (raw marker width) would misplace clicks, caret, highlight
    // and cut markers in half (lone "<"). Redirects below keep them in sync.

    @Redirect(method = "onClick(DD)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;plainSubstrByWidth(Ljava/lang/String;I)Ljava/lang/String;"))
    private String hexjs$glyphSubstrClick(Font font, String s, int pixels) {
        return PatternGlyphMetrics.glyphSubstr(font, s, pixels);
    }

    @Redirect(method = "renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;plainSubstrByWidth(Ljava/lang/String;I)Ljava/lang/String;"))
    private String hexjs$glyphSubstrRender(Font font, String s, int pixels) {
        return PatternGlyphMetrics.glyphSubstr(font, s, pixels);
    }

    @Redirect(method = "renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int hexjs$glyphWidthRender(Font font, String s) {
        return Math.round(PatternGlyphMetrics.glyphWidth(font, s));
    }

    @Redirect(method = "scrollTo(I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;plainSubstrByWidth(Ljava/lang/String;I)Ljava/lang/String;"))
    private String hexjs$glyphSubstrScroll(Font font, String s, int pixels) {
        return PatternGlyphMetrics.glyphSubstr(font, s, pixels);
    }

    @Redirect(method = "scrollTo(I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;plainSubstrByWidth(Ljava/lang/String;IZ)Ljava/lang/String;"))
    private String hexjs$glyphSubstrScrollReverse(Font font, String s, int pixels, boolean reverse) {
        return PatternGlyphMetrics.glyphSubstrReverse(font, s, pixels);
    }

    @Redirect(method = "getScreenX(I)I",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int hexjs$glyphWidthScreenX(Font font, String s) {
        return Math.round(PatternGlyphMetrics.glyphWidth(font, s));
    }
}

package me.nanorasmus.nanodev.hex_js.mixin.patternstyle;

import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import me.nanorasmus.nanodev.hex_js.client.render.PatternStyle;
import me.nanorasmus.nanodev.hex_js.helpers.PatternTextUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses {@code "<dir,sig>"} (and {@code HexPattern(...)}) text markers while text
 * is being decomposed for rendering, converting them into pattern-styled glyphs.
 * Lets you type patterns in chat, on signs, etc. (ported from HexGloop).
 */
@Mixin(StringDecomposer.class)
public abstract class MixinParsePatternFormatting {

    private static final Pattern PATTERN_PATTERN_REGEX = Pattern.compile(
        "\\A(?<escaped>\\\\?)(HexPattern)?[<(\\[{]\\s*(?<direction>[a-z_-]+)(?:\\s*[, ]\\s*(?<pattern>[aqweds]+))?\\s*[>)\\]}]",
        Pattern.CASE_INSENSITIVE);

    @WrapOperation(method = "iterate(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/StringDecomposer;feedChar(Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;IC)Z"))
    private static boolean hexjs$parsePatternFormattingPlain(Style style, FormattedCharSink sink, int index, char c,
                                                             Operation<Boolean> original,
                                                             @Local(ordinal = 1) LocalIntRef jref,
                                                             @Local(ordinal = 0) String text) {
        return tryParsePattern(text, jref, style, sink, index, c, original);
    }

    @WrapOperation(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/StringDecomposer;feedChar(Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;IC)Z"))
    private static boolean hexjs$parsePatternFormatting(Style style, FormattedCharSink sink, int index, char c,
                                                        Operation<Boolean> original,
                                                        @Local(ordinal = 2) LocalIntRef jref,
                                                        @Local(ordinal = 0) String text) {
        return tryParsePattern(text, jref, style, sink, index, c, original);
    }

    private static boolean tryParsePattern(String text, LocalIntRef jref, Style style, FormattedCharSink sink,
                                           int index, char c, Operation<Boolean> original) {
        int startishIndex = jref.get();
        String remainingText = text.substring(startishIndex);
        Matcher matcher = PATTERN_PATTERN_REGEX.matcher(remainingText);
        if (!matcher.find()) {
            return original.call(style, sink, index, c);
        }
        if (!matcher.group("escaped").isEmpty()) {
            // Escaped with a backslash — emit the marker as plain text, skipping the backslash.
            int endIndex = matcher.end();
            for (int i = 1; i < endIndex; i++) {
                sink.accept(startishIndex + i, style, text.charAt(startishIndex + i));
            }
            jref.set(startishIndex + matcher.end() - 1);
            return jref.get() < text.length();
        }
        String dirString = matcher.group("direction").toLowerCase(Locale.ROOT).strip().replace("_", "");
        HexDir dir = PatternTextUtils.DIR_MAP.get(dirString);
        if (dir == null) {
            return original.call(style, sink, index, c);
        }
        String angleSigs = matcher.group("pattern");
        HexPattern pattern = parsePattern(angleSigs, dir);
        if (pattern == null) {
            return original.call(style, sink, index, c);
        }
        // Emit a single glyph character; the pattern lives in its style.
        sink.accept(startishIndex, ((PatternStyle) style).withPattern(pattern), '!');
        jref.set(startishIndex + matcher.end() - 1);
        return (startishIndex + matcher.end() - 1) < text.length();
    }

    @Nullable
    private static HexPattern parsePattern(String angleSigs, HexDir dir) {
        if (angleSigs == null || dir == null) {
            return null;
        }
        try {
            return HexPattern.fromAngles(angleSigs, dir);
        } catch (Throwable t) {
            return null;
        }
    }
}
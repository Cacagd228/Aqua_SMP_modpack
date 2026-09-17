package me.nanorasmus.nanodev.hex_js.client.render;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import me.nanorasmus.nanodev.hex_js.helpers.PatternTextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.phys.Vec2;

import java.util.List;

/**
 * Mixed into {@link Style} by {@code MixinPatternStyle}. Lets a style carry a
 * {@link HexPattern} so the text renderer draws an actual pattern glyph instead of
 * a letter. Mirrors HexGloop's PatternStyle.
 */
public interface PatternStyle {

    HexPattern getPattern();

    /** Mutates this style to carry the pattern. Never call on shared singletons. */
    Style setPattern(HexPattern pattern);

    List<Vec2> getZappyPoints();

    List<Vec2> getPathfinderDots();

    default Style withPattern(HexPattern pattern) {
        return withPattern(pattern, true, true);
    }

    default Style withPattern(HexPattern pattern, boolean withPatternHoverEvent, boolean withPatternClickEvent) {
        Style style = (Style) this;
        if (withPatternHoverEvent) {
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.literal(PatternTextUtils.patternReadable(pattern)).withStyle(ChatFormatting.WHITE)));
        }
        if (withPatternClickEvent) {
            style = style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,
                PatternTextUtils.patternMarker(pattern)));
        }
        return ((PatternStyle) style).setPattern(pattern);
    }

    static Style fromPattern(HexPattern pattern) {
        // A fresh non-empty style so we never mutate the shared EMPTY singleton.
        return ((PatternStyle) Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))).setPattern(pattern);
    }
}
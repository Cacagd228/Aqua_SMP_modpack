package me.nanorasmus.nanodev.hex_js.helpers;

import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers for the pattern-rendering feature: turning patterns into the
 * {@code "<dir,sig>"} text marker that is both parseable (rendered as a glyph by
 * the client) and survives network serialization, plus the reverse direction map.
 */
public final class PatternTextUtils {
    private PatternTextUtils() {}

    /** Human-friendly direction spellings (HexGloop StringsToDirMap). */
    public static final Map<String, HexDir> DIR_MAP = new HashMap<>();

    static {
        DIR_MAP.put("northwest", HexDir.NORTH_WEST);
        DIR_MAP.put("west", HexDir.WEST);
        DIR_MAP.put("southwest", HexDir.SOUTH_WEST);
        DIR_MAP.put("southeast", HexDir.SOUTH_EAST);
        DIR_MAP.put("east", HexDir.EAST);
        DIR_MAP.put("northeast", HexDir.NORTH_EAST);
        DIR_MAP.put("nw", HexDir.NORTH_WEST);
        DIR_MAP.put("w", HexDir.WEST);
        DIR_MAP.put("sw", HexDir.SOUTH_WEST);
        DIR_MAP.put("se", HexDir.SOUTH_EAST);
        DIR_MAP.put("e", HexDir.EAST);
        DIR_MAP.put("ne", HexDir.NORTH_EAST);
    }

    /** {@code <northwest,aqw>} — the standard text marker. */
    public static String patternMarker(HexPattern pattern) {
        String dir = pattern.getStartDir().name().toLowerCase(Locale.ROOT).replace("_", "");
        return "<" + dir + "," + pattern.anglesSignature() + ">";
    }

    /** {@code <northwest aqw>} — same marker with a space separator (still parseable). */
    public static String patternMarkerSpaced(HexPattern pattern) {
        String dir = pattern.getStartDir().name().toLowerCase(Locale.ROOT).replace("_", "");
        return "<" + dir + " " + pattern.anglesSignature() + ">";
    }

    private static final Pattern COMMA_MARKER = Pattern.compile(
        "<\\s*([a-z_-]+)\\s*,\\s*([aqweds]+)\\s*>", Pattern.CASE_INSENSITIVE);

    /**
     * Announcement flavor: no commas or square brackets in the text. Comma
     * markers become spaced (still parseable), other commas become spaces,
     * square brackets are dropped. Angle brackets are kept — they delimit markers.
     */
    public static String flattenForAnnouncement(String text) {
        if (text == null) {
            return "";
        }
        String s = COMMA_MARKER.matcher(text).replaceAll("<$1 $2>");
        return s.replace("[", "").replace("]", "").replace(',', ' ');
    }

    /** {@code NORTH_WEST aqw} — human readable. */
    public static String patternReadable(HexPattern pattern) {
        StringBuilder bob = new StringBuilder();
        bob.append(pattern.getStartDir().name());
        String sig = pattern.anglesSignature();
        if (!sig.isEmpty()) {
            bob.append(" ").append(sig);
        }
        return bob.toString();
    }

    /** Unanchored marker matcher — finds {@code <dir,sig>} anywhere in a string. */
    private static final Pattern MARKER_REGEX = Pattern.compile(
        "(HexPattern)?[<(\\[{]\\s*(?<direction>[a-z_-]+)(?:\\s*[, ]\\s*(?<pattern>[aqweds]+))?\\s*[>)\\]}]",
        Pattern.CASE_INSENSITIVE);

    /**
     * Scans {@code text} for all pattern markers ({@code <dir,sig>} /
     * {@code HexPattern(...)}) and returns the parsed patterns, in order.
     */
    public static List<HexPattern> parsePatterns(String text) {
        List<HexPattern> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        Matcher matcher = MARKER_REGEX.matcher(text);
        while (matcher.find()) {
            String dirString = matcher.group("direction").toLowerCase(Locale.ROOT).strip().replace("_", "");
            HexDir dir = DIR_MAP.get(dirString);
            if (dir == null) {
                continue;
            }
            String sig = matcher.group("pattern");
            if (sig == null || sig.isEmpty()) {
                continue;
            }
            try {
                out.add(HexPattern.fromAngles(sig, dir));
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    /**
     * Display component for a pattern iota: a parseable {@code "<dir,sig>"} marker
     * that the client renders as a glyph, with hover + copy-to-clipboard events.
     * Built server-side too so the marker survives network serialization.
     */
    public static Component patternDisplayComponent(HexPattern pattern) {
        return patternDisplayComponent(pattern, List.of(pattern));
    }

    /**
     * Display marker for a pattern that belongs to a whole set (e.g. a pattern
     * inside a displayed list/spell). The hover shows this single pattern, but the
     * click carries the WHOLE set ({@link #patternSetMarker}), so clicking any glyph
     * of a multi-pattern display copies the entire set to the stack.
     */
    public static Component patternDisplayComponent(HexPattern pattern, List<HexPattern> wholeSet) {
        Style style = Style.EMPTY
            .withColor(ChatFormatting.GOLD)
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.literal(patternReadable(pattern)).withStyle(ChatFormatting.WHITE)))
            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, patternSetMarker(wholeSet)));
        return Component.literal(patternMarker(pattern)).setStyle(style);
    }

    /** Concatenated markers of the whole set: {@code <a,x><b,y>...}. */
    public static String patternSetMarker(List<HexPattern> patterns) {
        StringBuilder bob = new StringBuilder();
        for (HexPattern pattern : patterns) {
            bob.append(patternMarker(pattern));
        }
        return bob.toString();
    }

    /**
     * {@code [start,end)} spans of markers that actually render as glyphs
     * (known direction + valid pattern). Escaped markers are plain text and
     * are skipped. Used to treat glyphs as single symbols in text fields.
     */
    public static List<int[]> markerSpans(String text) {
        List<int[]> out = new ArrayList<>();
        for (MarkedSpan m : markedSpans(text)) {
            out.add(new int[]{m.start(), m.end()});
        }
        return out;
    }

    /** Valid marker span with its parsed pattern. */
    public record MarkedSpan(int start, int end, HexPattern pattern) {
    }

    public static List<MarkedSpan> markedSpans(String text) {
        List<MarkedSpan> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        Matcher matcher = MARKER_REGEX.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            if (start > 0 && text.charAt(start - 1) == '\\') {
                continue;
            }
            String dirString = matcher.group("direction").toLowerCase(Locale.ROOT).strip().replace("_", "");
            HexDir dir = DIR_MAP.get(dirString);
            String sig = matcher.group("pattern");
            if (dir == null || sig == null || sig.isEmpty()) {
                continue;
            }
            HexPattern pattern;
            try {
                pattern = HexPattern.fromAngles(sig, dir);
            } catch (Throwable ignored) {
                continue;
            }
            out.add(new MarkedSpan(start, matcher.end(), pattern));
        }
        return out;
    }
}
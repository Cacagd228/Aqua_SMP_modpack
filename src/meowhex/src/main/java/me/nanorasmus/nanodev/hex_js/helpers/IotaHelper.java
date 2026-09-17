package me.nanorasmus.nanodev.hex_js.helpers;

import at.petrak.hexcasting.api.casting.SpellList;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexAngle;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Small pure helpers translating between the compact {@code "awe"/"deewde"} angle
 * strings used across the HexJS script API and Hex Casting's pattern objects.
 */
public final class IotaHelper {
    private IotaHelper() {
    }

    /**
     * Parses the single-char angle letters used by the original HexJS into a list of
     * {@link HexAngle}s. Unknown letters are skipped, so malformed spell strings fail
     * into shorter (or empty) patterns rather than throwing.
     */
    public static ArrayList<HexAngle> anglesFromString(String input) {
        ArrayList<HexAngle> angles = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return angles;
        }
        for (String letter : input.toLowerCase().split("")) {
            switch (letter) {
                case "a" -> angles.add(HexAngle.LEFT_BACK);
                case "q" -> angles.add(HexAngle.LEFT);
                case "w" -> angles.add(HexAngle.FORWARD);
                case "e" -> angles.add(HexAngle.RIGHT);
                case "d" -> angles.add(HexAngle.RIGHT_BACK);
                case "s" -> angles.add(HexAngle.BACK);
                default -> {
                    // Not a pattern letter; the original silently ignored it too.
                }
            }
        }
        return angles;
    }

    /** Builds a pattern starting {@link HexDir#EAST} (the convention HexJS scripts use). */
    public static HexPattern patternFromString(String angles) {
        return new HexPattern(HexDir.EAST, anglesFromString(angles));
    }

    public static PatternIota patternIotaFromString(String angles) {
        return new PatternIota(patternFromString(angles));
    }

    public static ArrayList<Iota> patternIotasFromStrings(List<String> patternAngles) {
        ArrayList<Iota> patterns = new ArrayList<>();
        for (String angles : patternAngles) {
            patterns.add(patternIotaFromString(angles));
        }
        return patterns;
    }

    /**
     * Returns a new {@link ListIota} with the element at {@code idx} replaced.
     * {@code null} when the index is out of bounds.
     */
    public static ListIota setElementAtIndexOfListIota(ListIota list, int idx, Iota iota) {
        if (list == null || idx < 0 || idx >= list.getList().size()) {
            return null;
        }
        return new ListIota(list.getList().modifyAt(idx, it -> new SpellList.LPair(iota, it.getCdr())));
    }

    /** The element at {@code idx} of a {@link ListIota}, or {@code null} out of bounds. */
    public static Iota getIotaAtIndexofListIota(ListIota list, int idx) {
        if (list == null || idx < 0 || idx >= list.getList().size()) {
            return null;
        }
        return list.getList().getAt(idx);
    }
}
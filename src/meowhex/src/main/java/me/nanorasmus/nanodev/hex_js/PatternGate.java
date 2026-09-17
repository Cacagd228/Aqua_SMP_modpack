package me.nanorasmus.nanodev.hex_js;

import at.petrak.hexcasting.api.casting.math.HexAngle;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import me.nanorasmus.nanodev.hex_js.storage.PatternList;

import java.util.ArrayList;

/**
 * Decides what to do with a freshly drawn pattern given the player's and the
 * global {@link PatternList}s, mirroring the original HexJS semantics:
 * number-literal bases ({@code aqaa}/{@code dedd}) are gated specially so blocking
 * one kills all literals; Bookkeeper's Gambits honour their own length cap and are
 * never redirected; anything else is whitelisted/blocked and then redirected.
 *
 * <p>Lives OUTSIDE the {@code .mixin} package on purpose: every class in a declared
 * mixin package is treated as a mixin by the transformer and cannot be referenced
 * from regular code — the {@code SpellPatternInterceptor} calls back into this, so
 * it must stay in a normal package.
 *
 * <p>Standalone so the exact policy is unit-testable without a running server.
 */
public final class PatternGate {
    public enum Kind { ALLOW, BLOCKED, REDIRECT }

    public record Verdict(Kind kind, String reason, HexPattern redirected) {
        public static Verdict allow() {
            return new Verdict(Kind.ALLOW, null, null);
        }

        public static Verdict blocked(String reason) {
            return new Verdict(Kind.BLOCKED, reason, null);
        }

        public static Verdict redirect(HexPattern redirected) {
            return new Verdict(Kind.REDIRECT, null, redirected);
        }
    }

    private static final String BANNED_MESSAGE = "A strange force is prohibiting me from forming this pattern";
    private static final String BOOKKEEPER_MESSAGE =
            "A strange force is prohibiting me from forming a Bookkeeper's Gambit this long";

    private PatternGate() {
    }

    public static Verdict decide(HexPattern pattern, PatternList player, PatternList global) {
        String signature = pattern.anglesSignature();

        // The two bases of number literals: blocking the base kills every literal.
        if (signature.startsWith("aqaa")) {
            if (player.blocks("aqaa") || (!player.contains("aqaa") && global.blocks("aqaa"))) {
                return Verdict.blocked(BANNED_MESSAGE);
            }
            return Verdict.allow();
        }
        if (signature.startsWith("dedd")) {
            if (player.blocks("dedd") || (!player.contains("dedd") && global.blocks("dedd"))) {
                return Verdict.blocked(BANNED_MESSAGE);
            }
            return Verdict.allow();
        }

        Integer bookkeeperLength = bookkeeperLength(pattern);
        if (bookkeeperLength != null) {
            int playerCap = player.maxBookkeepersLength();
            int globalCap = global.maxBookkeepersLength();
            boolean tooLong = (playerCap != -1 && playerCap < bookkeeperLength)
                    || (playerCap == -1 && globalCap != -1 && globalCap < bookkeeperLength);
            if (tooLong) {
                return Verdict.blocked(BOOKKEEPER_MESSAGE);
            }
            // Bookkeeper's Gambits are never redirected, exactly as in the original.
            return Verdict.allow();
        }

        if (player.blocks(pattern) || (!player.contains(pattern) && global.blocks(pattern))) {
            return Verdict.blocked(BANNED_MESSAGE);
        }
        HexPattern playerRedirect = player.handleRedirect(pattern);
        if (playerRedirect != null) {
            return Verdict.redirect(playerRedirect);
        }
        HexPattern globalRedirect = global.handleRedirect(pattern);
        if (globalRedirect != null) {
            return Verdict.redirect(globalRedirect);
        }
        return Verdict.allow();
    }

    /** The number of straight strokes if {@code pattern} is a Bookkeeper's Gambit, else {@code null}. */
    private static Integer bookkeeperLength(HexPattern pattern) {
        ArrayList<HexDir> directions = new ArrayList<>(pattern.directions());
        HexDir flatDir = pattern.getStartDir();
        if (!pattern.getAngles().isEmpty() && pattern.getAngles().get(0) == HexAngle.LEFT_BACK) {
            flatDir = directions.get(0).rotatedBy(HexAngle.LEFT);
        }
        int length = 0;
        for (int i = 0; i < directions.size(); i++) {
            HexAngle angle = directions.get(i).angleFrom(flatDir);
            if (angle == HexAngle.FORWARD) {
                length++;
                continue;
            }
            if (i >= directions.size() - 1) {
                return null;
            }
            HexAngle next = directions.get(i + 1).angleFrom(flatDir);
            if (angle == HexAngle.RIGHT && next == HexAngle.LEFT) {
                length++;
                i++;
                continue;
            }
            return null;
        }
        return length;
    }
}
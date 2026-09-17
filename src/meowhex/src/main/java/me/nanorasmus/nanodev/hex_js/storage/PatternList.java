package me.nanorasmus.nanodev.hex_js.storage;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import me.nanorasmus.nanodev.hex_js.helpers.IotaHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One player's (or the global) pattern gatekeeping config: whitelist/blacklist of
 * angle signatures, redirects from one signature to another, and the Bookkeeper's
 * Gambit length cap ({@code -1} = unlimited).
 *
 * <p>All reads are safe to perform during the mixin's server-thread pattern handling;
 * mutations are authored solely through {@code HexJsData}, which marks the world
 * dirty so the config persists.
 */
public final class PatternList {
    private boolean isWhitelist;
    private final ArrayList<String> angleSignatures = new ArrayList<>();
    private final LinkedHashMap<String, String> redirects = new LinkedHashMap<>();
    private int maxBookkeepersLength = -1;

    public PatternList() {
    }

    public PatternList(boolean isWhitelist) {
        this.isWhitelist = isWhitelist;
    }

    public boolean isWhitelist() {
        return isWhitelist;
    }

    public void setWhitelist(boolean whitelist) {
        this.isWhitelist = whitelist;
    }

    public List<String> signatures() {
        return Collections.unmodifiableList(angleSignatures);
    }

    public Map<String, String> redirects() {
        return Collections.unmodifiableMap(redirects);
    }

    public int maxBookkeepersLength() {
        return maxBookkeepersLength;
    }

    public void setMaxBookkeepersLength(int length) {
        this.maxBookkeepersLength = length;
    }

    public void clearMaxBookkeepersLength() {
        this.maxBookkeepersLength = -1;
    }

    // ---------------------------------------------------------------------------
    // List membership
    // ---------------------------------------------------------------------------

    public boolean contains(String signature) {
        return angleSignatures.contains(signature);
    }

    public boolean contains(HexPattern pattern) {
        return contains(pattern.anglesSignature());
    }

    /**
     * Whether a signature is barred. A whitelist starts blocked and unblocks listed
     * signatures; a blacklist starts open and blocks listed ones.
     */
    public boolean blocks(String signature) {
        boolean blocked = isWhitelist;
        if (angleSignatures.contains(signature)) {
            blocked = !isWhitelist;
        }
        return blocked;
    }

    public boolean blocks(HexPattern pattern) {
        return blocks(pattern.anglesSignature());
    }

    public void addPattern(String signature) {
        if (!angleSignatures.contains(signature)) {
            angleSignatures.add(signature);
        }
    }

    public void addPatterns(List<String> signatures) {
        for (String signature : signatures) {
            addPattern(signature);
        }
    }

    public void removePattern(String signature) {
        angleSignatures.remove(signature);
    }

    public void clearPatterns() {
        angleSignatures.clear();
    }

    // ---------------------------------------------------------------------------
    // Redirects
    // ---------------------------------------------------------------------------

    /** The pattern a cast of {@code pattern} should be rewritten into, or {@code null}. */
    public HexPattern handleRedirect(HexPattern pattern) {
        String target = redirects.get(pattern.anglesSignature());
        if (target == null) {
            return null;
        }
        return new HexPattern(pattern.getStartDir(), IotaHelper.anglesFromString(target));
    }

    public void addRedirect(String from, String to) {
        redirects.put(from, to);
    }

    public void setRedirects(Map<String, String> newRedirects) {
        redirects.clear();
        redirects.putAll(newRedirects);
    }

    public void clearRedirects() {
        redirects.clear();
    }

    // ---------------------------------------------------------------------------
    // Copy / round-trip
    // ---------------------------------------------------------------------------

    public PatternList copy() {
        PatternList copy = new PatternList(isWhitelist);
        copy.angleSignatures.addAll(angleSignatures);
        copy.redirects.putAll(redirects);
        copy.maxBookkeepersLength = maxBookkeepersLength;
        return copy;
    }

    /** Overwrites this list's contents with {@code other}'s. Used when restoring global state. */
    public void copyFrom(PatternList other) {
        this.isWhitelist = other.isWhitelist;
        this.angleSignatures.clear();
        this.angleSignatures.addAll(other.angleSignatures);
        this.redirects.clear();
        this.redirects.putAll(other.redirects);
        this.maxBookkeepersLength = other.maxBookkeepersLength;
    }
}
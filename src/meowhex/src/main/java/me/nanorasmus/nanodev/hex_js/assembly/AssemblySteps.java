package me.nanorasmus.nanodev.hex_js.assembly;

import java.util.List;

/**
 * The five step ids the assembly runes write into a workpiece, in the order they
 * are listed on the scrolls.
 *
 * <p>These strings are the contract between the runes, the recipe registry and
 * KubeJS scripts: a recipe's {@code steps} list is a sequence of them. Renaming
 * one here silently invalidates every KubeJS recipe that used the old name, so
 * they are append-only.
 */
public final class AssemblySteps {
    /** Слияние Сущности — Merge Entities. */
    public static final String MERGE = "merge";
    /** Поглощение Даров — Absorb Gifts. */
    public static final String ABSORB = "absorb";
    /** Пощищение Сути — Purify Essence. */
    public static final String PURIFY = "purify";
    /** Вбирание Жертвы — Draw Sacrifice. */
    public static final String SACRIFICE = "sacrifice";
    /** Вливание Эфира — Infuse Aether; the only step that costs the caster mana. */
    public static final String MANA = "mana";

    public static final List<String> ALL = List.of(MERGE, ABSORB, PURIFY, SACRIFICE, MANA);

    private AssemblySteps() {
    }

    public static boolean isKnown(String step) {
        return ALL.contains(step);
    }
}

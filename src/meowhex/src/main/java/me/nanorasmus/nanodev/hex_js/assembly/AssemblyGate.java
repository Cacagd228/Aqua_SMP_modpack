package me.nanorasmus.nanodev.hex_js.assembly;

import me.nanorasmus.nanodev.hex_js.HexJsCommonConfig;

import java.util.Set;

/**
 * The single switch that turns the whole sequenced-assembly mechanic on or off,
 * plus the id lists the call sites need to recognise "this is an assembly
 * thing".
 *
 * <p><b>Why a flag and not unregistering the runes.</b> The obvious way to
 * disable a rune is to stop registering it. That crashes the book:
 * {@code LookupPatternComponent.getPatterns} resolves a page's {@code op_id}
 * through {@code getHolderOrThrow}, so a page whose action is missing throws
 * rather than rendering blank. The five Patchouli pages, the five advancement
 * icons and the five scroll items all reference these ids, so unregistering
 * leaves a dozen dangling references across three data formats. The flag
 * therefore gates <em>behaviour</em> — the runes stay in the registry, stay
 * drawable in the book, and simply refuse to do anything.
 *
 * <p><b>What {@code false} actually turns off.</b> The runes mishap, the JEI
 * page is never registered, and the five scrolls are removed from the chest
 * loot pool. Recipes still load (the serializer stays registered), so a
 * datapack that still ships {@code meowhex:assembly} JSON does not error on
 * reload — they are simply unreachable.
 *
 * <p>The default is {@code true}: this is unfinished and under test, not
 * abandoned. Flip {@code assembly.enabled} in
 * {@code config/meowhex-assembly.toml} and restart; no rebuild needed.
 */
public final class AssemblyGate {
    private AssemblyGate() {
    }

    /** The five scroll item ids ({@code scroll_*} suffixes of the op names). */
    public static final Set<String> SCROLL_IDS = Set.of(
            "scroll_merge_entities_loki",
            "scroll_absorb_gifts_loki",
            "scroll_purify_essence_loki",
            "scroll_draw_sacrifice_loki",
            "scroll_infuse_aether_loki");

    /**
     * Whether the assembly mechanic is live.
     *
     * <p>Defaults to {@code true} if the config cannot be read yet. Callers
     * include JEI's category registration, which can run before NeoForge has
     * finished loading configs, and a spec that throws there would take the
     * whole JEI plugin down. Erring towards "on" matches the config's own
     * default and fails safe: the worst case is the page appearing once on a
     * cold start, not a crash.
     */
    public static boolean enabled() {
        try {
            return HexJsCommonConfig.ASSEMBLY_ENABLED.get();
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * Whether a scroll id belongs to the assembly mechanic and should therefore
     * be kept out of the loot pool while the feature is off.
     */
    public static boolean isAssemblyScroll(String scrollId) {
        return SCROLL_IDS.contains(scrollId);
    }
}

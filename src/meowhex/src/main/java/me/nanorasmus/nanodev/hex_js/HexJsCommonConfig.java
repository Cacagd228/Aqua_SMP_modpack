package me.nanorasmus.nanodev.hex_js;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common (both sides) settings for the meowhex addon. Registered as
 * {@code meowhex-assembly.toml}.
 *
 * <p>Separate from {@code HexJsClientConfig} (rendering only) and from the
 * bundled Hexcasting fork's own {@code meowhex-common/server.toml}, which the
 * addon does not own.
 */
public final class HexJsCommonConfig {
    private HexJsCommonConfig() {
    }

    public static final ModConfigSpec SPEC;

    /**
     * Master switch for the sequenced-assembly mechanic (the four activator
     * runes plus the aether infusion rune).
     *
     * <p>{@code false} makes every one of the five runes refuse to cast, stops
     * the JEI page from being registered, and drops the five assembly scrolls
     * from the chest loot pool. It deliberately does <em>not</em> unregister
     * the runes: {@code LookupPatternComponent} resolves a book page's
     * {@code op_id} with {@code getHolderOrThrow}, so an unregistered rune
     * turns an unread page into a hard crash rather than a graceful absence.
     * The recipe serializer likewise stays registered so the
     * {@code meowhex:assembly} datapack JSON keeps parsing instead of
     * erroring on every reload.
     */
    public static final ModConfigSpec.BooleanValue ASSEMBLY_ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Sequenced assembly (Create's Sequenced Assembly rebuilt on hexcasting depots)").push("assembly");
        ASSEMBLY_ENABLED = builder
                .comment("Master switch for the whole assembly mechanic.",
                        "false = the five assembly runes refuse to cast, the JEI page is not",
                        "registered, and the five assembly scrolls stop dropping from chests.",
                        "The runes stay registered either way: the Patchouli book looks their",
                        "op_id up with getHolderOrThrow and would crash on a missing one.")
                .define("enabled", true);
        builder.pop();
        SPEC = builder.build();
    }
}

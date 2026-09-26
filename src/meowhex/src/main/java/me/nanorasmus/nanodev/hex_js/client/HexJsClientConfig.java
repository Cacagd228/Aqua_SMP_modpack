package me.nanorasmus.nanodev.hex_js.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-only settings. Registered as {@code meowhex-glyphs-client.toml}
 * (the default {@code meowhex-client.toml} is already taken by the bundled
 * Hexcasting fork in the same mod container).
 */
public final class HexJsClientConfig {
    private HexJsClientConfig() {
    }

    public static final ModConfigSpec SPEC;
    /**
     * Static (non-italic, non-obfuscated) pattern glyphs in text are drawn as a single
     * colored line strip (1 GPU submission per glyph) instead of the full two-pass
     * triangulated treatment with node dots (~7 submissions per glyph). Same layout
     * advance, much cheaper in rune-dense chat. Animated glyphs are unaffected.
     */
    public static final ModConfigSpec.BooleanValue SIMPLE_STATIC_GLYPHS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Pattern glyph (text rune) rendering").push("patternGlyphs");
        SIMPLE_STATIC_GLYPHS = builder
            .comment("Draw static pattern glyphs as a single line strip. "
                + "Enable when rune-dense chat tanks the framerate.")
            .define("simpleStaticGlyphs", true);
        builder.pop();
        SPEC = builder.build();
    }
}

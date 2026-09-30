package dev.aerofix;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Клиентский конфиг (config/aerofix-client.toml). */
public final class AeroFixClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue HOT_AIR_RENDERING;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("rendering");
        HOT_AIR_RENDERING = b
                .comment("Рендерить эффект горячего воздуха (дымка поверх блоков Envelope,",
                        "тепловой оверлей через Veil FBO). Это самая тяжёлая часть рендера",
                        "Create Aeronautics. Выключается без перезагрузки мира; на физику",
                        "и на contraptions не влияет вообще.")
                .define("hotAirRendering", true);
        b.pop();

        SPEC = b.build();
    }

    private AeroFixClientConfig() {}

    /** Миксин рендера может сработать раньше, чем конфиг загрузится. */
    public static boolean hotAirRendering() {
        try {
            return HOT_AIR_RENDERING.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }
}

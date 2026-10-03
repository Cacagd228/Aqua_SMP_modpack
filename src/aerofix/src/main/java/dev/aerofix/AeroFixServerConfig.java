package dev.aerofix;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Серверный конфиг (config/aerofix-server.toml).
 *
 * <p>Серверные значения читаются из mixin'ов на серверной стороне, поэтому их нельзя
 * держать в CLIENT-конфиге — на выделенном сервере он вообще не загружается.
 */
public final class AeroFixServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue FIX_STEERING_WHEEL;
    public static final ModConfigSpec.IntValue STEERING_MAX_RANGE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("fixes");
        FIX_STEERING_WHEEL = b
                .comment("Фикс руля (Steering Wheel): отбрасывать пакеты с NaN/Inf-углом и",
                        "от игроков дальше steeringMaxRange блоков, плюс санация состояния",
                        "блока от NaN и кламп предела угла.")
                .define("fixSteeringWheel", true);
        STEERING_MAX_RANGE = b
                .comment("Максимальная дистанция, с которой игрок может управлять рулём, блоков.",
                        "Защита от прокрутки руля на другом конце карты.")
                .defineInRange("steeringMaxRange", 64, 8, 512);
        b.pop();

        SPEC = b.build();
    }

    private AeroFixServerConfig() {}

    // Доступ через хелперы: миксины могут сработать раньше, чем конфиг загрузится.

    public static boolean fixSteeringWheel() {
        try {
            return FIX_STEERING_WHEEL.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    public static double steeringMaxRange() {
        try {
            return STEERING_MAX_RANGE.get();
        } catch (IllegalStateException e) {
            return 64.0;
        }
    }
}

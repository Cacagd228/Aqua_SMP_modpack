package dev.hexsable;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Серверный конфиг (config/hexsable-server.toml). */
public final class HexSableConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue IMPULSE_COST_FACTOR;
    public static final ModConfigSpec.DoubleValue SPIN_COST_FACTOR;
    public static final ModConfigSpec.DoubleValue BLINK_COST_FACTOR;
    public static final ModConfigSpec.DoubleValue MAX_IMPULSE_SPEED;
    public static final ModConfigSpec.DoubleValue MAX_SPIN;
    public static final ModConfigSpec.DoubleValue MAX_BLINK_DISTANCE;
    public static final ModConfigSpec.BooleanValue BLOCK_SPELLS_ON_STRUCTURES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("costs");
        IMPULSE_COST_FACTOR = b
                .comment("Множитель стоимости Structure Impulse (базово: пыль * масса * dv^2, dv в блоках/тик).")
                .defineInRange("impulseCostFactor", 1.0, 0.0, 1000.0);
        SPIN_COST_FACTOR = b
                .comment("Множитель стоимости Structure Torque (базово: пыль * dw^T * I * dw).")
                .defineInRange("spinCostFactor", 1.0, 0.0, 1000.0);
        BLINK_COST_FACTOR = b
                .comment("Множитель стоимости Structure Blink (базово: 2.5 пыли * блоки * sqrt(масса)).")
                .defineInRange("blinkCostFactor", 1.0, 0.0, 1000.0);
        b.pop();

        b.push("limits");
        MAX_IMPULSE_SPEED = b
                .comment("Максимальное изменение скорости за одно заклинание, блоков/тик (1 = 20 м/с).")
                .defineInRange("maxImpulseSpeed", 2.0, 0.0, 100.0);
        MAX_SPIN = b
                .comment("Максимальное изменение угловой скорости за одно заклинание, рад/тик.")
                .defineInRange("maxSpin", 0.5, 0.0, 10.0);
        MAX_BLINK_DISTANCE = b
                .comment("Максимальная дальность Structure Blink, блоков.")
                .defineInRange("maxBlinkDistance", 32.0, 0.0, 1024.0);
        b.pop();

        b.push("compat");
        BLOCK_SPELLS_ON_STRUCTURES = b
                .comment("Разрешить обычным заклинаниям Hex (Break Block, Place Block, ...) работать по блокам",
                        "внутри физических структур: проверка дальности использует проекцию плот-координат в мир.")
                .define("blockSpellsOnStructures", true);
        b.pop();

        SPEC = b.build();
    }

    private HexSableConfig() {}
}

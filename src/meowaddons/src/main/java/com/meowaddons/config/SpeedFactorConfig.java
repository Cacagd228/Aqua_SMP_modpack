package com.meowaddons.config;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class SpeedFactorConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("speed_factor");
        builder.comment("GregTech-like tier speed factor configuration");
        builder.comment("Formula: speedFactor = base ^ (machineTier - recipeTier)");
        builder.comment("When machineTier <= recipeTier, factor = 1 (no speedup)");

        CommonConfig config = new CommonConfig(builder);
        COMMON = config;
        COMMON_SPEC = builder.build();
    }

    public static class CommonConfig {
        public final ModConfigSpec.DoubleValue base;
        public final ModConfigSpec.IntValue minFactor;
        public final ModConfigSpec.BooleanValue enablePressing;
        public final ModConfigSpec.BooleanValue enableCrushing;
        public final ModConfigSpec.BooleanValue enableMilling;
        public final ModConfigSpec.BooleanValue enableMixing;
        public final ModConfigSpec.BooleanValue enableCutting;
        public final ModConfigSpec.BooleanValue enableDeploying;

        CommonConfig(ModConfigSpec.Builder builder) {
            base = builder
                .comment("Base of the exponential speed factor formula")
                .comment("Default: 4.0 (GregTech-like: each tier difference = 4x speed)")
                .defineInRange("base", 4.0, 1.0, 100.0);

            minFactor = builder
                .comment("Minimum speed factor (clamped)")
                .comment("Default: 1")
                .defineInRange("min_factor", 1, 1, 10000);

            enablePressing = builder
                .comment("Enable speed factor for Pressing (Mechanical Press)")
                .comment("Default: false (GregTech excludes pressing/compression from tier speedup)")
                .define("enable_pressing", false);

            enableCrushing = builder
                .comment("Enable speed factor for Crushing (Crushing Wheels)")
                .comment("Default: true")
                .define("enable_crushing", true);

            enableMilling = builder
                .comment("Enable speed factor for Milling (Millstones)")
                .comment("Default: true")
                .define("enable_milling", true);

            enableMixing = builder
                .comment("Enable speed factor for Mixing (Mechanical Mixers)")
                .comment("Default: true")
                .define("enable_mixing", true);

            enableCutting = builder
                .comment("Enable speed factor for Cutting (Mechanical Saws)")
                .comment("Default: true")
                .define("enable_cutting", true);

            enableDeploying = builder
                .comment("Enable speed factor for Deploying (Deployers)")
                .comment("Default: false (GregTech excludes deploying from tier speedup)")
                .define("enable_deploying", false);
        }
    }

    public static double getBase() {
        return COMMON.base.get();
    }

    public static int getMinFactor() {
        return COMMON.minFactor.get();
    }

    public static boolean isPressingEnabled() {
        return COMMON.enablePressing.get();
    }

    public static boolean isCrushingEnabled() {
        return COMMON.enableCrushing.get();
    }

    public static boolean isMillingEnabled() {
        return COMMON.enableMilling.get();
    }

    public static boolean isMixingEnabled() {
        return COMMON.enableMixing.get();
    }

    public static boolean isCuttingEnabled() {
        return COMMON.enableCutting.get();
    }

    public static boolean isDeployingEnabled() {
        return COMMON.enableDeploying.get();
    }
}
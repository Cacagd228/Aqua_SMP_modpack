package com.colonizer.colonycard.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Общие настройки мода (config/colonycard-common.toml). */
public final class ModCommonConfig {
    private ModCommonConfig() {
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> DISCORD_WEBHOOK_URL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("ColonyCard common settings").push("colonycard");
        DISCORD_WEBHOOK_URL = builder
                .comment("Discord webhook URL для уведомления о закрытии этапа. Пусто = отключено (только лог).")
                .define("discordWebhookUrl", "");
        builder.pop();
        SPEC = builder.build();
    }
}

package com.colonizer.colonycard.stage;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.config.ModCommonConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Отправка уведомлений в Discord через webhook.
 * Текст сообщения о закрытии этапа — заглушка, финальный текст будет позже.
 */
public final class DiscordWebhook {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /** Заглушка сообщения о закрытии этапа. */
    public static final String STAGE_COMPLETE_PLACEHOLDER = "Этап закрыт! (заглушка — текст позже)";

    private DiscordWebhook() {
    }

    public static void sendStageComplete(String summary) {
        String url = "";
        try {
            url = ModCommonConfig.DISCORD_WEBHOOK_URL.get();
        } catch (IllegalStateException e) {
            // Конфиг ещё не загружен — только лог.
            LOGGER.info("[{}] {}", ColonyCardMod.MODID, STAGE_COMPLETE_PLACEHOLDER);
            return;
        }
        if (url == null || url.isBlank()) {
            LOGGER.info("[{}] Webhook не настроен, Discord-уведомление пропущено: {}", ColonyCardMod.MODID, STAGE_COMPLETE_PLACEHOLDER);
            return;
        }
        String content = STAGE_COMPLETE_PLACEHOLDER + "\n" + summary;
        String json = "{\"content\":" + quote(content) + "}";
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url.trim()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("[{}] Некорректный Discord webhook URL: {}", ColonyCardMod.MODID, e.getMessage());
            return;
        }
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((resp, err) -> {
                    if (err != null) {
                        LOGGER.warn("[{}] Не удалось отправить Discord-уведомление: {}", ColonyCardMod.MODID, err.toString());
                    } else if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        LOGGER.warn("[{}] Discord webhook вернул статус {}", ColonyCardMod.MODID, resp.statusCode());
                    }
                });
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}

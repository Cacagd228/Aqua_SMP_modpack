package com.colonizer.colonycard.client;

import com.colonizer.colonycard.identity.FaceCover;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

/**
 * Скрытие личности в чате — постоянная механика, не зависит от отладки.
 *
 * <ul>
 *   <li>имя скрыто (балаклава, паспорта нет) → строка от «...»;</li>
 *   <li>закрытое лицо, но есть паспорт → в чате только имя из паспорта;</li>
 *   <li>открытое лицо → чат не трогаем.</li>
 * </ul>
 */
@EventBusSubscriber(modid = "colonycard", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class IdentityChatMaskHandler {
    private IdentityChatMaskHandler() {
    }

    private static final int MASKED_COLOR = 0xFF9A9A9A;
    private static final int PASSPORT_COLOR = 0xFFE8C06C;

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent.Player event) {
        if (event.getSender() == null) {
            return;
        }
        ClientNameplateCache.Entry entry = ClientNameplateCache.get(event.getSender());
        if (entry == null) {
            return;
        }
        String raw = event.getPlayerChatMessage().signedContent();
        switch (entry.mode()) {
            case FaceCover.MODE_HIDDEN -> event.setMessage(Component.literal("...")
                    .append(Component.literal(" " + raw))
                    .withStyle(s -> s.withColor(MASKED_COLOR)));
            case FaceCover.MODE_PASSPORT_ONLY -> event.setMessage(Component.literal(entry.passportName())
                    .append(Component.literal(": " + raw))
                    .withStyle(s -> s.withColor(PASSPORT_COLOR)));
            default -> {
                // DEFAULT / TRUE_PLUS — ванильный чат.
            }
        }
    }
}

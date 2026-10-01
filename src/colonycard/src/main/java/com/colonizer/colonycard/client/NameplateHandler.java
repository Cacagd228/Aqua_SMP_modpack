package com.colonizer.colonycard.client;

import com.colonizer.colonycard.identity.FaceCover;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

/**
 * Клиентский рендер Display-состояния (только nameplate над головой).
 *
 * <p>База для {@code TRUE_PLUS} — {@code event.getContent()}, а не GameProfile:
 * так сохраняются ванильное форматирование/цвета (команды и т.п.).
 */
@EventBusSubscriber(modid = "colonycard", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class NameplateHandler {
    private NameplateHandler() {
    }

    private static final int DEBUG_TRUE_NAME = 0xFFFF3B30;
    private static final int DEBUG_DOTS = 0xFFFF3B30;

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ClientNameplateCache.Entry entry = ClientNameplateCache.get(player.getUUID());
        boolean debug = ClientDebugIdentity.enabled();

        if (entry == null) {
            // Ванильное имя. В отладке показываем истинный ник явно.
            if (debug) {
                event.setContent(debugName(player));
            }
            return;
        }

        switch (entry.mode()) {
            case FaceCover.MODE_HIDDEN -> {
                if (debug) {
                    // Дебаг скрытия: три красные точки, истинный ник не показываем.
                    event.setContent(Component.literal("...")
                            .withStyle(s -> s.withColor(DEBUG_DOTS)));
                } else {
                    event.setCanRender(TriState.FALSE);
                }
            }
            case FaceCover.MODE_PASSPORT_ONLY -> {
                if (debug) {
                    event.setContent(Component.literal(entry.passportName())
                            .append(debugName(player)));
                } else {
                    event.setContent(Component.literal(entry.passportName()));
                }
            }
            case FaceCover.MODE_TRUE_PLUS -> {
                if (debug) {
                    event.setContent(event.getContent().copy()
                            .append(debugName(player)));
                } else {
                    event.setContent(event.getContent().copy()
                            .append(Component.literal(" \u00B7 " + entry.passportName())));
                }
            }
            default -> {
                // MODE_DEFAULT в кэше не хранится; сюда не попадаем.
            }
        }
    }

    /** Истинный ник красным — только в отладочном режиме. */
    private static Component debugName(Player player) {
        return Component.literal(" ")
                .append(Component.literal(player.getGameProfile().getName())
                        .withStyle(s -> s.withColor(DEBUG_TRUE_NAME).withBold(true)));
    }
}

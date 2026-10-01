package com.colonizer.colonycard.client;

import com.colonizer.colonycard.identity.FaceCover;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Клиентский кэш Display-состояний чужих nameplate.
 *
 * <p>Только чтение для рендера; решение принимает сервер.
 * {@code DEFAULT} в кэше не хранится (удаляем запись = стандартный nameplate).
 * Чистка: полный сброс при выходе + троттled-проход, выкидывающий UUID,
 * которых больше нет среди загруженных игроков (карта не растет за SMP-сессию).
 */
public final class ClientNameplateCache {
    private ClientNameplateCache() {
    }

    public record Entry(byte mode, String passportName) {
    }

    private static final Map<UUID, Entry> STATES = new ConcurrentHashMap<>();

    public static void apply(UUID target, byte mode, String passportName) {
        if (target == null) {
            return;
        }
        if (mode == FaceCover.MODE_DEFAULT) {
            STATES.remove(target);
            return;
        }
        STATES.put(target, new Entry(mode, passportName == null ? "" : passportName));
    }

    public static Entry get(UUID target) {
        if (target == null) {
            return null;
        }
        return STATES.get(target);
    }

    public static void clear() {
        STATES.clear();
    }

    @EventBusSubscriber(modid = "colonycard", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static final class Hooks {
        private Hooks() {
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            clear();
        }

        /** Раз в ~10 секунд выкидываем игроков, которых нет в текущем мире. */
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                if (!STATES.isEmpty()) {
                    clear();
                }
                return;
            }
            if ((mc.player.tickCount % 200) != 0 || STATES.isEmpty()) {
                return;
            }
            Set<UUID> present = new HashSet<>();
            present.add(mc.player.getUUID());
            for (var p : mc.level.players()) {
                present.add(p.getUUID());
            }
            STATES.keySet().removeIf(uuid -> !present.contains(uuid));
        }
    }
}

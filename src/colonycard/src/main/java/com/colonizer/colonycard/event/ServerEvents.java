package com.colonizer.colonycard.event;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.data.ColonistData;
import com.colonizer.colonycard.data.ColonistPools;
import com.colonizer.colonycard.data.ModAttachments;
import com.colonizer.colonycard.identity.FaceCover;
import com.colonizer.colonycard.network.SyncColonistDataPacket;
import com.colonizer.colonycard.network.SyncDebugIdentityPacket;
import com.colonizer.colonycard.network.SyncNameplatePacket;
import com.colonizer.colonycard.trade.TradeTerminalManager;
import com.colonizer.colonycard.stage.CrownStageManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side lifecycle: creates a colonist profile on first join and keeps the owning client synced. */
@EventBusSubscriber(modid = ColonyCardMod.MODID)
public final class ServerEvents {
    private ServerEvents() {
    }

    /** Последний разосланный Display по игрокам: шлем только при изменении (троттлинг). */
    private static final Map<UUID, String> LAST_DISPLAY = new ConcurrentHashMap<>();


    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ColonistData data = player.getData(ModAttachments.COLONIST_DATA);
        if (!data.initialized()) {
            RandomSource random = RandomSource.create();
            data = new ColonistData(
                    player.getGameProfile().getName(),
                    System.currentTimeMillis(),
                    ColonistPools.randomGoal(random),
                    ColonistPools.randomTraits(random),
                    0,
                    0,
                    0,
                    ColonistPools.randomArchipelago(random)
            );
            player.setData(ModAttachments.COLONIST_DATA, data);
        } else if (data.archipelago().isEmpty()) {
            // Миграция старых досье (до паспортного разворота): добрасываем архипелаг.
            data = data.withArchipelago(ColonistPools.randomArchipelago(RandomSource.create()));
            player.setData(ModAttachments.COLONIST_DATA, data);
        }
        if (data.initialized() && data.traits().size() > ColonistPools.TRAITS_PER_COLONIST) {
            // Примет теперь две: подрезаем старые тройки до первых двух.
            data = data.withTraits(List.copyOf(data.traits().subList(0, ColonistPools.TRAITS_PER_COLONIST)));
            player.setData(ModAttachments.COLONIST_DATA, data);
        }

        sync(player);
        CrownStageManager.syncTo(player, false);
        refreshDisplay(player);
        TradeTerminalManager.syncTo(player);
        // Отладочный режим личности: клиенту сразу высылаем его личное состояние.
        PacketDistributor.sendToPlayer(player,
                new SyncDebugIdentityPacket(DEBUG_IDENTITY.contains(player.getUUID())));
    }

    /** Отладочный режим личности: включён ли конкретный игрок (оп-команда {@code /colonycard debug_identity}). */
    private static final Set<UUID> DEBUG_IDENTITY = ConcurrentHashMap.newKeySet();

    public static boolean isDebugIdentity(UUID player) {
        return DEBUG_IDENTITY.contains(player);
    }

    /** Включить/выключить отладку одному игроку и отдать ему новое состояние. */
    public static void setDebugIdentity(ServerPlayer target, boolean enabled) {
        if (enabled) {
            DEBUG_IDENTITY.add(target.getUUID());
        } else {
            DEBUG_IDENTITY.remove(target.getUUID());
        }
        PacketDistributor.sendToPlayer(target, new SyncDebugIdentityPacket(enabled));
    }

    public static void sync(ServerPlayer player) {
        ColonistData data = player.getData(ModAttachments.COLONIST_DATA);
        PacketDistributor.sendToPlayer(player, new SyncColonistDataPacket(player.getGameProfile().getName(), data));
    }

    /** Пересчитать Display цели и разослать трекающим + себе, но только при изменении. */
    public static void refreshDisplay(ServerPlayer target) {
        FaceCover.Resolved resolved = FaceCover.resolve(target);
        String key = resolved.key();
        if (key.equals(LAST_DISPLAY.get(target.getUUID()))) {
            return;
        }
        LAST_DISPLAY.put(target.getUUID(), key);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                target, new SyncNameplatePacket(target.getUUID(), resolved));
    }

    /** Новый наблюдатель: точечно шлем текущий Display цели только ему. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer viewer)) {
            return;
        }
        FaceCover.Resolved resolved = FaceCover.resolve(target);
        LAST_DISPLAY.putIfAbsent(target.getUUID(), resolved.key());
        PacketDistributor.sendToPlayer(viewer, new SyncNameplatePacket(target.getUUID(), resolved));
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_DISPLAY.remove(player.getUUID());
            DEBUG_IDENTITY.remove(player.getUUID());
        }
    }

    /** Троттлинг: проверка раз в 20 тиков на игрока, отправка только при изменении. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if ((player.tickCount % 20) != 0) {
            return;
        }
        refreshDisplay(player);
    }
}

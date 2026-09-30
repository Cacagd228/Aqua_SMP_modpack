package com.fmm.teams.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fmm.teams.team.Team;
import com.fmm.teams.team.TeamManager;
import com.fmm.worldgen.IslandHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Shows the owning party in the action bar when a player walks onto a claimed island.
 * Purely cosmetic — no block protection. The message is re-sent only when the zone changes
 * so it does not spam every tick, and fades naturally after a few seconds.
 */
public final class TerritoryTracker {
    private TerritoryTracker() {}

    /** player -> zone id the action bar currently reflects. */
    private static final Map<UUID, Long> LAST_ZONE = new HashMap<>();
    /** player -> tick of the next poll; zone lookups are noise-based and far too costly per tick. */
    private static final Map<UUID, Integer> NEXT_POLL = new HashMap<>();
    private static final int POLL_INTERVAL = 10;

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;

        int now = player.tickCount;
        Integer next = NEXT_POLL.get(player.getUUID());
        if (next != null && now < next) return;
        NEXT_POLL.put(player.getUUID(), now + POLL_INTERVAL);

        long zoneId = IslandHelper.getZoneInfo(player.getX(), player.getZ()).island().zoneId();

        Long last = LAST_ZONE.get(player.getUUID());
        if (last != null && last == zoneId) return;   // already shown for this zone
        LAST_ZONE.put(player.getUUID(), zoneId);

        TeamManager mgr = TeamManager.get(player.server);
        UUID ownerId = mgr.islandOwner(zoneId);
        if (ownerId == null) {
            // Unclaimed island: still worth saying, so players know it is free to take.
            player.displayClientMessage(Component.translatable("msg.fmm_teams.territory_unclaimed"), true);
            return;
        }

        Team owner = mgr.byId(ownerId);
        if (owner == null) return;

        player.displayClientMessage(
                Component.translatable("msg.fmm_teams.territory", owner.name()), true);
    }

    /** Drops cached state so a relogging player sees the message again. */
    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        LAST_ZONE.remove(e.getEntity().getUUID());
        NEXT_POLL.remove(e.getEntity().getUUID());
    }
}

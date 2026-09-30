package com.fmm.teams.server;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fmm.teams.net.TeamNet;
import com.fmm.teams.team.Role;
import com.fmm.teams.team.Team;
import com.fmm.teams.team.TeamManager;
import com.fmm.worldgen.IslandData;
import com.fmm.worldgen.IslandHelper;
import com.fmm.worldgen.IslandTier;
import com.mojang.authlib.GameProfile;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side action handling + snapshot sync. */
public final class TeamServer {
    private TeamServer() {}

    public static void handleAction(ServerPlayer player, TeamNet.ServerboundTeamAction action) {
        MinecraftServer server = player.server;
        TeamManager mgr = TeamManager.get(server);
        String act = action.action();
        String err;
        switch (act) {
            case "sync" -> {
                pushTo(player, mgr);
                return;
            }
            case "create" -> err = mgr.create(action.text(), player.getUUID(), player.getGameProfile().getName());
            case "leave" -> err = mgr.leave(player.getUUID());
            case "disband" -> err = mgr.disband(player.getUUID());
            case "invite" -> {
                Target t = resolve(server, action.text());
                if (t == null) {
                    err = "msg.fmm_teams.err.no_player";
                    break;
                }
                err = mgr.invite(player.getUUID(), t.uuid(), t.name());
                if (err == null) {
                    ServerPlayer online = server.getPlayerList().getPlayer(t.uuid());
                    if (online != null) pushTo(online, mgr);
                }
            }
            case "accept" -> {
                UUID teamId = action.idAsUuid();
                // allow accept by index-less single invite: if id missing and exactly 1 invite, use it
                if (teamId == null) {
                    List<Team> inv = mgr.pendingInvites(player.getUUID());
                    teamId = inv.size() == 1 ? inv.get(0).id() : null;
                }
                err = teamId == null ? "msg.fmm_teams.err.not_invited"
                        : mgr.accept(player.getUUID(), player.getGameProfile().getName(), teamId);
            }
            case "decline" -> {
                UUID teamId = action.idAsUuid();
                if (teamId == null) {
                    List<Team> inv = mgr.pendingInvites(player.getUUID());
                    teamId = inv.size() == 1 ? inv.get(0).id() : null;
                }
                err = teamId == null ? "msg.fmm_teams.err.not_invited" : null;
                if (err == null && teamId != null) mgr.decline(player.getUUID(), teamId);
            }
            case "kick", "promote", "demote", "transfer" -> {
                Target t = resolve(server, action.text());
                if (t == null) {
                    err = "msg.fmm_teams.err.no_player";
                    break;
                }
                err = switch (act) {
                    case "kick" -> mgr.kick(player.getUUID(), t.uuid());
                    case "promote" -> mgr.promote(player.getUUID(), t.uuid());
                    case "demote" -> mgr.demote(player.getUUID(), t.uuid());
                    default -> mgr.transfer(player.getUUID(), t.uuid());
                };
                if (err == null) {
                    ServerPlayer online = server.getPlayerList().getPlayer(t.uuid());
                    if (online != null) pushTo(online, mgr);
                }
            }
            case "claim" -> err = handleClaim(player, mgr);
            case "unclaim" -> err = handleUnclaim(player, mgr, action);
            default -> err = "msg.fmm_teams.err.unknown";
        }

        if (err != null) {
            player.displayClientMessage(Component.translatable(err), false);
        } else if (!act.equals("sync")) {
            // broadcast fresh snapshot to everyone affected
            Team team = mgr.teamOf(player.getUUID());
            // kicked player is no longer in team: also push to actor's team members
            if (team != null) {
                for (UUID u : mgr.affected(team)) {
                    ServerPlayer sp = server.getPlayerList().getPlayer(u);
                    if (sp != null) pushTo(sp, mgr);
                }
            } else {
                pushTo(player, mgr);
            }
        }
        // always refresh actor view
        pushTo(player, mgr);
    }

    /** Claims the island under the player's feet. Returns error key or null on success. */
    private static String handleClaim(ServerPlayer player, TeamManager mgr) {
        IslandData island = IslandHelper.getZoneInfo(player.getX(), player.getZ()).island();
        if (island.tier() == IslandTier.SPAWN) return "msg.fmm_teams.err.island_spawn";
        return mgr.claimIsland(player.getUUID(), island.zoneId(), island.tier().getCost(),
                island.tier().name(), island.centerX(), island.centerZ());
    }

    /** Releases an island. Target comes from the packet text (zoneId as string) or, if absent, the island underfoot. */
    private static String handleUnclaim(ServerPlayer player, TeamManager mgr, TeamNet.ServerboundTeamAction action) {
        long zoneId;
        String text = action.text();
        if (text == null || text.isBlank()) {
            zoneId = IslandHelper.getZoneInfo(player.getX(), player.getZ()).island().zoneId();
        } else {
            try {
                zoneId = Long.parseLong(text.trim());
            } catch (NumberFormatException e) {
                return "msg.fmm_teams.err.unknown";
            }
        }
        return mgr.unclaimIsland(player.getUUID(), zoneId);
    }

    public static void pushTo(ServerPlayer player, TeamManager mgr) {
        PacketDistributor.sendToPlayer(player, buildSync(player, mgr));
    }

    public static void pushAll(MinecraftServer server) {
        TeamManager mgr = TeamManager.get(server);
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) pushTo(sp, mgr);
    }

    static TeamNet.ClientboundTeamSync buildSync(ServerPlayer player, TeamManager mgr) {
        UUID uuid = player.getUUID();
        mgr.touchPlayer(uuid, player.getGameProfile().getName());
        Team team = mgr.teamOf(uuid);
        List<TeamNet.InviteEntry> invites = new ArrayList<>();
        for (Team t : mgr.pendingInvites(uuid)) {
            invites.add(new TeamNet.InviteEntry(t.id(), t.name(), t.nameOf(t.owner())));
        }
        if (team == null) {
            return new TeamNet.ClientboundTeamSync(false, null, "", "", null, "private", 0L,
                    List.of(), invites, 0, 0, List.of(), null);
        }
        List<TeamNet.MemberEntry> members = new ArrayList<>();
        for (UUID m : team.sortedMembers()) {
            boolean online = player.server.getPlayerList().getPlayer(m) != null;
            members.add(new TeamNet.MemberEntry(m, team.nameOf(m), team.roleOf(m), online));
        }
        Role yourRole = team.roleOf(uuid);

        // Islands: everything this team owns, plus the island under the player's feet
        // (so the GUI can show what a claim here would cost even if unclaimed).
        List<TeamNet.IslandEntry> islands = new ArrayList<>();
        for (var claim : mgr.allClaims()) {
            if (!claim.teamId().equals(team.id())) continue;
            islands.add(describeIsland(claim));
        }
        islands.sort((a, b) -> Long.compare(b.cost(), a.cost()));
        TeamNet.IslandEntry here = describeHere(player, mgr, team.id());

        return new TeamNet.ClientboundTeamSync(true, team.id(), team.name(),
                team.nameOf(team.owner()), team.owner(),
                yourRole == null ? "private" : yourRole.id(), team.createdAt(), members, invites,
                mgr.teamPoints(team.id()), mgr.teamClaimedCost(team.id()), islands, here);
    }

    /** Turns a stored claim into a client entry. Tier name is localised on the client. */
    private static TeamNet.IslandEntry describeIsland(TeamManager.IslandClaim claim) {
        return new TeamNet.IslandEntry(claim.zoneId(), claim.tierId(),
                claim.cost(), claim.centerX(), claim.centerZ(), true);
    }

    /** Island under the player's current position, or null if the lookup fails. */
    private static TeamNet.IslandEntry describeHere(ServerPlayer player, TeamManager mgr, UUID teamId) {
        IslandData data = IslandHelper.getZoneInfo(player.getX(), player.getZ()).island();
        if (data == null) return null;
        TeamManager.IslandClaim claim = mgr.getClaim(data.zoneId());
        boolean mine = claim != null && claim.teamId().equals(teamId);
        return new TeamNet.IslandEntry(data.zoneId(), data.tier().name(),
                data.tier().getCost(), data.centerX(), data.centerZ(), mine);
    }

    private record Target(UUID uuid, String name) {}

    /** Resolves by online player name first, then by member/invite name cache, then profile cache. */
    static Target resolve(MinecraftServer server, String text) {
        if (text == null || text.isBlank()) return null;
        String name = text.trim();
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) return new Target(online.getUUID(), online.getGameProfile().getName());
        TeamManager mgr = TeamManager.get(server);
        for (Team t : mgr.all()) {
            for (var e : t.namesView().entrySet()) {
                if (e.getValue().equalsIgnoreCase(name)) return new Target(e.getKey(), e.getValue());
            }
            for (var e : t.inviteNamesView().entrySet()) {
                if (e.getValue().equalsIgnoreCase(name)) return new Target(e.getKey(), e.getValue());
            }
        }
        var cache = server.getProfileCache();
        if (cache != null) {
            var opt = cache.get(name);
            if (opt.isPresent()) {
                GameProfile p = opt.get();
                return new Target(p.getId(), p.getName());
            }
        }
        return null;
    }
}

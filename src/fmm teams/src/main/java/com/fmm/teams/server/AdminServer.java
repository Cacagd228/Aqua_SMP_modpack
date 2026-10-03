package com.fmm.teams.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.fmm.teams.net.AdminNet;
import com.fmm.teams.team.Role;
import com.fmm.teams.team.Team;
import com.fmm.teams.team.TeamManager;
import com.fmm.worldgen.IslandData;
import com.fmm.worldgen.IslandHelper;
import com.fmm.worldgen.IslandTier;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Backs the operator panel. Every action is re-authorised here against permission level 2 — the
 * client panel is a view, never a trust boundary — and every action ends with a fresh snapshot for
 * the operator plus a re-sync for whichever players the mutation touched.
 */
public final class AdminServer {
    /** Same level {@code /fmm admin} uses, so panel and commands always agree. */
    public static final int PERM_LEVEL = 2;

    private AdminServer() {}

    public static boolean isAdmin(ServerPlayer player) {
        return player.hasPermissions(PERM_LEVEL);
    }

    // ---------- snapshot ----------

    /** Asks the server for a fresh panel snapshot. Non-operators get a refused, empty one. */
    public static void requestSnapshot(ServerPlayer player) {
        TeamManager mgr = TeamManager.get(player.server);
        boolean permitted = isAdmin(player);
        String message = permitted ? "" : Component.translatable("gui.fmm_teams.admin_no_perm").getString();
        PacketDistributor.sendToPlayer(player, build(player, mgr, permitted, false, message));
    }

    public static void push(ServerPlayer player, String message, boolean ok) {
        if (!isAdmin(player)) return;
        TeamManager mgr = TeamManager.get(player.server);
        PacketDistributor.sendToPlayer(player, build(player, mgr, true, ok, message));
    }

    private static AdminNet.ClientboundAdminData build(ServerPlayer player, TeamManager mgr,
                                                      boolean permitted, boolean ok, String message) {
        if (!permitted) {
            return new AdminNet.ClientboundAdminData(false, List.of(), List.of(), null, message, false);
        }
        List<AdminNet.AdminTeam> teams = new ArrayList<>();
        for (Team t : mgr.all()) {
            List<AdminNet.AdminMember> members = new ArrayList<>();
            for (UUID u : t.sortedMembers()) {
                boolean online = player.server.getPlayerList().getPlayer(u) != null;
                members.add(new AdminNet.AdminMember(u, t.nameOf(u), t.roleOf(u), online));
            }
            List<AdminNet.AdminIsland> islands = new ArrayList<>();
            for (var claim : mgr.allClaims()) {
                if (!claim.teamId().equals(t.id())) continue;
                islands.add(new AdminNet.AdminIsland(claim.zoneId(), claim.tierId(), claim.cost(),
                        claim.centerX(), claim.centerZ()));
            }
            islands.sort(Comparator.comparingDouble(AdminNet.AdminIsland::centerZ));
            teams.add(new AdminNet.AdminTeam(t.id(), t.name(), t.owner(), t.nameOf(t.owner()),
                    t.createdAt(), t.bonus(), mgr.teamPoints(t.id()), mgr.teamClaimedCost(t.id()),
                    members, islands));
        }
        teams.sort(Comparator.comparing(AdminNet.AdminTeam::name, String.CASE_INSENSITIVE_ORDER));

        List<AdminNet.AdminClaim> claims = new ArrayList<>();
        for (var claim : mgr.allClaims()) {
            Team owner = mgr.byId(claim.teamId());
            claims.add(new AdminNet.AdminClaim(claim.zoneId(), claim.tierId(), claim.cost(),
                    claim.centerX(), claim.centerZ(), owner == null ? "?" : owner.name()));
        }
        claims.sort(Comparator.comparing(AdminNet.AdminClaim::ownerName, String.CASE_INSENSITIVE_ORDER));

        return new AdminNet.ClientboundAdminData(true, teams, claims, describeHere(player),
                message == null ? "" : message, ok);
    }

    /** Island under the operator's feet, whatever its claim state. */
    private static AdminNet.AdminIsland describeHere(ServerPlayer player) {
        IslandData data = IslandHelper.getZoneInfo(player.getX(), player.getZ()).island();
        if (data == null) return null;
        return new AdminNet.AdminIsland(data.zoneId(), data.tier().name(), data.tier().getCost(),
                data.centerX(), data.centerZ());
    }

    // ---------- actions ----------

    public static void handleAction(ServerPlayer player, AdminNet.ServerboundAdminAction action) {
        TeamManager mgr = TeamManager.get(player.server);
        if (!isAdmin(player)) {
            // Stay silent: a refused panel must not even confirm that the action existed.
            requestSnapshot(player);
            return;
        }

        String act = action.action();
        if (act.equals("refresh")) {
            push(player, "", true);
            return;
        }

        String err = null;
        String okKey = null;
        Object[] okArgs = new Object[0];
        UUID syncTeam = null;
        UUID syncFormerOwner = null;

        switch (act) {
            case "points", "points_set" -> {
                Team t = team(mgr, action.arg1());
                if (t == null) {
                    err = "msg.fmm_teams.err.no_team";
                    break;
                }
                int n = parseInt(action.arg2());
                if (n == Integer.MIN_VALUE) {
                    err = "msg.fmm_teams.err.bad_number";
                    break;
                }
                err = act.equals("points") ? mgr.addBonus(t.id(), n) : mgr.setBonus(t.id(), n);
                if (err == null) {
                    syncTeam = t.id();
                    okKey = act.equals("points")
                            ? "msg.fmm_teams.admin_points_added"
                            : "msg.fmm_teams.admin_points_set";
                    okArgs = act.equals("points")
                            ? new Object[]{t.name(), n, mgr.teamPoints(t.id())}
                            : new Object[]{t.name(), mgr.teamPoints(t.id())};
                }
            }
            case "kick", "promote", "demote", "transfer", "add" -> {
                Team t = team(mgr, action.arg1());
                if (t == null) {
                    err = "msg.fmm_teams.err.no_team";
                    break;
                }
                TeamServer.Target target = TeamServer.resolve(player.server, action.arg2());
                if (target == null) {
                    err = "msg.fmm_teams.err.no_player";
                    break;
                }
                if (act.equals("add")) {
                    UUID former = mgr.teamOf(target.uuid()) == null
                            ? null : mgr.teamOf(target.uuid()).id();
                    err = mgr.adminAddMember(t.id(), target.uuid(), target.name());
                    if (err == null && former != null) syncFormerOwner = former;
                } else if (act.equals("kick")) {
                    err = mgr.adminKickHard(t.id(), target.uuid());
                } else if (act.equals("promote")) {
                    err = mgr.adminSetRole(t.id(), target.uuid(), Role.COMMANDER);
                } else if (act.equals("demote")) {
                    err = mgr.adminSetRole(t.id(), target.uuid(), Role.PRIVATE);
                } else {
                    err = mgr.adminTransfer(t.id(), target.uuid());
                }
                syncTeam = t.id();
                okKey = "msg.fmm_teams.admin_member_" + act;
                // Every admin_member_* key reads "%player% ... %party%", in that order.
                okArgs = new Object[]{target.name(), t.name()};
            }
            case "disband" -> {
                Team t = team(mgr, action.arg1());
                if (t == null) {
                    err = "msg.fmm_teams.err.no_team";
                    break;
                }
                err = mgr.adminDisband(t.id());
                if (err == null) {
                    syncTeam = t.id();
                    okKey = "msg.fmm_teams.admin_disbanded";
                    okArgs = new Object[]{t.name()};
                }
            }
            case "rename" -> {
                Team t = team(mgr, action.arg1());
                if (t == null) {
                    err = "msg.fmm_teams.err.no_team";
                    break;
                }
                String old = t.name();
                err = mgr.adminRename(t.id(), action.arg2());
                if (err == null) {
                    syncTeam = t.id();
                    okKey = "msg.fmm_teams.admin_renamed";
                    okArgs = new Object[]{old, t.name()};
                }
            }
            case "claim_here" -> {
                Team t = team(mgr, action.arg1());
                if (t == null) {
                    err = "msg.fmm_teams.err.no_team";
                    break;
                }
                IslandData island = IslandHelper.getZoneInfo(player.getX(), player.getZ()).island();
                if (island.tier() == IslandTier.SPAWN) {
                    err = "msg.fmm_teams.err.island_spawn";
                    break;
                }
                boolean free = action.arg2().equals("free");
                err = mgr.adminClaimIsland(t.id(), island.zoneId(), island.tier().getCost(),
                        island.tier().name(), island.centerX(), island.centerZ(), free);
                if (err != null) break;
                syncTeam = t.id();
                okKey = "msg.fmm_teams.admin_island_given";
                okArgs = new Object[]{t.name(), island.tier().getDisplayName(), island.tier().getCost()};
            }
            case "take" -> {
                long zoneId = parseLong(action.arg1());
                if (zoneId == Long.MIN_VALUE) {
                    err = "msg.fmm_teams.err.unknown";
                    break;
                }
                UUID ownerId = mgr.islandOwner(zoneId);
                if (ownerId == null) {
                    err = "msg.fmm_teams.err.island_not_claimed";
                    break;
                }
                err = mgr.adminUnclaimIsland(zoneId);
                if (err != null) break;
                syncTeam = ownerId;
                Team former = mgr.byId(ownerId);
                okKey = "msg.fmm_teams.admin_island_taken";
                okArgs = new Object[]{former == null ? "?" : former.name()};
            }
            default -> err = "msg.fmm_teams.err.unknown";
        }

        // Push the affected players' own panels before the operator's, so the operator always
        // renders the final state even if a party vanished mid-action.
        if (syncTeam != null) pushToTeam(player, syncTeam, mgr);
        if (syncFormerOwner != null) pushToTeam(player, syncFormerOwner, mgr);
        TeamServer.pushAll(player.server);

        String message = err != null
                ? Component.translatable(err).getString()
                : Component.translatable(okKey == null ? "msg.fmm_teams.admin_done" : okKey, okArgs).getString();
        push(player, message, err == null);
    }

    // ---------- helpers ----------

    private static Team team(TeamManager mgr, String name) {
        if (name == null || name.isBlank()) return null;
        return mgr.byName(name);
    }

    /** Returns Integer.MIN_VALUE as the "not a number" sentinel so 0 stays a legal value. */
    private static int parseInt(String raw) {
        if (raw == null) return Integer.MIN_VALUE;
        String s = raw.trim();
        if (s.isEmpty()) return Integer.MIN_VALUE;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    private static long parseLong(String raw) {
        if (raw == null) return Long.MIN_VALUE;
        String s = raw.trim();
        if (s.isEmpty()) return Long.MIN_VALUE;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return Long.MIN_VALUE;
        }
    }

    private static void pushToTeam(ServerPlayer actor, UUID teamId, TeamManager mgr) {
        Team t = mgr.byId(teamId);
        if (t == null) return;
        for (UUID u : mgr.affected(t)) {
            ServerPlayer sp = actor.server.getPlayerList().getPlayer(u);
            if (sp != null) TeamServer.pushTo(sp, mgr);
        }
    }
}

package com.fmm.teams.team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server authority for teams.
 * Rules: one player = one team; newcomers join as PRIVATE; invites required.
 */
public final class TeamManager extends SavedData {
    public static final String SAVE_KEY = "fmm_teams";
    public static final int MAX_NAME_LEN = 24;
    public static final int MAX_TEAM_SIZE = 20;

    private final Map<UUID, Team> teams = new LinkedHashMap<>();
    /** member -> teamId */
    private final Map<UUID, UUID> memberIndex = new LinkedHashMap<>();
    /** invited player -> teamIds (usually 0..n, we allow several pending invites) */
    private final Map<UUID, List<UUID>> inviteIndex = new LinkedHashMap<>();
    /** zoneId -> IslandClaim (claimed islands) */
    private final Map<Long, IslandClaim> islandClaims = new LinkedHashMap<>();

    public TeamManager() {}

    // ---------- lookup ----------

    public synchronized Collection<Team> all() { return List.copyOf(teams.values()); }

    public synchronized Team byId(UUID id) { return teams.get(id); }

    public synchronized Team teamOf(UUID player) {
        UUID teamId = memberIndex.get(player);
        return teamId == null ? null : teams.get(teamId);
    }

    public synchronized List<Team> pendingInvites(UUID player) {
        List<UUID> ids = inviteIndex.getOrDefault(player, List.of());
        List<Team> out = new ArrayList<>();
        for (UUID id : ids) {
            Team t = teams.get(id);
            if (t != null && t.isInvited(player)) out.add(t);
        }
        return out;
    }

    public synchronized Team byName(String name) {
        for (Team t : teams.values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    // ---------- island claims ----------

    /** Returns team points = number of members (1 point per player). */
    public synchronized int teamPoints(UUID teamId) {
        Team team = teams.get(teamId);
        return team == null ? 0 : team.size();
    }

    /** Returns total cost of all islands claimed by this team. */
    public synchronized int teamClaimedCost(UUID teamId) {
        int total = 0;
        for (IslandClaim claim : islandClaims.values()) {
            if (claim.teamId().equals(teamId)) {
                total += claim.cost();
            }
        }
        return total;
    }

    /** Check if team can afford an island of given cost. */
    public synchronized boolean canAfford(UUID teamId, int cost) {
        int points = teamPoints(teamId);
        int spent = teamClaimedCost(teamId);
        return (points - spent) >= cost;
    }

    /** Claim an island for a team. Returns error key or null on success. */
    public synchronized String claimIsland(UUID player, long zoneId, int islandCost) {
        Team team = teamOf(player);
        if (team == null) return "msg.fmm_teams.err.no_team";
        Role role = team.roleOf(player);
        if (role != Role.OWNER && role != Role.COMMANDER) return "msg.fmm_teams.err.no_perm";

        // Check if already claimed
        IslandClaim existing = islandClaims.get(zoneId);
        if (existing != null) {
            if (existing.teamId().equals(team.id())) return "msg.fmm_teams.err.island_already_owned";
            return "msg.fmm_teams.err.island_claimed";
        }

        // Check team budget
        if (!canAfford(team.id(), islandCost)) return "msg.fmm_teams.err.insufficient_points";

        islandClaims.put(zoneId, new IslandClaim(zoneId, team.id(), islandCost));
        setDirty();
        return null;
    }

    /** Unclaim an island. Returns error key or null on success. */
    public synchronized String unclaimIsland(UUID player, long zoneId) {
        Team team = teamOf(player);
        if (team == null) return "msg.fmm_teams.err.no_team";
        Role role = team.roleOf(player);
        if (role != Role.OWNER && role != Role.COMMANDER) return "msg.fmm_teams.err.no_perm";

        IslandClaim claim = islandClaims.get(zoneId);
        if (claim == null) return "msg.fmm_teams.err.island_not_claimed";
        if (!claim.teamId().equals(team.id())) return "msg.fmm_teams.err.island_not_yours";

        islandClaims.remove(zoneId);
        setDirty();
        return null;
    }

    /** Get team that owns an island, or null. */
    public synchronized UUID islandOwner(long zoneId) {
        IslandClaim claim = islandClaims.get(zoneId);
        return claim == null ? null : claim.teamId();
    }

    /** Get all islands claimed by a team. */
    public synchronized List<Long> teamIslands(UUID teamId) {
        List<Long> out = new ArrayList<>();
        for (IslandClaim claim : islandClaims.values()) {
            if (claim.teamId().equals(teamId)) out.add(claim.zoneId());
        }
        return out;
    }

    /** Get claim info for an island. */
    public synchronized IslandClaim getClaim(long zoneId) {
        return islandClaims.get(zoneId);
    }

    /** Get all claims. */
    public synchronized Collection<IslandClaim> allClaims() {
        return List.copyOf(islandClaims.values());
    }

    // ---------- mutations, all return error key or null on success ----------

    public synchronized String create(String rawName, UUID owner, String ownerName) {
        if (memberIndex.containsKey(owner)) return "msg.fmm_teams.err.already_in_team";
        String name = sanitizeName(rawName);
        if (name == null) return "msg.fmm_teams.err.bad_name";
        if (byName(name) != null) return "msg.fmm_teams.err.name_taken";
        Team team = new Team(UUID.randomUUID(), name, owner, ownerName, System.currentTimeMillis());
        teams.put(team.id(), team);
        memberIndex.put(owner, team.id());
        setDirty();
        return null;
    }

    public synchronized String invite(UUID actor, UUID target, String targetName) {
        Team team = teamOf(actor);
        if (team == null) return "msg.fmm_teams.err.no_team";
        Role actorRole = team.roleOf(actor);
        if (actorRole != Role.OWNER && actorRole != Role.COMMANDER) return "msg.fmm_teams.err.no_perm";
        if (memberIndex.containsKey(target)) return "msg.fmm_teams.err.target_in_team";
        if (team.isMember(target)) return "msg.fmm_teams.err.target_in_team";
        if (team.size() >= MAX_TEAM_SIZE) return "msg.fmm_teams.err.team_full";
        if (!team.invite(target, targetName)) return "msg.fmm_teams.err.already_invited";
        inviteIndex.computeIfAbsent(target, k -> new ArrayList<>()).add(team.id());
        setDirty();
        return null;
    }

    public synchronized String accept(UUID player, String playerName, UUID teamId) {
        if (memberIndex.containsKey(player)) return "msg.fmm_teams.err.already_in_team";
        Team team = teams.get(teamId);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (!team.isInvited(player)) return "msg.fmm_teams.err.not_invited";
        if (team.size() >= MAX_TEAM_SIZE) return "msg.fmm_teams.err.team_full";
        team.touchName(player, playerName);
        team.addMember(player, playerName);
        memberIndex.put(player, team.id());
        clearInvite(player, team.id());
        setDirty();
        return null;
    }

    public synchronized String decline(UUID player, UUID teamId) {
        Team team = teams.get(teamId);
        if (team == null) {
            clearInvite(player, teamId);
            return null;
        }
        team.uninvite(player);
        clearInvite(player, teamId);
        setDirty();
        return null;
    }

    public synchronized String leave(UUID player) {
        Team team = teamOf(player);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (player.equals(team.owner())) {
            if (team.size() == 1) {
                disbandInternal(team);
                setDirty();
                return null;
            }
            return "msg.fmm_teams.err.owner_leave";
        }
        team.removeMember(player);
        memberIndex.remove(player);
        // Unclaim islands if team can no longer afford them
        rebalanceClaims(team.id());
        setDirty();
        return null;
    }

    public synchronized String kick(UUID actor, UUID target) {
        Team team = teamOf(actor);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (!target.equals(actor) && !team.isMember(target)) {
            Team targetTeam = teamOf(target);
            if (targetTeam == null || !targetTeam.id().equals(team.id())) return "msg.fmm_teams.err.not_member";
        }
        if (actor.equals(target)) return leave(actor);
        Role actorRole = team.roleOf(actor);
        Role targetRole = team.roleOf(target);
        if (targetRole == null) return "msg.fmm_teams.err.not_member";
        if (actorRole == Role.PRIVATE) return "msg.fmm_teams.err.no_perm";
        if (targetRole == Role.OWNER) return "msg.fmm_teams.err.no_perm";
        if (actorRole == Role.COMMANDER && targetRole != Role.PRIVATE) return "msg.fmm_teams.err.no_perm";
        team.removeMember(target);
        memberIndex.remove(target);
        // Unclaim islands if team can no longer afford them
        rebalanceClaims(team.id());
        setDirty();
        return null;
    }

    public synchronized String promote(UUID actor, UUID target) {
        Team team = teamOf(actor);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (!team.isMember(target)) return "msg.fmm_teams.err.not_member";
        if (!actor.equals(team.owner())) return "msg.fmm_teams.err.no_perm";
        if (team.roleOf(target) != Role.PRIVATE) return "msg.fmm_teams.err.bad_role";
        team.setRole(target, Role.COMMANDER);
        setDirty();
        return null;
    }

    public synchronized String demote(UUID actor, UUID target) {
        Team team = teamOf(actor);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (!team.isMember(target)) return "msg.fmm_teams.err.not_member";
        if (!actor.equals(team.owner())) return "msg.fmm_teams.err.no_perm";
        if (team.roleOf(target) != Role.COMMANDER) return "msg.fmm_teams.err.bad_role";
        team.setRole(target, Role.PRIVATE);
        setDirty();
        return null;
    }

    public synchronized String transfer(UUID actor, UUID target) {
        Team team = teamOf(actor);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (!actor.equals(team.owner())) return "msg.fmm_teams.err.no_perm";
        if (!team.isMember(target)) return "msg.fmm_teams.err.not_member";
        team.transferOwner(target);
        setDirty();
        return null;
    }

    public synchronized String disband(UUID actor) {
        Team team = teamOf(actor);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (!actor.equals(team.owner())) return "msg.fmm_teams.err.no_perm";
        disbandInternal(team);
        setDirty();
        return null;
    }

    /** Unclaim islands if team points < total claim cost. Unclaims most expensive first. */
    private void rebalanceClaims(UUID teamId) {
        int points = teamPoints(teamId);
        int spent = teamClaimedCost(teamId);
        if (spent <= points) return;

        // Collect team's claims sorted by cost descending
        List<IslandClaim> teamClaims = new ArrayList<>();
        for (IslandClaim claim : islandClaims.values()) {
            if (claim.teamId().equals(teamId)) teamClaims.add(claim);
        }
        teamClaims.sort((a, b) -> Integer.compare(b.cost(), a.cost()));

        // Remove claims until affordable
        for (IslandClaim claim : teamClaims) {
            if (spent <= points) break;
            islandClaims.remove(claim.zoneId());
            spent -= claim.cost();
        }
    }

    private void disbandInternal(Team team) {
        teams.remove(team.id());
        memberIndex.entrySet().removeIf(e -> e.getValue().equals(team.id()));
        // Unclaim all islands owned by this team
        islandClaims.entrySet().removeIf(e -> e.getValue().teamId().equals(team.id()));
        for (UUID invited : team.invitesView()) {
            List<UUID> list = inviteIndex.get(invited);
            if (list != null) {
                list.remove(team.id());
                if (list.isEmpty()) inviteIndex.remove(invited);
            }
        }
    }

    private void clearInvite(UUID player, UUID teamId) {
        List<UUID> list = inviteIndex.get(player);
        if (list != null) {
            list.remove(teamId);
            if (list.isEmpty()) inviteIndex.remove(player);
        }
    }

    public synchronized void touchPlayer(UUID player, String name) {
        Team t = teamOf(player);
        if (t != null) {
            t.touchName(player, name);
            setDirty();
        }
    }

    public static String sanitizeName(String raw) {
        if (raw == null) return null;
        String s = raw.trim().replaceAll("\\s+", " ");
        if (s.length() < 3 || s.length() > MAX_NAME_LEN) return null;
        if (!s.matches("[\\p{L}0-9 _\\-!]+")) return null;
        return s;
    }

    // ---------- persistence ----------

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Team t : teams.values()) {
            CompoundTag ct = new CompoundTag();
            ct.putUUID("id", t.id());
            ct.putString("name", t.name());
            ct.putUUID("owner", t.owner());
            ct.putLong("created", t.createdAt());
            ListTag members = new ListTag();
            for (Map.Entry<UUID, Role> e : t.rolesView().entrySet()) {
                CompoundTag m = new CompoundTag();
                m.putUUID("uuid", e.getKey());
                m.putString("role", e.getValue().id());
                m.putString("name", t.nameOf(e.getKey()));
                members.add(m);
            }
            ct.put("members", members);
            ListTag inv = new ListTag();
            for (UUID u : t.invitesView()) {
                CompoundTag m = new CompoundTag();
                m.putUUID("uuid", u);
                m.putString("name", t.inviteNamesView().getOrDefault(u, "?"));
                inv.add(m);
            }
            ct.put("invites", inv);
            list.add(ct);
        }
        tag.put("teams", list);

        // Save island claims
        ListTag claimsList = new ListTag();
        for (IslandClaim claim : islandClaims.values()) {
            CompoundTag ct = new CompoundTag();
            ct.putLong("zoneId", claim.zoneId());
            ct.putUUID("teamId", claim.teamId());
            ct.putInt("cost", claim.cost());
            claimsList.add(ct);
        }
        tag.put("islandClaims", claimsList);

        return tag;
    }

    public static TeamManager load(CompoundTag tag, HolderLookup.Provider provider) {
        TeamManager mgr = new TeamManager();
        ListTag list = tag.getList("teams", Tag.TAG_COMPOUND);
        for (Tag tt : list) {
            CompoundTag ct = (CompoundTag) tt;
            UUID id = ct.getUUID("id");
            String name = ct.getString("name");
            UUID owner = ct.getUUID("owner");
            long created = ct.contains("created") ? ct.getLong("created") : System.currentTimeMillis();
            String ownerName = "?";
            ListTag members = ct.getList("members", Tag.TAG_COMPOUND);
            // find owner name first
            for (Tag mt : members) {
                CompoundTag m = (CompoundTag) mt;
                if (m.getUUID("uuid").equals(owner)) ownerName = m.getString("name");
            }
            Team team = new Team(id, name, owner, ownerName, created);
            for (Tag mt : members) {
                CompoundTag m = (CompoundTag) mt;
                UUID u = m.getUUID("uuid");
                if (u.equals(owner)) {
                    team.touchName(u, m.getString("name"));
                    continue;
                }
                team.addMember(u, m.getString("name"));
                team.setRole(u, Role.fromId(m.getString("role")));
            }
            // owner role could be stored as owner anyway
            ListTag inv = ct.getList("invites", Tag.TAG_COMPOUND);
            for (Tag it : inv) {
                CompoundTag m = (CompoundTag) it;
                UUID u = m.getUUID("uuid");
                team.invite(u, m.contains("name") ? m.getString("name") : "?");
                mgr.inviteIndex.computeIfAbsent(u, k -> new ArrayList<>()).add(id);
            }
            mgr.teams.put(id, team);
            for (UUID member : team.rolesView().keySet()) {
                mgr.memberIndex.put(member, id);
            }
        }

        // Load island claims
        if (tag.contains("islandClaims")) {
            ListTag claimsList = tag.getList("islandClaims", Tag.TAG_COMPOUND);
            for (Tag ct : claimsList) {
                CompoundTag c = (CompoundTag) ct;
                long zoneId = c.getLong("zoneId");
                UUID teamId = c.getUUID("teamId");
                int cost = c.getInt("cost");
                mgr.islandClaims.put(zoneId, new IslandClaim(zoneId, teamId, cost));
            }
        }

        return mgr;
    }

    public static TeamManager get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TeamManager::new, TeamManager::load, null), SAVE_KEY);
    }

    /** All UUIDs that must be re-synced after a mutation (members + invited). */
    public synchronized List<UUID> affected(Team team) {
        List<UUID> out = new ArrayList<>(team.rolesView().keySet());
        out.addAll(team.invitesView());
        return out;
    }

    // ---------- IslandClaim record ----------

    public record IslandClaim(long zoneId, UUID teamId, int cost) {}
}
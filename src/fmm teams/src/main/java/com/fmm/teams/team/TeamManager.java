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

    /** Returns team points = number of members (1 point per player) + admin bonus. */
    public synchronized int teamPoints(UUID teamId) {
        Team team = teams.get(teamId);
        return team == null ? 0 : team.size() + team.bonus();
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
        return claimIsland(player, zoneId, islandCost, "UNKNOWN", 0, 0);
    }

    /** Claim an island, recording its tier and centre so it can be listed without world lookups. */
    public synchronized String claimIsland(UUID player, long zoneId, int islandCost,
                                            String tierId, double centerX, double centerZ) {
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

        islandClaims.put(zoneId, new IslandClaim(zoneId, team.id(), islandCost, tierId, centerX, centerZ));
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

    // ---------- admin operations (bypass role checks, still charge points) ----------

    /**
     * Admin island grant. Charges the team like a normal claim, but skips the role check
     * and the "already claimed" error is still enforced. Returns error key or null on success.
     */
    public synchronized String adminClaimIsland(UUID teamId, long zoneId, int islandCost,
                                               String tierId, double centerX, double centerZ) {
        Team team = teams.get(teamId);
        if (team == null) return "msg.fmm_teams.err.no_team";
        IslandClaim existing = islandClaims.get(zoneId);
        if (existing != null) return "msg.fmm_teams.err.island_claimed";
        if (!canAfford(team.id(), islandCost)) return "msg.fmm_teams.err.insufficient_points";
        islandClaims.put(zoneId, new IslandClaim(zoneId, team.id(), islandCost, tierId, centerX, centerZ));
        setDirty();
        return null;
    }

    /** Admin island revoke, regardless of who owns it. Returns error key or null on success. */
    public synchronized String adminUnclaimIsland(long zoneId) {
        if (islandClaims.remove(zoneId) == null) return "msg.fmm_teams.err.island_not_claimed";
        setDirty();
        return null;
    }

    /**
     * Admin island grant that may skip the point budget. With {@code free = true} the claim is
     * recorded at full cost but the team is allowed to go into the red, which is what an operator
     * wants when fixing a broken economy by hand.
     * Returns error key or null on success.
     */
    public synchronized String adminClaimIsland(UUID teamId, long zoneId, int islandCost,
                                               String tierId, double centerX, double centerZ,
                                               boolean free) {
        Team team = teams.get(teamId);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (islandClaims.containsKey(zoneId)) return "msg.fmm_teams.err.island_claimed";
        if (!free && !canAfford(team.id(), islandCost)) return "msg.fmm_teams.err.insufficient_points";
        islandClaims.put(zoneId, new IslandClaim(zoneId, team.id(), islandCost, tierId, centerX, centerZ));
        setDirty();
        return null;
    }

    /** Adds (or with a negative delta, removes) admin bonus points. Clamped at 0. */
    public synchronized String addBonus(UUID teamId, int delta) {
        Team team = teams.get(teamId);
        if (team == null) return "msg.fmm_teams.err.no_team";
        team.addBonus(delta);
        setDirty();
        return null;
    }

    /** Sets the admin bonus to an exact value. Clamped at 0. */
    public synchronized String setBonus(UUID teamId, int bonus) {
        Team team = teams.get(teamId);
        if (team == null) return "msg.fmm_teams.err.no_team";
        team.setBonus(bonus);
        setDirty();
        return null;
    }

    /** Admin kick: removes a member without a role check and rebalances claims. */
    public synchronized String adminKick(UUID teamId, UUID target) {
        Team team = teams.get(teamId);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (!team.isMember(target)) return "msg.fmm_teams.err.not_member";
        if (target.equals(team.owner())) return "msg.fmm_teams.err.owner_leave";
        team.removeMember(target);
        memberIndex.remove(target);
        rebalanceClaims(team.id());
        setDirty();
        return null;
    }

    /**
     * Admin kick that also copes with the owner: ownership moves to the first remaining member,
     * and a one-man party is disbanded outright. Kicking the last person standing must never leave
     * an ownerless team behind. Returns error key or null on success.
     */
    public synchronized String adminKickHard(UUID teamId, UUID target) {
        Team team = teams.get(teamId);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (!team.isMember(target)) return "msg.fmm_teams.err.not_member";
        if (target.equals(team.owner())) {
            if (team.size() == 1) {
                disbandInternal(team);
                setDirty();
                return null;
            }
            List<UUID> rest = new ArrayList<>(team.sortedMembers());
            rest.remove(target);
            team.transferOwner(rest.get(0));
            team.removeMember(target);
            memberIndex.remove(target);
            rebalanceClaims(team.id());
            setDirty();
            return null;
        }
        return adminKick(teamId, target);
    }

    /** Admin disband: drops the party and every island it held, with no owner check. */
    public synchronized String adminDisband(UUID teamId) {
        Team team = teams.get(teamId);
        if (team == null) return "msg.fmm_teams.err.no_team";
        disbandInternal(team);
        setDirty();
        return null;
    }

    /**
     * Admin force-join: pulls a player into the party even if they already belong to another one,
     * which is what an operator needs to untangle a messy player base. Their old party is
     * rebalanced so it does not keep islands it can no longer pay for.
     * Returns error key or null on success.
     */
    public synchronized String adminAddMember(UUID teamId, UUID player, String playerName) {
        Team team = teams.get(teamId);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (team.isMember(player)) return "msg.fmm_teams.err.already_in_team";
        if (team.size() >= MAX_TEAM_SIZE) return "msg.fmm_teams.err.team_full";

        Team previous = teamOf(player);
        if (previous != null) {
            previous.removeMember(player);
            memberIndex.remove(player);
            rebalanceClaims(previous.id());
        }
        clearInvite(player, teamId);
        team.addMember(player, playerName);
        memberIndex.put(player, teamId);
        setDirty();
        return null;
    }

    /** Admin role change without an owner check. Ownership only moves through transfer. */
    public synchronized String adminSetRole(UUID teamId, UUID target, Role role) {
        Team team = teams.get(teamId);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (!team.isMember(target)) return "msg.fmm_teams.err.not_member";
        if (role == Role.OWNER) return "msg.fmm_teams.err.bad_role";
        if (target.equals(team.owner())) return "msg.fmm_teams.err.owner_leave";
        if (!team.setRole(target, role)) return "msg.fmm_teams.err.bad_role";
        setDirty();
        return null;
    }

    /** Admin ownership transfer, no owner check. */
    public synchronized String adminTransfer(UUID teamId, UUID target) {
        Team team = teams.get(teamId);
        if (team == null) return "msg.fmm_teams.err.no_team";
        if (!team.isMember(target)) return "msg.fmm_teams.err.not_member";
        if (target.equals(team.owner())) return "msg.fmm_teams.err.bad_role";
        team.transferOwner(target);
        setDirty();
        return null;
    }

    /** Admin rename, validated exactly like a fresh party name. */
    public synchronized String adminRename(UUID teamId, String rawName) {
        Team team = teams.get(teamId);
        if (team == null) return "msg.fmm_teams.err.no_team";
        String name = sanitizeName(rawName);
        if (name == null) return "msg.fmm_teams.err.bad_name";
        Team other = byName(name);
        if (other != null && !other.id().equals(teamId)) return "msg.fmm_teams.err.name_taken";
        team.rename(name);
        setDirty();
        return null;
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
            // bonus was added later; older saves simply load it as 0
            ct.putInt("bonus", t.bonus());
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
            ct.putString("tier", claim.tierId());
            ct.putDouble("cx", claim.centerX());
            ct.putDouble("cz", claim.centerZ());
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
            if (ct.contains("bonus")) team.setBonus(ct.getInt("bonus"));
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
                // tier/centre were added after the first save format; older saves fall back to UNKNOWN
                String tier = c.contains("tier") ? c.getString("tier") : "UNKNOWN";
                double cx = c.contains("cx") ? c.getDouble("cx") : 0;
                double cz = c.contains("cz") ? c.getDouble("cz") : 0;
                mgr.islandClaims.put(zoneId, new IslandClaim(zoneId, teamId, cost, tier, cx, cz));
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

    /**
     * A claimed island. Tier id and centre are stored alongside the cost so the claim stays
     * displayable without re-deriving the island from the world — zoneId is a hash of the
     * worldgen grid cell and is not cheaply invertible.
     */
    public record IslandClaim(long zoneId, UUID teamId, int cost, String tierId, double centerX, double centerZ) {
        /** Back-compatible constructor for saves written before tier/centre were persisted. */
        public IslandClaim(long zoneId, UUID teamId, int cost) {
            this(zoneId, teamId, cost, "UNKNOWN", 0, 0);
        }
    }
}
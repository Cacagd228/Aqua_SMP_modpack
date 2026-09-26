package com.fmm.teams.team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side team. Members map always contains the owner.
 * New members join as {@link Role#PRIVATE} by default.
 * One player can only be in one team — enforced by {@link TeamManager}.
 */
public final class Team {
    private final UUID id;
    private String name;
    private UUID owner;
    private final Map<UUID, Role> roles = new LinkedHashMap<>();
    private final Map<UUID, String> names = new LinkedHashMap<>();
    private final Set<UUID> invites = new LinkedHashSet<>();
    private final Map<UUID, String> inviteNames = new LinkedHashMap<>();
    private final long createdAt;

    public Team(UUID id, String name, UUID owner, String ownerName, long createdAt) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.createdAt = createdAt;
        roles.put(owner, Role.OWNER);
        names.put(owner, ownerName);
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public void rename(String name) { this.name = name; }
    public UUID owner() { return owner; }
    public long createdAt() { return createdAt; }

    public boolean isMember(UUID player) { return roles.containsKey(player); }
    public boolean isInvited(UUID player) { return invites.contains(player); }

    public Role roleOf(UUID player) {
        return roles.getOrDefault(player, null);
    }

    public int size() { return roles.size(); }

    public Map<UUID, Role> rolesView() { return Map.copyOf(roles); }
    public Map<UUID, String> namesView() { return Map.copyOf(names); }
    public Set<UUID> invitesView() { return Set.copyOf(invites); }
    public Map<UUID, String> inviteNamesView() { return Map.copyOf(inviteNames); }

    public String nameOf(UUID player) {
        return names.getOrDefault(player, player.toString().substring(0, 8));
    }

    public void touchName(UUID player, String name) {
        if (roles.containsKey(player)) names.put(player, name);
        if (invites.contains(player)) inviteNames.put(player, name);
    }

    /** Adds a newcomer as PRIVATE. Returns false if already in team. */
    public boolean addMember(UUID player, String playerName) {
        if (roles.containsKey(player)) return false;
        roles.put(player, Role.PRIVATE);
        names.put(player, playerName);
        invites.remove(player);
        inviteNames.remove(player);
        return true;
    }

    public boolean removeMember(UUID player) {
        invites.remove(player);
        inviteNames.remove(player);
        if (roles.remove(player) == null) return false;
        names.remove(player);
        return true;
    }

    public boolean invite(UUID player, String playerName) {
        if (roles.containsKey(player)) return false;
        if (!invites.add(player)) return false;
        inviteNames.put(player, playerName);
        return true;
    }

    public boolean uninvite(UUID player) {
        inviteNames.remove(player);
        return invites.remove(player);
    }

    public boolean setRole(UUID player, Role role) {
        if (!roles.containsKey(player)) return false;
        if (player.equals(owner) && role != Role.OWNER) return false;
        roles.put(player, role);
        return true;
    }

    /** Transfers ownership: old owner becomes COMMANDER, new owner becomes OWNER. */
    public boolean transferOwner(UUID newOwner) {
        if (!roles.containsKey(newOwner) || newOwner.equals(owner)) return false;
        roles.put(owner, Role.COMMANDER);
        roles.put(newOwner, Role.OWNER);
        owner = newOwner;
        return true;
    }

    /** Members sorted by role (owner top, commander, private), then by name. */
    public List<UUID> sortedMembers() {
        List<UUID> list = new ArrayList<>(roles.keySet());
        list.sort(Comparator
                .comparingInt((UUID u) -> roles.get(u).weight)
                .thenComparing(u -> names.getOrDefault(u, "?"), String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public int countRole(Role role) {
        int n = 0;
        for (Role r : roles.values()) if (r == role) n++;
        return n;
    }
}

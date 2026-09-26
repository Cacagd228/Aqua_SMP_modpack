package com.fmm.teams.team;

/** Roles in priority order: OWNER on top, then COMMANDER, then PRIVATE (default for newcomers). */
public enum Role {
    OWNER(0, "role.fmm_teams.owner", 0xFFE8C06C),
    COMMANDER(1, "role.fmm_teams.commander", 0xFF7DD3FC),
    PRIVATE(2, "role.fmm_teams.private", 0xFFACA188);

    public final int weight;
    public final String langKey;
    /** Panoptic-style accent color for the role label. */
    public final int color;

    Role(int weight, String langKey, int color) {
        this.weight = weight;
        this.langKey = langKey;
        this.color = color;
    }

    public static Role fromId(String id) {
        if (id == null) return PRIVATE;
        return switch (id.toLowerCase()) {
            case "owner" -> OWNER;
            case "commander", "officer" -> COMMANDER;
            default -> PRIVATE;
        };
    }

    public String id() {
        return name().toLowerCase();
    }
}

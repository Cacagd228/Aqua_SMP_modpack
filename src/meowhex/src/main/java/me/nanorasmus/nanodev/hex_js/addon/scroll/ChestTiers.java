package me.nanorasmus.nanodev.hex_js.addon.scroll;

import java.util.Set;

/**
 * Shared chest value tiers: every {@code chests/*} loot table rolls the base
 * ("common") chance, richer vanilla tables roll higher chances. Covers modded
 * chests too (they fall back to the common chance).
 */
public final class ChestTiers {
    /** Highest tier: endgame loot (ancient city, end city, mansion, buried treasure). */
    private static final Set<String> TREASURE_TABLES = Set.of(
            "chests/ancient_city",
            "chests/end_city_treasure",
            "chests/woodland_mansion",
            "chests/buried_treasure");

    /** Middle tier: dungeons, mineshafts, fortresses and similar. */
    private static final Set<String> RICH_TABLES = Set.of(
            "chests/simple_dungeon",
            "chests/abandoned_mineshaft",
            "chests/nether_bridge",
            "chests/bastion_treasure",
            "chests/bastion_other",
            "chests/desert_pyramid",
            "chests/jungle_temple",
            "chests/jungle_temple_dispenser",
            "chests/pillager_outpost",
            "chests/stronghold_corridor",
            "chests/stronghold_crossing",
            "chests/stronghold_library",
            "chests/shipwreck_treasure",
            "chests/ruined_portal");

    private ChestTiers() {
    }

    public static float chanceFor(String tablePath, float common, float rich, float treasure) {
        if (TREASURE_TABLES.contains(tablePath)) {
            return treasure;
        }
        if (RICH_TABLES.contains(tablePath)
                || tablePath.startsWith("chests/trial_chambers/")
                || tablePath.startsWith("chests/bastion_")
                || tablePath.startsWith("chests/stronghold_")
                || tablePath.startsWith("chests/village/village_weapon")
                || tablePath.startsWith("chests/village/village_tool")) {
            return rich;
        }
        return common;
    }
}

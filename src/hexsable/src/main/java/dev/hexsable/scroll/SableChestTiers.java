package dev.hexsable.scroll;

import java.util.Set;

/**
 * Общие тиры ценности сундуков: каждый лут-стол {@code chests/*} крутит
 * базовый ("common") шанс, более богатые ванильные столы — повышенные.
 * Модовые сундуки падают на общий шанс.
 *
 * <p>Копия {@code me.nanorasmus.nanodev.hex_js.addon.scroll.ChestTiers} из MeowHex.
 */
public final class SableChestTiers {
    /** Высший тир: эндгейм-лут (древний город, город Края, особняк, клад). */
    private static final Set<String> TREASURE_TABLES = Set.of(
            "chests/ancient_city",
            "chests/end_city_treasure",
            "chests/woodland_mansion",
            "chests/buried_treasure");

    /** Средний тир: данжи, шахты, крепости и подобное. */
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

    private SableChestTiers() {
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

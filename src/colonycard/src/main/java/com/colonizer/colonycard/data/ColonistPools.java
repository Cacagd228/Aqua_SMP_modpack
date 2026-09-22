package com.colonizer.colonycard.data;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Static pools used to randomly generate a colonist's profile on first join,
 * plus the fixed definitions of the 6 "special reward" slots (zones 8-13 on the card).
 */
public final class ColonistPools {
    private ColonistPools() {
    }

    /** 10 possible distinguishing marks; each colonist gets a random subset of these. */
    public static final String[] TRAIT_KEYS = {
            "colonycard.trait.1", "colonycard.trait.2", "colonycard.trait.3", "colonycard.trait.4", "colonycard.trait.5",
            "colonycard.trait.6", "colonycard.trait.7", "colonycard.trait.8", "colonycard.trait.9", "colonycard.trait.10"
    };

    /**
     * Mutually exclusive pairs (same inner array = never issued together):
     * generous/miser, rowdy/calm, taciturn/talkative.
     */
    private static final String[][] EXCLUSIVE_GROUPS = {
            {"colonycard.trait.5", "colonycard.trait.3"},
            {"colonycard.trait.6", "colonycard.trait.7"},
            {"colonycard.trait.4", "colonycard.trait.10"},
    };

    /** How many traits each colonist is assigned on first join. */
    public static final int TRAITS_PER_COLONIST = 2;

    /** Pool of possible "arrival goals"; one is chosen at random on first join. */
    public static final String[] GOAL_KEYS = {
            "colonycard.goal.1", "colonycard.goal.2", "colonycard.goal.3",
            "colonycard.goal.4", "colonycard.goal.5", "colonycard.goal.6"
    };

    /** Pool of archipelago names ("Наименование Архипелага" на левой странице паспорта). */
    public static final String[] ARCHIPELAGO_KEYS = {
            "colonycard.archipelago.1", "colonycard.archipelago.2", "colonycard.archipelago.3",
            "colonycard.archipelago.4", "colonycard.archipelago.5", "colonycard.archipelago.6"
    };

    /**
     * Fixed definitions for the 6 reward slots (zones 8-13 in the layout).
     * loyaltyThreshold is on the card's -100..100 scale (not currently auto-checked
     * anywhere -- rewards are unlocked via /colonycard reward -- but kept in sync
     * with the display scale for whenever that logic is added).
     */
    public record RewardDef(String nameKey, Item icon, int loyaltyThreshold, int contributionThreshold) {
    }

    public static final RewardDef[] REWARDS = {
            new RewardDef("colonycard.reward.name.1", Items.PAPER, 0, 0),
            new RewardDef("colonycard.reward.name.2", Items.BREAD, 0, 0),
            new RewardDef("colonycard.reward.name.3", Items.MAP, 0, 0),
            new RewardDef("colonycard.reward.name.4", Items.IRON_PICKAXE, 20, 0),
            new RewardDef("colonycard.reward.name.5", Items.GOLDEN_HELMET, 60, 0),
            new RewardDef("colonycard.reward.name.6", Items.NETHER_STAR, 80, 0)
    };

    public static List<String> randomTraits(RandomSource random) {
        // Fisher-Yates over a copy, then greedy pick skipping exclusive conflicts.
        List<String> pool = new ArrayList<>(List.of(TRAIT_KEYS));
        for (int i = pool.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String tmp = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, tmp);
        }
        List<String> chosen = new ArrayList<>();
        for (String key : pool) {
            if (chosen.size() >= TRAITS_PER_COLONIST) {
                break;
            }
            if (conflicts(key, chosen)) {
                continue;
            }
            chosen.add(key);
        }
        return chosen;
    }

    private static boolean conflicts(String key, List<String> chosen) {
        for (String[] group : EXCLUSIVE_GROUPS) {
            if (contains(group, key)) {
                for (String c : chosen) {
                    if (contains(group, c)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean contains(String[] group, String key) {
        for (String s : group) {
            if (s.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static String randomGoal(RandomSource random) {
        return GOAL_KEYS[random.nextInt(GOAL_KEYS.length)];
    }

    public static String randomArchipelago(RandomSource random) {
        return ARCHIPELAGO_KEYS[random.nextInt(ARCHIPELAGO_KEYS.length)];
    }
}

package at.petrak.hexcasting.common.loot;

import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

// https://github.com/VazkiiMods/Botania/blob/1.18.x/Xplat/src/main/java/vazkii/botania/common/loot/LootHandler.java
// We need to inject dungeon loot (scrolls and lore), make amethyst drop fewer shards, and the extra dust stuff.
// On forge:
// - Scrolls and lore are done with a loot mod
// - Amethyst drop fiddling is done with another loot mod; the shard delta is in the loot mod data and the rest of
//   the stuff is loaded from TABLE_INJECT_AMETHYST_CLUSTER
public class HexLootHandler {
    public static final ImmutableList<ScrollInjection> DEFAULT_SCROLL_INJECTS = ImmutableList.of(
        // TODO: not sure what the lore implications of scrolls and the nether/end are. adding scrolls
        // there for now just to be nice to players.

        // In places where it doesn't really make sense to have them lore-wise just put them rarely anyways
        // to make it less of a PITA for new players
        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/simple_dungeon"), 1),
        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/abandoned_mineshaft"), 1),
        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/bastion_other"), 1),
        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/nether_bridge"), 1),

        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/jungle_temple"), 2),
        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/desert_pyramid"), 2),
        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_cartographer"), 2),

        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/shipwreck_map"), 3),
        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/bastion_treasure"), 3),
        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/end_city_treasure"), 3),

        // ancient city chests have amethyst in them, thinking emoji
        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/ancient_city"), 4),
        // wonder what those pillagers are up to with those scrolls
        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/pillager_outpost"), 4),

        // if you manage to find one of these things you deserve a lot of scrolls
        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/woodland_mansion"), 5),
        new ScrollInjection(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/stronghold_library"), 5)
    );

    public static final ImmutableList<ResourceLocation> DEFAULT_LORE_INJECTS = ImmutableList.of(
        ResourceLocation.fromNamespaceAndPath("minecraft", "chests/simple_dungeon"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "chests/abandoned_mineshaft"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "chests/pillager_outpost"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "chests/woodland_mansion"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "chests/stronghold_library"),
        // >:)
        ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_desert_house"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_plains_house"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_savanna_house"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_snowy_house"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_taiga_house")
    );

    public static int getScrollCount(int range, RandomSource random) {
        return Math.max(random.nextIntBetweenInclusive(-range, range), 0);
    }

    public static final double DEFAULT_SHARD_MODIFICATION = -0.5;
    public static final double DEFAULT_LORE_CHANCE = 0.4;

    public static final ResourceLocation TABLE_INJECT_AMETHYST_CLUSTER = modLoc("inject/amethyst_cluster");

    public record ScrollInjection(ResourceLocation injectee, int countRange) {
    }
}

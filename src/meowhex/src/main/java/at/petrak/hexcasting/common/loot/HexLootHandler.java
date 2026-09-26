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
    // meowhex: vanilla scroll/lore chest injections removed entirely —
    // pattern scrolls are granted by meowhex:scroll_pages instead, lore fragments
    // no longer exist. Kept as empty lists so datagen never resurrects them.
    public static final ImmutableList<ScrollInjection> DEFAULT_SCROLL_INJECTS = ImmutableList.of();

    public static final ImmutableList<ResourceLocation> DEFAULT_LORE_INJECTS = ImmutableList.of();

    public static int getScrollCount(int range, RandomSource random) {
        return Math.max(random.nextIntBetweenInclusive(-range, range), 0);
    }

    public static final double DEFAULT_SHARD_MODIFICATION = -0.5;
    public static final double DEFAULT_LORE_CHANCE = 0.4;

    public static final ResourceLocation TABLE_INJECT_AMETHYST_CLUSTER = modLoc("inject/amethyst_cluster");

    public record ScrollInjection(ResourceLocation injectee, int countRange) {
    }
}

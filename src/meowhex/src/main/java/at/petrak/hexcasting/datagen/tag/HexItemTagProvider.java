package at.petrak.hexcasting.datagen.tag;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.mod.HexTags;
import at.petrak.hexcasting.common.lib.HexItems;
import at.petrak.hexcasting.xplat.IXplatTags;
import at.petrak.paucal.api.datagen.PaucalItemTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public class HexItemTagProvider extends PaucalItemTagProvider {
    private final IXplatTags xtags;

    public HexItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, TagsProvider<Block> pBlockTagsProvider, IXplatTags xtags) {
        super(output, lookup, HexAPI.MOD_ID, pBlockTagsProvider);
        this.xtags = xtags;
    }



    @Override
    protected void addTags(HolderLookup.Provider provider) {
        add(tag(xtags.gems()),
            HexItems.CHARGED_AMETHYST);
        add(tag(xtags.amethystDust()),
            HexItems.AMETHYST_DUST);

        add(tag(HexTags.Items.STAVES),
            HexItems.STAFF_EDIFIED,
            HexItems.STAFF_OAK, HexItems.STAFF_SPRUCE, HexItems.STAFF_BIRCH,
            HexItems.STAFF_JUNGLE, HexItems.STAFF_ACACIA, HexItems.STAFF_DARK_OAK,
            HexItems.STAFF_CRIMSON, HexItems.STAFF_WARPED, HexItems.STAFF_MANGROVE,
            HexItems.STAFF_CHERRY,HexItems.STAFF_BAMBOO,
            HexItems.STAFF_QUENCHED, HexItems.STAFF_MINDSPLICE);

        // The hextended (meowhex) staves must be in hexcasting:staves too: the
        // spell GUI's own tick() closes itself unless the held item is in this
        // exact tag, so a meowhex-only tag leaves the GUI unusable.
        add(tag(HexTags.Items.STAVES), hextendedStaves());

        add(tag(HexTags.Items.PHIAL_BASE),
            Items.GLASS_BOTTLE);
        add(tag(HexTags.Items.GRANTS_ROOT_ADVANCEMENT),
            HexItems.AMETHYST_DUST, Items.AMETHYST_SHARD,
            HexItems.CHARGED_AMETHYST);
        add(tag(HexTags.Items.SEAL_MATERIALS),
            Items.HONEYCOMB);

        this.copy(HexTags.Blocks.EDIFIED_LOGS, HexTags.Items.EDIFIED_LOGS);
        this.copy(HexTags.Blocks.EDIFIED_PLANKS, HexTags.Items.EDIFIED_PLANKS);
        this.copy(HexTags.Blocks.IMPETI, HexTags.Items.IMPETI);
        this.copy(HexTags.Blocks.DIRECTRICES, HexTags.Items.DIRECTRICES);
        this.copy(HexTags.Blocks.MINDFLAYED_CIRCLE_COMPONENTS, HexTags.Items.MINDFLAYED_CIRCLE_COMPONENTS);
        this.copy(BlockTags.LOGS_THAT_BURN, ItemTags.LOGS_THAT_BURN);
        this.copy(BlockTags.LOGS, ItemTags.LOGS);
        this.copy(BlockTags.PLANKS, ItemTags.PLANKS);
        this.copy(BlockTags.SLABS, ItemTags.SLABS);
        this.copy(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS);
        this.copy(BlockTags.DOORS, ItemTags.DOORS);
        this.copy(BlockTags.WOODEN_DOORS, ItemTags.WOODEN_DOORS);
        this.copy(BlockTags.TRAPDOORS, ItemTags.TRAPDOORS);
        this.copy(BlockTags.WOODEN_TRAPDOORS, ItemTags.WOODEN_TRAPDOORS);
        this.copy(BlockTags.LEAVES, ItemTags.LEAVES);
        // Apparently, there's no "Pressure Plates" item tag.
        this.copy(BlockTags.WOODEN_PRESSURE_PLATES, ItemTags.WOODEN_PRESSURE_PLATES);
        this.copy(BlockTags.BUTTONS, ItemTags.BUTTONS);
        this.copy(BlockTags.WOODEN_BUTTONS, ItemTags.WOODEN_BUTTONS);
    }

    /**
     * The hextended staff set, resolved from the live registry by id so this
     * datagen class (which lives in the {@code at.petrak} package and must not
     * import addon classes) stays decoupled from the addon itself.
     */
    private static Item[] hextendedStaves() {
        String[] ids = {
            "staff/moss", "staff/flowered_moss", "staff/prismarine",
            "staff/dark_prismarine", "staff/obsidian", "staff/purpur",
            "staff/livingwood", "staff/manasteel", "staff/terrasteel",
            "staff/dreamwood", "staff/elementium", "staff/drawing_orb",
        };
        var out = new Item[ids.length];
        for (int i = 0; i < ids.length; i++) {
            out[i] = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("meowhex", ids[i]));
        }
        return out;
    }

    void add(TagAppender<Item> appender, Item... items) {
        for (Item item : items) {
            appender.add(BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow());
        }
    }
}

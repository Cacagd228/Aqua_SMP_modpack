package me.nanorasmus.nanodev.hex_js.addon;

import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemManaBerry;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemManaPieSlice;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mana consumables: the mana berry (permanent max-mana bonus) and the mana pie
 * slice (instant 30% restore). Shown in the HEX Artifacts creative tab.
 */
public final class HexFoodItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HexJS.MOD_ID);

    public static final DeferredHolder<Item, ? extends Item> MANA_BERRY = ITEMS.register("mana_berry",
            () -> new ItemManaBerry(new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, ? extends Item> MANA_PIE_SLICE = ITEMS.register("mana_pie_slice",
            () -> new ItemManaPieSlice(new Item.Properties().stacksTo(64)));

    private HexFoodItems() {
    }

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
    }
}

package me.nanorasmus.nanodev.hex_js.addon;

import at.petrak.hexcasting.common.items.ItemStaff;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemBoundSpellbook;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemBatteryStaff;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemChargedDiadem;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemDrawingOrb;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemExtendedAmethystStaff;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemExtendedStaff;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemSpellbookCover;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * All hextended-staves items, ported into the {@code hex_js} namespace. Faithful to
 * the original (CC0): plain material staffs, extended (+grid-zoom) staffs, battery
 * staffs, the drawing orb, the bound spellbook family and the charged diadem.
 *
 * <p>One deliberate fix vs the original: the oak/birch/spruce id mix-up in the
 * original registry (birch field registered as "spruce" and vice-versa) is corrected
 * so each id matches its field name.
 */
public final class HextendedItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HexJS.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HexJS.MOD_ID);

    /** All registered items, in insertion order — used to fill the creative tab. */
    public static final List<DeferredHolder<Item, ? extends Item>> ALL = new ArrayList<>();

    // ---- plain material staffs (ItemStaff) ----
    public static final DeferredHolder<Item, ? extends Item> MOSS_STAFF = staff("staff/moss", ItemStaff::new);
    public static final DeferredHolder<Item, ? extends Item> FLOWERED_MOSS_STAFF = staff("staff/flowered_moss", ItemStaff::new);
    public static final DeferredHolder<Item, ? extends Item> PRISMARINE_STAFF = staff("staff/prismarine", ItemStaff::new);
    public static final DeferredHolder<Item, ? extends Item> DARK_PRISMARINE_STAFF = staff("staff/dark_prismarine", ItemStaff::new);
    public static final DeferredHolder<Item, ? extends Item> OBSIDIAN_STAFF = staff("staff/obsidian", ItemStaff::new);
    public static final DeferredHolder<Item, ? extends Item> PURPUR_STAFF = staff("staff/purpur", ItemStaff::new);
    public static final DeferredHolder<Item, ? extends Item> LIVINGWOOD_STAFF = staff("staff/livingwood", ItemStaff::new);
    public static final DeferredHolder<Item, ? extends Item> MANASTEEL_STAFF = staff("staff/manasteel", ItemStaff::new);
    public static final DeferredHolder<Item, ? extends Item> TERRASTEEL_STAFF = staff("staff/terrasteel", ItemStaff::new);
    public static final DeferredHolder<Item, ? extends Item> DREAMWOOD_STAFF = staff("staff/dreamwood", ItemStaff::new);
    public static final DeferredHolder<Item, ? extends Item> ELEMENTIUM_STAFF = staff("staff/elementium", ItemStaff::new);

    // ---- extended material staffs (ItemExtendedStaff: +grid zoom) ----
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_MOSS_STAFF = longStaff("staff/long/moss", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_FLOWERED_MOSS_STAFF = longStaff("staff/long/flowered_moss", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_PRISMARINE_STAFF = longStaff("staff/long/prismarine", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_DARK_PRISMARINE_STAFF = longStaff("staff/long/dark_prismarine", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_OBSIDIAN_STAFF = longStaff("staff/long/obsidian", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_PURPUR_STAFF = longStaff("staff/long/purpur", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_LIVINGWOOD_STAFF = longStaff("staff/long/livingwood", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_MANASTEEL_STAFF = longStaff("staff/long/manasteel", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_TERRASTEEL_STAFF = longStaff("staff/long/terrasteel", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_DREAMWOOD_STAFF = longStaff("staff/long/dreamwood", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_ELEMENTIUM_STAFF = longStaff("staff/long/elementium", ItemExtendedStaff::new);

    // ---- extended wood staffs ----
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_OAK_STAFF = longStaff("staff/long/oak", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_SPRUCE_STAFF = longStaff("staff/long/spruce", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_BIRCH_STAFF = longStaff("staff/long/birch", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_JUNGLE_STAFF = longStaff("staff/long/jungle", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_DARK_OAK_STAFF = longStaff("staff/long/dark_oak", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_ACACIA_STAFF = longStaff("staff/long/acacia", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_CRIMSON_STAFF = longStaff("staff/long/crimson", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_WARPED_STAFF = longStaff("staff/long/warped", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_MANGROVE_STAFF = longStaff("staff/long/mangrove", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_BAMBOO_STAFF = longStaff("staff/long/bamboo", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_CHERRY_STAFF = longStaff("staff/long/cherry", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_EDIFIED_STAFF = longStaff("staff/long/edified", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_MINDSPLICE_STAFF = longStaff("staff/long/mindsplice", ItemExtendedStaff::new);
    public static final DeferredHolder<Item, ? extends Item> EXTENDED_QUENCHED_STAFF = longStaff("staff/long/quenched", ItemExtendedStaff::new);

    // ---- battery staffs ----
    public static final DeferredHolder<Item, ? extends Item> LESSER_BATTERY_STAFF = staff("staff/lesser_battery", ItemBatteryStaff::new);
    public static final DeferredHolder<Item, ? extends Item> SEALED_LESSER_BATTERY_STAFF = staff("staff/sealed_lesser_battery", ItemStaff::new);
    public static final DeferredHolder<Item, ? extends Item> LESSER_BATTERY_EXTENDED_STAFF = longStaff("staff/long/lesser_battery", ItemExtendedAmethystStaff::new);
    public static final DeferredHolder<Item, ? extends Item> SEALED_LESSER_BATTERY_EXTENDED_STAFF = longStaff("staff/long/sealed_lesser_battery", ItemExtendedStaff::new);

    // ---- drawing orb ----
    public static final DeferredHolder<Item, ? extends Item> DRAWING_ORB = staff("staff/drawing_orb", ItemDrawingOrb::new);

    // ---- spellbook ----
    public static final DeferredHolder<Item, ? extends Item> SPELLBOOK_COVER = register("spellbook_cover", ItemSpellbookCover::new, 64);
    public static final DeferredHolder<Item, ? extends Item> BOUND_SPELLBOOK = register("bound_spellbook", ItemBoundSpellbook::new, 1);

    // ---- diadem (shown in the HEX Artifacts tab, see HexArtifactsItems) ----
    public static final DeferredHolder<Item, ? extends Item> CHARGED_AMETHYST_DIADEM =
            ITEMS.register("charged_amethyst_diadem", () -> new ItemChargedDiadem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("hextended", () ->
            CreativeModeTab.builder()
                    .icon(() -> new ItemStack(MOSS_STAFF.get()))
                    .title(Component.translatable("itemGroup.meowhex_hextended"))
                    .displayItems((params, out) -> ALL.forEach(h -> out.accept(h.get())))
                    .build());

    private HextendedItems() {
    }

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    /** Convenience for {@link ItemStaff} subclasses; ids mirror the original paths. */
    private static DeferredHolder<Item, ? extends Item> staff(String id, Function<Item.Properties, ? extends ItemStaff> factory) {
        return register(id, factory, 1);
    }

    private static DeferredHolder<Item, ? extends Item> longStaff(String id, Function<Item.Properties, ? extends ItemStaff> factory) {
        return register(id, factory, 1);
    }

    private static DeferredHolder<Item, ? extends Item> register(String id,
                                                                 Function<Item.Properties, ? extends Item> factory,
                                                                 int maxStack) {
        DeferredHolder<Item, ? extends Item> holder =
                ITEMS.register(id, () -> factory.apply(new Item.Properties().stacksTo(maxStack)));
        ALL.add(holder);
        return holder;
    }
}
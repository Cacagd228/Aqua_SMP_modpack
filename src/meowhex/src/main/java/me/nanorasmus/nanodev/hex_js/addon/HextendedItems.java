package me.nanorasmus.nanodev.hex_js.addon;

import at.petrak.hexcasting.common.items.ItemStaff;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemAmethystFishingRod;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemBoundSpellbook;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemChargedDiadem;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemDrawingOrb;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemSpellbookCover;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemStellarTune;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Регистрация предметов бывшей вкладки «Hextended», перенесённых в «HEX Artifacts»
 * (см. {@link HexArtifactsItems#TAB}).
 *
 * <p>Отдельной вкладки у этого набора больше нет: посохи, чертёжный шар, книга
 * и аметистовая удочка показываются в «Артефактах». Убраны вместе с вкладкой
 * длинные посохи ({@code staff/long/*}) и оба батарейных — им места не осталось.
 *
 * <p>Одна намеренная правка относительно оригинала (CC0): в нём поля oak/birch/
 * spruce были перепутаны с id (birch зарегистрирован как «spruce» и наоборот) —
 * здесь каждый id совпадает с названием.
 */
public final class HextendedItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HexJS.MOD_ID);

    /** Зарегистрированные здесь предметы, в порядке регистрации. */
    public static final List<DeferredHolder<Item, ? extends Item>> ALL = new ArrayList<>();

    // ---- обычные посохи (ItemStaff) ----
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

    // ---- чертёжный шар ----
    public static final DeferredHolder<Item, ? extends Item> DRAWING_ORB = staff("staff/drawing_orb", ItemDrawingOrb::new);

    // ---- аметистовая удочка: два поплавка, поднимаются разом ----
    public static final DeferredHolder<Item, ? extends Item> AMETHYST_FISHING_ROD =
            register("amethyst_fishing_rod", ItemAmethystFishingRod::new, 1);

    // ---- «Стеллар Тюн»: звёздная гитара, нота + звезда по курсору ----
    public static final DeferredHolder<Item, ? extends Item> STELLAR_TUNE =
            register("stellar_tune", ItemStellarTune::new, 1);

    // ---- книга заклинаний ----
    public static final DeferredHolder<Item, ? extends Item> SPELLBOOK_COVER = register("spellbook_cover", ItemSpellbookCover::new, 64);
    public static final DeferredHolder<Item, ? extends Item> BOUND_SPELLBOOK = register("bound_spellbook", ItemBoundSpellbook::new, 1);

    // ---- диадема (показывается в «Артефактах», см. HexArtifactsItems) ----
    public static final DeferredHolder<Item, ? extends Item> CHARGED_AMETHYST_DIADEM =
            ITEMS.register("charged_amethyst_diadem", () -> new ItemChargedDiadem(new Item.Properties().stacksTo(1)));

    private HextendedItems() {
    }

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
    }

    /** Convenience for {@link ItemStaff} subclasses; ids mirror the original paths. */
    private static DeferredHolder<Item, ? extends Item> staff(String id, Function<Item.Properties, ? extends ItemStaff> factory) {
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

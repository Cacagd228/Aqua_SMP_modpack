package me.nanorasmus.nanodev.hex_js.addon.armor;

import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Сет Святой Валькирии (геометрия и стандартная текстура avatar_robe):
 * <ul>
 *   <li>holy_valkyrie_helmet — Шлем Святой Валькирии</li>
 *   <li>holy_valkyrie_cuirass — Кираса Святой Валькирии (нагрудник)</li>
 *   <li>holy_valkyrie_leggings — Поножи Святой Валькирии</li>
 *   <li>holy_valkyrie_boots — Сапоги Святой Валькирии</li>
 * </ul>
 */
public final class HolyValkyrieArmorItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HexJS.MOD_ID);

    public static final DeferredHolder<Item, ModGeoArmorItem> HELMET =
            ITEMS.register("holy_valkyrie_helmet", () -> new ModGeoArmorItem(
                    ModArmorMaterials.HOLY_VALKYRIE, ArmorItem.Type.HELMET, "holy_valkyrie", "helmet",
                    new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, ModGeoArmorItem> CUIRASS =
            ITEMS.register("holy_valkyrie_cuirass", () -> new ModGeoArmorItem(
                    ModArmorMaterials.HOLY_VALKYRIE, ArmorItem.Type.CHESTPLATE, "holy_valkyrie", "cuirass",
                    new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, ModGeoArmorItem> LEGGINGS =
            ITEMS.register("holy_valkyrie_leggings", () -> new ModGeoArmorItem(
                    ModArmorMaterials.HOLY_VALKYRIE, ArmorItem.Type.LEGGINGS, "holy_valkyrie", "leggings",
                    new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, ModGeoArmorItem> BOOTS =
            ITEMS.register("holy_valkyrie_boots", () -> new ModGeoArmorItem(
                    ModArmorMaterials.HOLY_VALKYRIE, ArmorItem.Type.BOOTS, "holy_valkyrie", "boots",
                    new Item.Properties().stacksTo(1)));

    private HolyValkyrieArmorItems() {
    }

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
    }
}

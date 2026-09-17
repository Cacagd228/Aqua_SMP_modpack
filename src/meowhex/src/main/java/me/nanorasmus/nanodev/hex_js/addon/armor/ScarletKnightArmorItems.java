package me.nanorasmus.nanodev.hex_js.addon.armor;

import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Сет Алого рыцаря (геометрия deathmantle, бывшая «crimson_mage»):
 * <ul>
 *   <li>scarlet_knight_hood — Капюшон Алого рыцаря (шлем)</li>
 *   <li>scarlet_knight_cuirass — Кираса Алого рыцаря (нагрудник)</li>
 *   <li>scarlet_knight_leggings — Поножи Алого рыцаря</li>
 *   <li>scarlet_knight_boots — Сапоги Алого рыцаря</li>
 * </ul>
 */
public final class ScarletKnightArmorItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HexJS.MOD_ID);

    public static final DeferredHolder<Item, ModGeoArmorItem> HOOD =
            ITEMS.register("scarlet_knight_hood", () -> new ModGeoArmorItem(
                    ModArmorMaterials.SCARLET_KNIGHT, ArmorItem.Type.HELMET, "scarlet_knight", "hood",
                    new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, ModGeoArmorItem> CUIRASS =
            ITEMS.register("scarlet_knight_cuirass", () -> new ModGeoArmorItem(
                    ModArmorMaterials.SCARLET_KNIGHT, ArmorItem.Type.CHESTPLATE, "scarlet_knight", "cuirass",
                    new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, ModGeoArmorItem> LEGGINGS =
            ITEMS.register("scarlet_knight_leggings", () -> new ModGeoArmorItem(
                    ModArmorMaterials.SCARLET_KNIGHT, ArmorItem.Type.LEGGINGS, "scarlet_knight", "leggings",
                    new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, ModGeoArmorItem> BOOTS =
            ITEMS.register("scarlet_knight_boots", () -> new ModGeoArmorItem(
                    ModArmorMaterials.SCARLET_KNIGHT, ArmorItem.Type.BOOTS, "scarlet_knight", "boots",
                    new Item.Properties().stacksTo(1)));

    private ScarletKnightArmorItems() {
    }

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
    }
}

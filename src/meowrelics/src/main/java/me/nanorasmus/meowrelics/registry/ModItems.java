package me.nanorasmus.meowrelics.registry;

import me.nanorasmus.meowrelics.MeowRelics;
import me.nanorasmus.meowrelics.item.LootKind;
import me.nanorasmus.meowrelics.item.RelicBagItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Предметы мода. Мешочки расходуются целиком, но копятся в стаке. */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MeowRelics.MOD_ID);

    /** Обычная начинка: случайная реликвия по лут-весам. */
    public static final DeferredItem<RelicBagItem> RELIC_BAG = bag("relic_bag", LootKind.RELIC);

    /** Повышенный шанс на мощные реликвии. */
    public static final DeferredItem<RelicBagItem> RELIC_BAG_RARE =
            bag("relic_bag_rare", LootKind.POWERFUL_RELIC);

    /** Артефакты meowhex. */
    public static final DeferredItem<RelicBagItem> RELIC_BAG_HEXCASTING =
            bag("relic_bag_hexcasting", LootKind.HEX_ARTIFACT);

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private static DeferredItem<RelicBagItem> bag(String name, LootKind kind) {
        return ITEMS.registerItem(name,
                properties -> new RelicBagItem(properties.stacksTo(64), kind));
    }

    private ModItems() {
    }
}

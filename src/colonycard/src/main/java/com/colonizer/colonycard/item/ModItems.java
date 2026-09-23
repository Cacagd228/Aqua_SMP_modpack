package com.colonizer.colonycard.item;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.block.ModBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Предметы мода. Пока один — бумажная грамота (имперский декрет о награждении).
 * Текстура-заглушка: обычный лист бумаги (модель указывает на minecraft:item/paper).
 */
public final class ModItems {
    private ModItems() {
    }

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ColonyCardMod.MODID);

    public static final DeferredItem<ImperialDecreeItem> IMPERIAL_DECREE = ITEMS.register(
            "imperial_decree",
            () -> new ImperialDecreeItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<?> STAGE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("stage_block", ModBlocks.STAGE_BLOCK);

    /** Кладём грамоту в стандартную вкладку, чтобы была видна и без команды. */
    @EventBusSubscriber(modid = ColonyCardMod.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Tabs {
        private Tabs() {
        }

        @SubscribeEvent
        public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                event.accept(IMPERIAL_DECREE);
                event.accept(STAGE_BLOCK_ITEM);
            }
        }
    }
}

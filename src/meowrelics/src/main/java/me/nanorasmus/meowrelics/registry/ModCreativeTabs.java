package me.nanorasmus.meowrelics.registry;

import me.nanorasmus.meowrelics.MeowRelics;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Своя вкладка в креативе — чтобы мешочки не терялись среди чужих модов. */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MeowRelics.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.meowrelics"))
                    .icon(() -> ModItems.RELIC_BAG.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.RELIC_BAG.get());
                        output.accept(ModItems.RELIC_BAG_RARE.get());
                        output.accept(ModItems.RELIC_BAG_HEXCASTING.get());
                    })
                    .build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    private ModCreativeTabs() {
    }
}

package me.nanorasmus.nanodev.hex_js.addon.scroll;

import com.mojang.serialization.MapCodec;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ScrollItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HexJS.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HexJS.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODS =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, HexJS.MOD_ID);

    public static final Map<String, DeferredHolder<Item, ? extends Item>> BY_ID = new LinkedHashMap<>();

    static {
        for (String scrollId : ScrollDefs.SCROLLS) {
            DeferredHolder<Item, ? extends Item> holder = ITEMS.register(scrollId,
                    () -> new ItemPatternScroll(new Item.Properties().stacksTo(64), scrollId));
            BY_ID.put(scrollId, holder);
        }
    }

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<EnchantedBookLootModifier>> ENCHANTED_BOOKS =
            LOOT_MODS.register("enchanted_books", EnchantedBookLootModifier.CODEC);

    public static final DeferredHolder<Item, SealedScrollItem> SEALED_SCROLL = ITEMS.register("sealed_scroll",
            () -> new SealedScrollItem(new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("scrolls", () ->
            CreativeModeTab.builder()
                    .icon(() -> new ItemStack(BY_ID.get(ScrollDefs.SCROLLS.get(0)).get()))
                    .title(Component.translatable("itemGroup.meowhex_scrolls"))
                    .displayItems((params, out) -> {
                        BY_ID.values().forEach(h -> out.accept(h.get()));
                        out.accept(new ItemStack(SEALED_SCROLL.get()));
                    })
                    .build());

    private ScrollItems() {
    }

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
        LOOT_MODS.register(bus);
    }
}

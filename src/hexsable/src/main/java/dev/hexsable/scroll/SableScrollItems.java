package dev.hexsable.scroll;

import com.mojang.serialization.MapCodec;
import dev.hexsable.HexSable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Регистрация свитков Hex Sable Bridge и типа глобального
 * лут-модификатора. По образцу
 * {@code me.nanorasmus.nanodev.hex_js.addon.scroll.ScrollItems} из MeowHex.
 *
 * <p>Отдельной вкладки нет — свитки докладываются в обычную вкладку
 * свитков MeowHex ({@code meowhex:scrolls}, см. {@link #onBuildTabContents}).
 */
public final class SableScrollItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HexSable.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, HexSable.MOD_ID);

    /** Ключ обычной вкладки свитков MeowHex ("Scrolls of Knowledge"). */
    private static final ResourceKey<CreativeModeTab> MEOWHEX_SCROLLS_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath("meowhex", "scrolls"));

    /** Scroll item id -> holder, в порядке {@link SableScrollDefs#SCROLLS}. */
    public static final Map<String, DeferredHolder<Item, ? extends Item>> BY_ID = new LinkedHashMap<>();

    static {
        for (String scrollId : SableScrollDefs.SCROLLS) {
            DeferredHolder<Item, ? extends Item> holder = ITEMS.register(scrollId,
                    () -> new ItemSableScroll(new Item.Properties().stacksTo(64), scrollId));
            BY_ID.put(scrollId, holder);
        }
    }

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<SableScrollLootModifier>> SABLE_SCROLLS =
            LOOT_MODS.register("sable_scrolls", SableScrollLootModifier.CODEC);

    private SableScrollItems() {
    }

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
        LOOT_MODS.register(bus);
    }

    /** Докладывает все свитки Sable в обычную вкладку свитков MeowHex. */
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!MEOWHEX_SCROLLS_TAB.equals(event.getTabKey())) {
            return;
        }
        for (DeferredHolder<Item, ? extends Item> holder : BY_ID.values()) {
            event.accept(holder.get());
        }
    }

    /** Равномерно выбирает один id свитка (для лут-модификатора). */
    public static String randomScrollId(RandomSource random) {
        return SableScrollDefs.SCROLLS.get(random.nextInt(SableScrollDefs.SCROLLS.size()));
    }

    /** Равномерно выбирает один предмет-свиток (для лут-модификатора). */
    public static Item randomScroll(RandomSource random) {
        return BY_ID.get(randomScrollId(random)).get();
    }
}

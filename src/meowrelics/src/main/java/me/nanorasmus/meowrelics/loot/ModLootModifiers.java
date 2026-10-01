package me.nanorasmus.meowrelics.loot;

import com.mojang.serialization.MapCodec;
import me.nanorasmus.meowrelics.MeowRelics;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Регистрация кода нашего глобального модификатора лута. */
public final class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    MeowRelics.MOD_ID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<
            MapCodec<? extends IGlobalLootModifier>, MapCodec<BagLootModifier>> BAG_LOOT =
            CODECS.register("relic_bag_loot", () -> BagLootModifier.CODEC);

    public static void register(IEventBus modBus) {
        CODECS.register(modBus);
    }

    private ModLootModifiers() {
    }
}

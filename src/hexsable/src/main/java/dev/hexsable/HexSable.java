package dev.hexsable;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.common.lib.HexRegistries;
import com.mojang.logging.LogUtils;
import dev.hexsable.casting.HexSableActions;
import dev.hexsable.casting.SableRangeComponent;
import dev.hexsable.iota.SubLevelIota;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;

@Mod(HexSable.MOD_ID)
public final class HexSable {
    public static final String MOD_ID = "hexsable";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HexSable(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, HexSableConfig.SPEC);
        modEventBus.addListener(HexSable::onRegister);

        // Каждому новому CastingEnvironment цепляем расширение проверки дальности (блоки внутри структур).
        CastingEnvironment.addCreateEventListener(SableRangeComponent::attach);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void onRegister(RegisterEvent event) {
        event.register(HexRegistries.IOTA_TYPE, helper ->
                helper.register(id("sublevel"), SubLevelIota.TYPE));

        event.register(HexRegistries.ACTION, helper -> {
            for (HexSableActions.Entry e : HexSableActions.all()) {
                helper.register(id(e.path()), e.entry());
            }
        });
        LOGGER.info("Hex Sable Bridge: registered {} patterns", HexSableActions.all().size());
    }
}

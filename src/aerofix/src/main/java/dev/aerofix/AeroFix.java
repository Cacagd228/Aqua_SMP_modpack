package dev.aerofix;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Create Aeronautics Fix — набор патчей, которые ставятся ПОВЕРХ оригинального
 * bundle-мода Create Aeronautics 1.3.x и не трогают его файлы.
 *
 * <p>Все правки вносятся mixin'ами в рантайме, поэтому мод остаётся независимым от
 * внутренностей Simulated Project: если апстрим пересоберёт bundle, фикс продолжит
 * работать, пока публичные сигнатуры целевых методов не поменяются — иначе мод
 * аккуратно откажется грузиться, а не уронит игру.
 */
@Mod(AeroFix.MOD_ID)
public final class AeroFix {
    public static final String MOD_ID = "aerofix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AeroFix(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, AeroFixServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, AeroFixClientConfig.SPEC);
        LOGGER.info("Create Aeronautics Fix loaded");
    }
}

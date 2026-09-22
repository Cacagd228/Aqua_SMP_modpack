package com.fmm.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod("fmm_worldgen")
public class FMMWorldgen {
   public static final String MODID = "fmm_worldgen";
   public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS = DeferredRegister.create(
      Registries.DENSITY_FUNCTION_TYPE, "fmm_worldgen"
   );
   public static final Supplier<MapCodec<IslandMathFunction>> ISLAND_MATH = DENSITY_FUNCTIONS.register("island_math", () -> IslandMathFunction.CODEC);
   public static final Supplier<MapCodec<IslandTemperatureFunction>> ISLAND_TEMPERATURE = DENSITY_FUNCTIONS.register(
      "island_temperature", () -> IslandTemperatureFunction.CODEC
   );
   public static final Supplier<MapCodec<IslandOffsetFunction>> ISLAND_OFFSET = DENSITY_FUNCTIONS.register("island_offset", () -> IslandOffsetFunction.CODEC);

   public FMMWorldgen(IEventBus modEventBus, ModContainer modContainer) {
      DENSITY_FUNCTIONS.register(modEventBus);
      modContainer.registerConfig(Type.COMMON, WorldGenConfig.SPEC, "fmm_worldgen-common.toml");
      modEventBus.addListener(Loading.class, WorldGenConfig::onConfigLoad);
      modEventBus.addListener(Reloading.class, WorldGenConfig::onConfigLoad);
      NeoForge.EVENT_BUS.register(IslandDepositHandler.class);
   }
}

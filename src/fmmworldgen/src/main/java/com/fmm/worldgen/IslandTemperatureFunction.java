package com.fmm.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.ContextProvider;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.DensityFunction.Visitor;

public record IslandTemperatureFunction(DensityFunction baseTemperature) implements DensityFunction {
   public static final MapCodec<IslandTemperatureFunction> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("base_temperature").forGetter(IslandTemperatureFunction::baseTemperature))
         .apply(instance, IslandTemperatureFunction::new)
   );
   public static final KeyDispatchDataCodec<IslandTemperatureFunction> DATA_CODEC = KeyDispatchDataCodec.of(CODEC);

   public double compute(FunctionContext context) {
      IslandHelper.IslandSample sample = IslandHelper.sample(context.blockX(), context.blockZ());
      double distFromCoast = sample.distFromCoast();
      double warmOceanRadius = sample.warmOceanRadius();
      double ambient = this.baseTemperature.compute(context);
      if (distFromCoast <= 0.0) {
         return sample.islandTemperature();
      } else if (distFromCoast <= warmOceanRadius) {
         return sample.waterTemperature();
      } else if (distFromCoast <= warmOceanRadius + 25.0) {
         double t = (distFromCoast - warmOceanRadius) / 25.0;
         double cappedAmbient = Math.min(ambient, 0.45);
         return sample.waterTemperature() + (cappedAmbient - sample.waterTemperature()) * IslandHelper.smoothstep(t);
      } else {
         return Math.min(ambient, 0.45);
      }
   }

   public void fillArray(double[] array, ContextProvider provider) {
      provider.fillAllDirectly(array, this);
   }

   public DensityFunction mapAll(Visitor visitor) {
      return visitor.apply(new IslandTemperatureFunction(this.baseTemperature.mapAll(visitor)));
   }

   public double minValue() {
      return -1.0;
   }

   public double maxValue() {
      return 1.0;
   }

   public KeyDispatchDataCodec<? extends DensityFunction> codec() {
      return DATA_CODEC;
   }
}

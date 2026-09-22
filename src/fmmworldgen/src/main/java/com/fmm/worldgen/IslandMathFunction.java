package com.fmm.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.ContextProvider;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.DensityFunction.Visitor;

public record IslandMathFunction() implements DensityFunction {
   public static final MapCodec<IslandMathFunction> CODEC = MapCodec.unit(new IslandMathFunction());
   public static final KeyDispatchDataCodec<IslandMathFunction> DATA_CODEC = KeyDispatchDataCodec.of(CODEC);

   public double compute(FunctionContext context) {
      IslandHelper.IslandSample sample = IslandHelper.sample(context.blockX(), context.blockZ());
      double distFromCoast = sample.distFromCoast();
      double warmOceanRadius = sample.warmOceanRadius();
      double beachWidth = WorldGenConfig.CACHED_BEACH_WIDTH;
      if (distFromCoast <= 0.0) {
         double inwardDist = -distFromCoast;
         if (inwardDist <= beachWidth) {
            double t = inwardDist / beachWidth;
            return -0.16 + 0.04000000000000001 * IslandHelper.smoothstep(t);
         } else {
            double t = Math.clamp((inwardDist - beachWidth) / 25.0, 0.0, 1.0);
            double baseContinentalness = -0.12 + (sample.targetInlandCont() - -0.12) * IslandHelper.smoothstep(t);
            double macroVariation = 0.02
               * sample.terrainRoughness()
               * FastNoise2D.simplex2D(context.blockX() * 0.003, context.blockZ() * 0.003, sample.islandData().islandSeed());
            return baseContinentalness + t * macroVariation;
         }
      } else if (distFromCoast <= warmOceanRadius) {
         return Math.min(-0.28, -0.19);
      } else if (distFromCoast <= warmOceanRadius + 40.0) {
         double t = (distFromCoast - warmOceanRadius) / 40.0;
         double slope = -0.28 + -0.47 * IslandHelper.smoothstep(t);
         return Math.min(slope, -0.19);
      } else {
         return -0.75;
      }
   }

   public void fillArray(double[] array, ContextProvider provider) {
      provider.fillAllDirectly(array, this);
   }

   public DensityFunction mapAll(Visitor visitor) {
      return visitor.apply(this);
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

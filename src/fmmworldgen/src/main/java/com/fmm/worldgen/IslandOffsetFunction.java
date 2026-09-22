package com.fmm.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.ContextProvider;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.DensityFunction.Visitor;

public record IslandOffsetFunction() implements DensityFunction {
   public static final MapCodec<IslandOffsetFunction> CODEC = MapCodec.unit(new IslandOffsetFunction());
   public static final KeyDispatchDataCodec<IslandOffsetFunction> DATA_CODEC = KeyDispatchDataCodec.of(CODEC);

   public double compute(FunctionContext context) {
      IslandHelper.IslandSample sample = IslandHelper.sample(context.blockX(), context.blockZ());
      double distFromCoast = sample.distFromCoast();
      double warmOceanRadius = sample.warmOceanRadius();
      double shelfOffset = sample.shelfDepthOffset();
      double beachWidth = WorldGenConfig.CACHED_BEACH_WIDTH;
      if (distFromCoast <= 0.0) {
         double inwardDist = -distFromCoast;
         if (inwardDist <= beachWidth) {
            double t = inwardDist / beachWidth;
            return -0.25 + 0.01999999999999999 * IslandHelper.smoothstep(t);
         } else {
            double t = Math.clamp((inwardDist - beachWidth) / 25.0, 0.0, 1.0);
            double baseOffset = -0.23 + (sample.targetInlandOffset() - -0.23) * IslandHelper.smoothstep(t);
            double macroVariation = 0.02
               * sample.terrainRoughness()
               * FastNoise2D.simplex2D(context.blockX() * 0.003, context.blockZ() * 0.003, sample.islandData().islandSeed() + 1L);
            return baseOffset + t * macroVariation;
         }
      } else if (distFromCoast <= 6.0) {
         double t = distFromCoast / 6.0;
         return -0.25 + (shelfOffset - -0.25) * IslandHelper.smoothstep(t);
      } else if (distFromCoast <= warmOceanRadius) {
         return shelfOffset;
      } else if (distFromCoast <= warmOceanRadius + 40.0) {
         double t = (distFromCoast - warmOceanRadius) / 40.0;
         return shelfOffset + (-1.16 - shelfOffset) * IslandHelper.smoothstep(t);
      } else {
         return -1.16;
      }
   }

   public void fillArray(double[] array, ContextProvider provider) {
      provider.fillAllDirectly(array, this);
   }

   public DensityFunction mapAll(Visitor visitor) {
      return visitor.apply(this);
   }

   public double minValue() {
      return -2.0;
   }

   public double maxValue() {
      return 2.0;
   }

   public KeyDispatchDataCodec<? extends DensityFunction> codec() {
      return DATA_CODEC;
   }
}

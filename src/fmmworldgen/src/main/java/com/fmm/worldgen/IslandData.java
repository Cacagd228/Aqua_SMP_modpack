package com.fmm.worldgen;

public record IslandData(
   long zoneId,
   IslandArchetype islandType,
   IslandTier tier,
   double centerX,
   double centerZ,
   double baseRadius,
   double warmOceanRadius,
   double islandTemperature,
   double waterTemperature,
   long islandSeed
) {
}

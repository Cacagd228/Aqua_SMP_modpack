package com.fmm.worldgen;

import java.util.ArrayList;
import java.util.List;

public final class IslandHelper {
   public static final double DEEP_OCEAN_CONTINENTS = -0.75;
   public static final double WARM_OCEAN_CONTINENTS = -0.28;
   public static final double MAX_OCEAN_CONTINENTS = -0.19;
   public static final double MAX_DEEP_OCEAN_TEMP = 0.45;
   public static final double BEACH_WATERLINE_CONTINENTS = -0.16;
   public static final double BEACH_INLAND_CONTINENTS = -0.12;
   public static final double DEEP_OCEAN_OFFSET = -1.16;
   public static final double BEACH_WATERLINE_OFFSET = -0.25;
   public static final double BEACH_INLAND_OFFSET = -0.23;
   public static final double SLOPE_WIDTH = 40.0;

   private IslandHelper() {
   }

   public static IslandHelper.IslandSample sample(double x, double z) {
      List<IslandHelper.RawCandidate> candidates = collectCandidates(x, z);
      if (candidates.isEmpty()) {
         IslandData defaultSpawn = new IslandData(1L, IslandArchetype.TEMPERATE_FOREST_HILLS, IslandTier.SPAWN, 0.0, 0.0, 80.0, 70.0, 0.35, 0.5, 777L);
         return new IslandHelper.IslandSample(1000.0, 1.0, 60.0, -0.35, 0.24, -0.16, 0.06, 0.35, 0.5, defaultSpawn);
      } else {
         IslandHelper.RawCandidate best = candidates.get(0);

         for (int i = 1; i < candidates.size(); i++) {
            if (candidates.get(i).coastDist < best.coastDist) {
               best = candidates.get(i);
            }
         }

         List<IslandHelper.RawCandidate> cluster = new ArrayList<>();
         cluster.add(best);

         for (IslandHelper.RawCandidate other : candidates) {
            if (other != best) {
               double distCenters = Math.hypot(best.cx - other.cx, best.cz - other.cz);
               if (distCenters <= best.baseRadius + other.baseRadius + 48.0) {
                  cluster.add(other);
               }
            }
         }

         if (cluster.size() == 1) {
            IslandData island = new IslandData(
               best.zoneId, best.archetype, best.tier, best.cx, best.cz, best.baseRadius, best.warmOceanRadius, best.temp, best.waterTemp, best.seed
            );
            return new IslandHelper.IslandSample(
               best.coastDist,
               best.relDist,
               best.warmOceanRadius,
               best.shelfOffset,
               best.targetCont,
               best.targetOffset,
               best.roughness,
               best.temp,
               best.waterTemp,
               island
            );
         } else {
            double totalWeight = 0.0;
            double mergedCx = 0.0;
            double mergedCz = 0.0;
            long minZoneId = best.zoneId;
            IslandTier maxTier = best.tier;
            double maxWarmOcean = best.warmOceanRadius;
            double minCoastDist = best.coastDist;

            for (IslandHelper.RawCandidate member : cluster) {
               double w = member.baseRadius * member.baseRadius;
               totalWeight += w;
               mergedCx += member.cx * w;
               mergedCz += member.cz * w;
               if (member.zoneId < minZoneId) {
                  minZoneId = member.zoneId;
               }

               if (member.tier.ordinal() > maxTier.ordinal()) {
                  maxTier = member.tier;
               }

               if (member.warmOceanRadius > maxWarmOcean) {
                  maxWarmOcean = member.warmOceanRadius;
               }

               if (member.coastDist < minCoastDist) {
                  minCoastDist = member.coastDist;
               }
            }

            mergedCx /= totalWeight;
            mergedCz /= totalWeight;
            double maxSpan = 0.0;

            for (IslandHelper.RawCandidate member : cluster) {
               double d = Math.hypot(mergedCx - member.cx, mergedCz - member.cz) + member.baseRadius;
               if (d > maxSpan) {
                  maxSpan = d;
               }
            }

            IslandData mergedIsland = new IslandData(
               minZoneId, best.archetype, maxTier, mergedCx, mergedCz, maxSpan, maxWarmOcean, best.temp, best.waterTemp, best.seed
            );
            return new IslandHelper.IslandSample(
               minCoastDist,
               best.relDist,
               maxWarmOcean,
               best.shelfOffset,
               best.targetCont,
               best.targetOffset,
               best.roughness,
               best.temp,
               best.waterTemp,
               mergedIsland
            );
         }
      }
   }

   private static List<IslandHelper.RawCandidate> collectCandidates(double x, double z) {
      List<IslandHelper.RawCandidate> list = new ArrayList<>();
      double distSpawn = Math.sqrt(x * x + z * z);
      double angleSpawn = Math.atan2(z, x);
      double noiseSpawn = FastNoise2D.simplex2D(x * 0.015, z * 0.015, 777L);
      double shapeSpawn = 1.0 + 0.1 * Math.sin(2.0 * angleSpawn) + 0.08 * Math.cos(3.0 * angleSpawn) + 0.1 * noiseSpawn;
      double radiusSpawn = 80.0 * shapeSpawn;
      double spawnCoastDist = distSpawn - radiusSpawn;
      IslandHelper.RawCandidate spawn = new IslandHelper.RawCandidate();
      spawn.zoneId = 1L;
      spawn.archetype = IslandArchetype.TEMPERATE_FOREST_HILLS;
      spawn.tier = IslandTier.SPAWN;
      spawn.cx = 0.0;
      spawn.cz = 0.0;
      spawn.baseRadius = 80.0;
      spawn.warmOceanRadius = 70.0;
      spawn.shelfOffset = -0.34;
      spawn.targetCont = 0.24;
      spawn.targetOffset = -0.16;
      spawn.roughness = 0.06;
      spawn.temp = 0.35;
      spawn.waterTemp = 0.5;
      spawn.seed = 777L;
      spawn.coastDist = spawnCoastDist;
      spawn.relDist = distSpawn / radiusSpawn;
      list.add(spawn);
      collectTierCandidates(
         list,
         x,
         z,
         WorldGenConfig.CACHED_LARGE_GRID,
         WorldGenConfig.CACHED_LARGE_MIN_R,
         WorldGenConfig.CACHED_LARGE_MAX_R,
         WorldGenConfig.CACHED_LARGE_CHANCE,
         IslandTier.LARGE,
         9003L
      );
      collectTierCandidates(
         list,
         x,
         z,
         WorldGenConfig.CACHED_MEDIUM_GRID,
         WorldGenConfig.CACHED_MEDIUM_MIN_R,
         WorldGenConfig.CACHED_MEDIUM_MAX_R,
         WorldGenConfig.CACHED_MEDIUM_CHANCE,
         IslandTier.MEDIUM,
         8002L
      );
      collectTierCandidates(
         list,
         x,
         z,
         WorldGenConfig.CACHED_SMALL_GRID,
         WorldGenConfig.CACHED_SMALL_MIN_R,
         WorldGenConfig.CACHED_SMALL_MAX_R,
         WorldGenConfig.CACHED_SMALL_CHANCE,
         IslandTier.SMALL,
         7001L
      );
      return list;
   }

   private static void collectTierCandidates(
      List<IslandHelper.RawCandidate> list,
      double x,
      double z,
      double gridSize,
      double minRadius,
      double maxRadius,
      double spawnChance,
      IslandTier tier,
      long salt
   ) {
      long currentGridX = (long)Math.floor(x / gridSize);
      long currentGridZ = (long)Math.floor(z / gridSize);

      for (long dx = -1L; dx <= 1L; dx++) {
         for (long dz = -1L; dz <= 1L; dz++) {
            long cellX = currentGridX + dx;
            long cellZ = currentGridZ + dz;
            double spawnRoll = FastNoise2D.hash(cellX, cellZ, salt);
            if (!(spawnRoll > spawnChance) && (cellX != 0L || cellZ != 0L || !(Math.abs(x) < 450.0) || !(Math.abs(z) < 450.0))) {
               double randOffsetX = FastNoise2D.hash(cellX, cellZ, salt + 10L);
               double randOffsetZ = FastNoise2D.hash(cellX, cellZ, salt + 20L);
               double centerX = cellX * gridSize + gridSize * (0.12 + 0.76 * randOffsetX);
               double centerZ = cellZ * gridSize + gridSize * (0.12 + 0.76 * randOffsetZ);
               if (!(Math.hypot(centerX, centerZ) < 250.0)) {
                  double randRadius = FastNoise2D.hash(cellX, cellZ, salt + 30L);
                  double baseRadius = minRadius + (maxRadius - minRadius) * randRadius;
                  double minLagoon = WorldGenConfig.CACHED_WARM_OCEAN_MIN_R;
                  double maxLagoon = WorldGenConfig.CACHED_WARM_OCEAN_MAX_R;
                  double randWarmOcean = FastNoise2D.hash(cellX, cellZ, salt + 60L);
                  double rawLagoon = minLagoon + (maxLagoon - minLagoon) * randWarmOcean;
                  double islandWarmOceanRadius = Math.min(rawLagoon, baseRadius * 0.5);
                  double randDepth = FastNoise2D.hash(cellX, cellZ, salt + 70L);
                  double islandShelfOffset = -0.33 - 0.04 * randDepth;
                  double totalWeight = WorldGenConfig.CACHED_SNOWY_CHANCE
                     + WorldGenConfig.CACHED_COLD_CHANCE
                     + WorldGenConfig.CACHED_TEMPERATE_CHANCE
                     + WorldGenConfig.CACHED_WARM_CHANCE
                     + WorldGenConfig.CACHED_HOT_CHANCE;
                  if (totalWeight <= 0.0) {
                     totalWeight = 1.0;
                  }

                  double tSnowy = WorldGenConfig.CACHED_SNOWY_CHANCE / totalWeight;
                  double tCold = tSnowy + WorldGenConfig.CACHED_COLD_CHANCE / totalWeight;
                  double tTemperate = tCold + WorldGenConfig.CACHED_TEMPERATE_CHANCE / totalWeight;
                  double tWarm = tTemperate + WorldGenConfig.CACHED_WARM_CHANCE / totalWeight;
                  double climateRoll = FastNoise2D.hash(cellX, cellZ, salt + 150L);
                  double subRoll = FastNoise2D.hash(cellX, cellZ, salt + 160L);
                  double targetInlandCont;
                  double targetInlandOffset;
                  double terrainRoughness;
                  double islandTemperature;
                  double waterTemperature;
                  IslandArchetype archetype;
                  if (climateRoll < tSnowy) {
                     if (subRoll < 0.5) {
                        archetype = IslandArchetype.SNOWY_TAIGA;
                        targetInlandCont = 0.28;
                        targetInlandOffset = -0.14;
                        terrainRoughness = 0.08;
                        islandTemperature = -0.55;
                        waterTemperature = -0.6;
                     } else if (subRoll < 0.85) {
                        archetype = IslandArchetype.ALPINE_PEAKS;
                        targetInlandCont = 0.6;
                        targetInlandOffset = 0.15;
                        terrainRoughness = 0.18;
                        islandTemperature = -0.6;
                        waterTemperature = -0.65;
                     } else {
                        archetype = IslandArchetype.ICE_SPIKES;
                        targetInlandCont = 0.2;
                        targetInlandOffset = -0.18;
                        terrainRoughness = 0.05;
                        islandTemperature = -0.7;
                        waterTemperature = -0.7;
                     }
                  } else if (climateRoll < tCold) {
                     if (subRoll < 0.65) {
                        archetype = IslandArchetype.BOREAL_TAIGA;
                        targetInlandCont = 0.28 + 0.06 * FastNoise2D.hash(cellX, cellZ, salt + 130L);
                        targetInlandOffset = -0.13;
                        terrainRoughness = 0.07;
                        islandTemperature = -0.3;
                        waterTemperature = -0.3;
                     } else {
                        archetype = IslandArchetype.WINDSWEPT_CLIFFS;
                        targetInlandCont = 0.45 + 0.08 * FastNoise2D.hash(cellX, cellZ, salt + 130L);
                        targetInlandOffset = 0.05;
                        terrainRoughness = 0.15;
                        islandTemperature = -0.28;
                        waterTemperature = -0.3;
                     }
                  } else if (climateRoll < tTemperate) {
                     if (subRoll < 0.5) {
                        archetype = IslandArchetype.TEMPERATE_FOREST_HILLS;
                        targetInlandCont = 0.26 + 0.06 * FastNoise2D.hash(cellX, cellZ, salt + 130L);
                        targetInlandOffset = -0.14;
                        terrainRoughness = 0.07;
                        islandTemperature = 0.35;
                        waterTemperature = 0.35;
                     } else if (subRoll < 0.8) {
                        archetype = IslandArchetype.DARK_OAK_WOODS;
                        targetInlandCont = 0.32 + 0.06 * FastNoise2D.hash(cellX, cellZ, salt + 130L);
                        targetInlandOffset = -0.1;
                        terrainRoughness = 0.08;
                        islandTemperature = 0.3;
                        waterTemperature = 0.35;
                     } else {
                        archetype = IslandArchetype.GREEN_ALPS;
                        targetInlandCont = 0.56 + 0.1 * FastNoise2D.hash(cellX, cellZ, salt + 130L);
                        targetInlandOffset = 0.14;
                        terrainRoughness = 0.18;
                        islandTemperature = 0.28;
                        waterTemperature = 0.35;
                     }
                  } else if (climateRoll < tWarm) {
                     if (subRoll < 0.45) {
                        archetype = IslandArchetype.TROPICAL_JUNGLE;
                        targetInlandCont = 0.24 + 0.05 * FastNoise2D.hash(cellX, cellZ, salt + 130L);
                        targetInlandOffset = -0.16;
                        terrainRoughness = 0.06;
                        islandTemperature = 0.55;
                        waterTemperature = 0.75;
                     } else if (subRoll < 0.75) {
                        archetype = IslandArchetype.CHERRY_BLOSSOM_VALLEY;
                        targetInlandCont = 0.36 + 0.08 * FastNoise2D.hash(cellX, cellZ, salt + 130L);
                        targetInlandOffset = -0.06;
                        terrainRoughness = 0.1;
                        islandTemperature = 0.5;
                        waterTemperature = 0.75;
                     } else {
                        archetype = IslandArchetype.LUSH_FLOWER_PLAINS;
                        targetInlandCont = 0.18 + 0.04 * FastNoise2D.hash(cellX, cellZ, salt + 130L);
                        targetInlandOffset = -0.18;
                        terrainRoughness = 0.04;
                        islandTemperature = 0.55;
                        waterTemperature = 0.75;
                     }
                  } else if (subRoll < 0.45) {
                     archetype = IslandArchetype.ARID_SAVANNA;
                     targetInlandCont = 0.3 + 0.06 * FastNoise2D.hash(cellX, cellZ, salt + 130L);
                     targetInlandOffset = -0.1;
                     terrainRoughness = 0.06;
                     islandTemperature = 0.85;
                     waterTemperature = 0.85;
                  } else if (subRoll < 0.75) {
                     archetype = IslandArchetype.RED_DESERT_BADLANDS;
                     targetInlandCont = 0.42 + 0.08 * FastNoise2D.hash(cellX, cellZ, salt + 130L);
                     targetInlandOffset = 0.02;
                     terrainRoughness = 0.12;
                     islandTemperature = 0.9;
                     waterTemperature = 0.85;
                  } else {
                     archetype = IslandArchetype.MANGROVE_OASIS;
                     targetInlandCont = 0.16 + 0.04 * FastNoise2D.hash(cellX, cellZ, salt + 130L);
                     targetInlandOffset = -0.2;
                     terrainRoughness = 0.03;
                     islandTemperature = 0.8;
                     waterTemperature = 0.85;
                  }

                  double deltaX = x - centerX;
                  double deltaZ = z - centerZ;
                  double dist = Math.hypot(deltaX, deltaZ);
                  double angle = Math.atan2(deltaZ, deltaX);
                  double orientationAngle = FastNoise2D.hash(cellX, cellZ, salt + 80L) * Math.PI * 2.0;
                  double ovalFactor = 1.0 + 0.11 * Math.sin(2.0 * (angle + orientationAngle));
                  double bayFactor = 0.07 * Math.sin(3.0 * angle + salt) + 0.04 * Math.cos(5.0 * angle + salt * 2L);
                  double fineNoise = FastNoise2D.simplex2D((x - centerX) * 0.012, (z - centerZ) * 0.012, salt + 99L);
                  double coastlineNoise = 0.08 * fineNoise;
                  double shapeFactor = ovalFactor + bayFactor + coastlineNoise;
                  double effectiveRadius = baseRadius * shapeFactor;
                  double coastDist = dist - effectiveRadius;
                  long islandSeed = cellX * 3119L + cellZ * 7013L + salt;
                  long uniqueZoneId = Math.abs(cellX * 73856093L ^ cellZ * 19349663L ^ salt) % 900000L + 100000L;
                  IslandHelper.RawCandidate cand = new IslandHelper.RawCandidate();
                  cand.zoneId = uniqueZoneId;
                  cand.archetype = archetype;
                  cand.tier = tier;
                  cand.cx = centerX;
                  cand.cz = centerZ;
                  cand.baseRadius = baseRadius;
                  cand.warmOceanRadius = islandWarmOceanRadius;
                  cand.shelfOffset = islandShelfOffset;
                  cand.targetCont = targetInlandCont;
                  cand.targetOffset = targetInlandOffset;
                  cand.roughness = terrainRoughness;
                  cand.temp = islandTemperature;
                  cand.waterTemp = waterTemperature;
                  cand.seed = islandSeed;
                  cand.coastDist = coastDist;
                  cand.relDist = dist / effectiveRadius;
                  list.add(cand);
               }
            }
         }
      }
   }

   public static boolean isIslandChunk(int chunkX, int chunkZ) {
      double cx = chunkX * 16.0 + 8.0;
      double cz = chunkZ * 16.0 + 8.0;
      if (sample(cx, cz).distFromCoast() <= 16.0) {
         return true;
      } else if (sample(cx - 8.0, cz - 8.0).distFromCoast() <= 16.0) {
         return true;
      } else if (sample(cx + 8.0, cz + 8.0).distFromCoast() <= 16.0) {
         return true;
      } else {
         return sample(cx - 8.0, cz + 8.0).distFromCoast() <= 16.0 ? true : sample(cx + 8.0, cz - 8.0).distFromCoast() <= 16.0;
      }
   }

   public static IslandZoneInfo getZoneInfo(double x, double z) {
      IslandHelper.IslandSample sample = sample(x, z);
      IslandData island = sample.islandData();
      double dx = x - island.centerX();
      double dz = z - island.centerZ();
      double distFromCenter = Math.sqrt(dx * dx + dz * dz);
      double distFromCoast = sample.distFromCoast();
      double beachWidth = WorldGenConfig.CACHED_BEACH_WIDTH;
      int chunkX = (int)Math.floor(x / 16.0);
      int chunkZ = (int)Math.floor(z / 16.0);
      boolean inIslandChunk = isIslandChunk(chunkX, chunkZ);
      ZoneRegion region;
      if (inIslandChunk) {
         if (distFromCoast <= -beachWidth) {
            region = ZoneRegion.INLAND;
         } else {
            region = ZoneRegion.BEACH;
         }
      } else if (distFromCoast <= island.warmOceanRadius()) {
         if (island.islandType().isSnowy()) {
            region = ZoneRegion.COLD_LAGOON;
         } else if (island.islandType().isCold()) {
            region = ZoneRegion.COLD_LAGOON;
         } else {
            region = ZoneRegion.WARM_LAGOON;
         }
      } else {
         region = ZoneRegion.DEEP_OCEAN;
      }

      return new IslandZoneInfo(island, distFromCenter, distFromCoast, sample.relativeIslandDist(), region);
   }

   public static double smoothstep(double t) {
      double clamped = Math.clamp(t, 0.0, 1.0);
      return clamped * clamped * (3.0 - 2.0 * clamped);
   }

   public record IslandSample(
      double distFromCoast,
      double relativeIslandDist,
      double warmOceanRadius,
      double shelfDepthOffset,
      double targetInlandCont,
      double targetInlandOffset,
      double terrainRoughness,
      double islandTemperature,
      double waterTemperature,
      IslandData islandData
   ) {
   }

   public static final class RawCandidate {
      public long zoneId;
      public IslandArchetype archetype;
      public IslandTier tier;
      public double cx;
      public double cz;
      public double baseRadius;
      public double warmOceanRadius;
      public double shelfOffset;
      public double targetCont;
      public double targetOffset;
      public double roughness;
      public double temp;
      public double waterTemp;
      public long seed;
      public double coastDist;
      public double relDist;
   }
}

package com.fmm.worldgen;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;

public final class WorldGenConfig {
   public static final ModConfigSpec SPEC;
   public static final DoubleValue SMALL_GRID_SIZE;
   public static final DoubleValue SMALL_MIN_RADIUS;
   public static final DoubleValue SMALL_MAX_RADIUS;
   public static final DoubleValue SMALL_SPAWN_CHANCE;
   public static final DoubleValue MEDIUM_GRID_SIZE;
   public static final DoubleValue MEDIUM_MIN_RADIUS;
   public static final DoubleValue MEDIUM_MAX_RADIUS;
   public static final DoubleValue MEDIUM_SPAWN_CHANCE;
   public static final DoubleValue LARGE_GRID_SIZE;
   public static final DoubleValue LARGE_MIN_RADIUS;
   public static final DoubleValue LARGE_MAX_RADIUS;
   public static final DoubleValue LARGE_SPAWN_CHANCE;
   public static final DoubleValue HOT_ISLAND_CHANCE;
   public static final DoubleValue WARM_ISLAND_CHANCE;
   public static final DoubleValue TEMPERATE_ISLAND_CHANCE;
   public static final DoubleValue COLD_ISLAND_CHANCE;
   public static final DoubleValue SNOWY_ISLAND_CHANCE;
   public static final DoubleValue BEACH_WIDTH;
   public static final DoubleValue WARM_OCEAN_MIN_RADIUS;
   public static final DoubleValue WARM_OCEAN_MAX_RADIUS;
   public static final ModConfigSpec.BooleanValue ENABLE_ISLAND_DEPOSITS;
   public static final DoubleValue DEPOSIT_SIZE_MULTIPLIER;
   public static final ModConfigSpec.BooleanValue DEPOSIT_EXTRA_SPAWN_VEIN;
   public static volatile double CACHED_SMALL_GRID = 750.0;
   public static volatile double CACHED_SMALL_MIN_R = 75.0;
   public static volatile double CACHED_SMALL_MAX_R = 87.5;
   public static volatile double CACHED_SMALL_CHANCE = 0.4;
   public static volatile double CACHED_MEDIUM_GRID = 1400.0;
   public static volatile double CACHED_MEDIUM_MIN_R = 125.0;
   public static volatile double CACHED_MEDIUM_MAX_R = 175.0;
   public static volatile double CACHED_MEDIUM_CHANCE = 0.35;
   public static volatile double CACHED_LARGE_GRID = 2400.0;
   public static volatile double CACHED_LARGE_MIN_R = 187.5;
   public static volatile double CACHED_LARGE_MAX_R = 212.5;
   public static volatile double CACHED_LARGE_CHANCE = 0.3;
   public static volatile double CACHED_HOT_CHANCE = 0.15;
   public static volatile double CACHED_WARM_CHANCE = 0.25;
   public static volatile double CACHED_TEMPERATE_CHANCE = 0.3;
   public static volatile double CACHED_COLD_CHANCE = 0.15;
   public static volatile double CACHED_SNOWY_CHANCE = 0.15;
   public static volatile double CACHED_BEACH_WIDTH = 20.0;
   public static volatile double CACHED_WARM_OCEAN_MIN_R = 30.0;
   public static volatile double CACHED_WARM_OCEAN_MAX_R = 120.0;
   public static volatile boolean CACHED_DEPOSIT_ENABLED = true;
   public static volatile double CACHED_DEPOSIT_SIZE_MULT = 1.0;
   public static volatile boolean CACHED_DEPOSIT_EXTRA_SPAWN = true;

   public static void onConfigLoad(ModConfigEvent event) {
      if (event.getConfig().getSpec() == SPEC) {
         updateCache();
      }
   }

   public static void updateCache() {
      CACHED_SMALL_GRID = (Double)SMALL_GRID_SIZE.get();
      CACHED_SMALL_MIN_R = (Double)SMALL_MIN_RADIUS.get();
      CACHED_SMALL_MAX_R = (Double)SMALL_MAX_RADIUS.get();
      CACHED_SMALL_CHANCE = (Double)SMALL_SPAWN_CHANCE.get();
      CACHED_MEDIUM_GRID = (Double)MEDIUM_GRID_SIZE.get();
      CACHED_MEDIUM_MIN_R = (Double)MEDIUM_MIN_RADIUS.get();
      CACHED_MEDIUM_MAX_R = (Double)MEDIUM_MAX_RADIUS.get();
      CACHED_MEDIUM_CHANCE = (Double)MEDIUM_SPAWN_CHANCE.get();
      CACHED_LARGE_GRID = (Double)LARGE_GRID_SIZE.get();
      CACHED_LARGE_MIN_R = (Double)LARGE_MIN_RADIUS.get();
      CACHED_LARGE_MAX_R = (Double)LARGE_MAX_RADIUS.get();
      CACHED_LARGE_CHANCE = (Double)LARGE_SPAWN_CHANCE.get();
      CACHED_HOT_CHANCE = (Double)HOT_ISLAND_CHANCE.get();
      CACHED_WARM_CHANCE = (Double)WARM_ISLAND_CHANCE.get();
      CACHED_TEMPERATE_CHANCE = (Double)TEMPERATE_ISLAND_CHANCE.get();
      CACHED_COLD_CHANCE = (Double)COLD_ISLAND_CHANCE.get();
      CACHED_SNOWY_CHANCE = (Double)SNOWY_ISLAND_CHANCE.get();
      CACHED_BEACH_WIDTH = (Double)BEACH_WIDTH.get();
      CACHED_WARM_OCEAN_MIN_R = (Double)WARM_OCEAN_MIN_RADIUS.get();
      CACHED_WARM_OCEAN_MAX_R = (Double)WARM_OCEAN_MAX_RADIUS.get();
      CACHED_DEPOSIT_ENABLED = (Boolean)ENABLE_ISLAND_DEPOSITS.get();
      CACHED_DEPOSIT_SIZE_MULT = (Double)DEPOSIT_SIZE_MULTIPLIER.get();
      CACHED_DEPOSIT_EXTRA_SPAWN = (Boolean)DEPOSIT_EXTRA_SPAWN_VEIN.get();
   }

   static {
      Builder builder = new Builder();
      builder.comment(
            new String[]{
               "=====================================================",
               "      FMM Island World Generation Configuration      ",
               "====================================================="
            }
         )
         .push("general");
      builder.push("small_islands");
      builder.comment(
         new String[]{"Grid spacing (cell size in blocks) for small island distribution.", "Default: 750.0 (Spacious ocean distance between small islands)"}
      );
      SMALL_GRID_SIZE = builder.defineInRange("grid_size", 750.0, 64.0, 8192.0);
      builder.comment(new String[]{"Minimum radius for small islands (diameter = 2 * radius).", "Default: 75.0 (Min Diameter = 150 blocks)"});
      SMALL_MIN_RADIUS = builder.defineInRange("min_radius", 75.0, 10.0, 1000.0);
      builder.comment(new String[]{"Maximum radius for small islands (diameter = 2 * radius).", "Default: 87.5 (Max Diameter = 175 blocks)"});
      SMALL_MAX_RADIUS = builder.defineInRange("max_radius", 87.5, 10.0, 1000.0);
      builder.comment(new String[]{"Spawn probability of small islands per grid cell (0.0 to 1.0).", "Default: 0.40 (40% spawn chance per 750-block cell)"});
      SMALL_SPAWN_CHANCE = builder.defineInRange("spawn_chance", 0.4, 0.0, 1.0);
      builder.pop();
      builder.push("medium_islands");
      builder.comment(
         new String[]{"Grid spacing (cell size in blocks) for medium island distribution.", "Default: 1400.0 (Spacious ocean distance between medium islands)"}
      );
      MEDIUM_GRID_SIZE = builder.defineInRange("grid_size", 1400.0, 128.0, 8192.0);
      builder.comment(new String[]{"Minimum radius for medium islands (diameter = 2 * radius).", "Default: 125.0 (Min Diameter = 250 blocks)"});
      MEDIUM_MIN_RADIUS = builder.defineInRange("min_radius", 125.0, 20.0, 2000.0);
      builder.comment(new String[]{"Maximum radius for medium islands (diameter = 2 * radius).", "Default: 175.0 (Max Diameter = 350 blocks)"});
      MEDIUM_MAX_RADIUS = builder.defineInRange("max_radius", 175.0, 20.0, 2000.0);
      builder.comment(new String[]{"Spawn probability of medium islands per grid cell (0.0 to 1.0).", "Default: 0.35 (35% spawn chance per 1400-block cell)"});
      MEDIUM_SPAWN_CHANCE = builder.defineInRange("spawn_chance", 0.35, 0.0, 1.0);
      builder.pop();
      builder.push("large_islands");
      builder.comment(
         new String[]{"Grid spacing (cell size in blocks) for large island distribution.", "Default: 2400.0 (Spacious distance between grand continents)"}
      );
      LARGE_GRID_SIZE = builder.defineInRange("grid_size", 2400.0, 256.0, 16384.0);
      builder.comment(new String[]{"Minimum radius for large islands (diameter = 2 * radius).", "Default: 187.5 (Min Diameter = 375 blocks)"});
      LARGE_MIN_RADIUS = builder.defineInRange("min_radius", 187.5, 50.0, 4000.0);
      builder.comment(new String[]{"Maximum radius for large islands (diameter = 2 * radius).", "Default: 212.5 (Max Diameter = 425 blocks)"});
      LARGE_MAX_RADIUS = builder.defineInRange("max_radius", 212.5, 50.0, 4000.0);
      builder.comment(new String[]{"Spawn probability of large islands per grid cell (0.0 to 1.0).", "Default: 0.30 (30% spawn chance per 2400-block cell)"});
      LARGE_SPAWN_CHANCE = builder.defineInRange("spawn_chance", 0.3, 0.0, 1.0);
      builder.pop();
      builder.push("climate_and_coast");
      builder.comment(new String[]{"Spawn weight/chance for HOT islands (Savannas, Badlands Mesas, Mangrove Oases).", "Default: 0.15 (15%)"});
      HOT_ISLAND_CHANCE = builder.defineInRange("hot_island_chance", 0.15, 0.0, 1.0);
      builder.comment(new String[]{"Spawn weight/chance for WARM islands (Tropical Jungles, Cherry Blossom Hills, Sunflower Plains).", "Default: 0.25 (25%)"});
      WARM_ISLAND_CHANCE = builder.defineInRange("warm_island_chance", 0.25, 0.0, 1.0);
      builder.comment(
         new String[]{"Spawn weight/chance for TEMPERATE islands (Oak/Birch Forest Hills, Dark Oak Woods, Green Alpine Meadows).", "Default: 0.30 (30%)"}
      );
      TEMPERATE_ISLAND_CHANCE = builder.defineInRange("temperate_island_chance", 0.3, 0.0, 1.0);
      builder.comment(
         new String[]{"Spawn weight/chance for COLD islands (Boreal Pine & Spruce Taiga, Windswept Cliffs - NO SNOW on the ground).", "Default: 0.15 (15%)"}
      );
      COLD_ISLAND_CHANCE = builder.defineInRange("cold_island_chance", 0.15, 0.0, 1.0);
      builder.comment(
         new String[]{"Spawn weight/chance for SNOWY islands (Snowy Taiga, Alpine Frozen Peaks, Ice Spikes & Tundra - WINTER).", "Default: 0.15 (15%)"}
      );
      SNOWY_ISLAND_CHANCE = builder.defineInRange("snowy_island_chance", 0.15, 0.0, 1.0);
      builder.comment(new String[]{"Width of the sandy/snowy beach ring around all islands in blocks.", "Default: 20.0 blocks"});
      BEACH_WIDTH = builder.defineInRange("beach_width", 20.0, 4.0, 64.0);
      builder.comment(new String[]{"Minimum radius/width of the shallow reef lagoon (10-15 blocks depth) around islands.", "Default: 30.0 blocks"});
      WARM_OCEAN_MIN_RADIUS = builder.defineInRange("warm_ocean_min_radius", 30.0, 0.0, 500.0);
      builder.comment(new String[]{"Maximum radius/width of the shallow reef lagoon (10-15 blocks depth) around islands.", "Default: 120.0 blocks"});
      WARM_OCEAN_MAX_RADIUS = builder.defineInRange("warm_ocean_max_radius", 120.0, 10.0, 500.0);
      builder.pop();
      builder.push("island_deposits");
      builder.comment(
         new String[]{"Guaranteed Create: Rock & Stone ore vein on EVERY island (no exceptions).", "Vein size scales with island radius. Default: true"}
      );
      ENABLE_ISLAND_DEPOSITS = builder.define("enable_island_deposits", true);
      builder.comment(new String[]{"Scales the guaranteed vein size (radius multiplier).", "Default: 1.0"});
      DEPOSIT_SIZE_MULTIPLIER = builder.defineInRange("vein_size_multiplier", 1.0, 0.25, 4.0);
      builder.comment(
         new String[]{"Spawn island gets a second bonus vein (copper next to iron) for early progression.", "Default: true"}
      );
      DEPOSIT_EXTRA_SPAWN_VEIN = builder.define("extra_spawn_vein", true);
      builder.pop();
      builder.pop();
      SPEC = builder.build();
   }
}

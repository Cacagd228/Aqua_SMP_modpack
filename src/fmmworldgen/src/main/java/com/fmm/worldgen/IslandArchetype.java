package com.fmm.worldgen;

public enum IslandArchetype {
   ARID_SAVANNA("Arid Savanna & Plateaus", IslandClimate.HOT),
   RED_DESERT_BADLANDS("Red Badlands & Mesas", IslandClimate.HOT),
   MANGROVE_OASIS("Mangrove Swamp & Oasis", IslandClimate.HOT),
   TROPICAL_JUNGLE("Tropical Jungle Valleys", IslandClimate.WARM),
   CHERRY_BLOSSOM_VALLEY("Cherry Grove Hills", IslandClimate.WARM),
   LUSH_FLOWER_PLAINS("Lush Sunflower Plains", IslandClimate.WARM),
   TEMPERATE_FOREST_HILLS("Oak & Birch Forest Hills", IslandClimate.TEMPERATE),
   DARK_OAK_WOODS("Dark Forest & Shrublands", IslandClimate.TEMPERATE),
   GREEN_ALPS("Alpine Meadows & Green Peaks", IslandClimate.TEMPERATE),
   BOREAL_TAIGA("Boreal Pine & Spruce Taiga", IslandClimate.COLD),
   WINDSWEPT_CLIFFS("Windswept Stony Cliffs", IslandClimate.COLD),
   SNOWY_TAIGA("Snowy Taiga", IslandClimate.SNOWY),
   ALPINE_PEAKS("Alpine Frozen Peaks", IslandClimate.SNOWY),
   ICE_SPIKES("Ice Spikes & Tundra", IslandClimate.SNOWY);

   private final String displayName;
   private final IslandClimate climate;

   private IslandArchetype(String displayName, IslandClimate climate) {
      this.displayName = displayName;
      this.climate = climate;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public IslandClimate getClimate() {
      return this.climate;
   }

   public boolean isHot() {
      return this.climate.isHot();
   }

   public boolean isWarm() {
      return this.climate.isWarm();
   }

   public boolean isTemperate() {
      return this.climate.isTemperate();
   }

   public boolean isCold() {
      return this.climate.isCold();
   }

   public boolean isSnowy() {
      return this.climate.isSnowy();
   }
}

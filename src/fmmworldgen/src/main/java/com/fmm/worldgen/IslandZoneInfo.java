package com.fmm.worldgen;

public record IslandZoneInfo(IslandData island, double distFromCenter, double distFromCoast, double relativeDist, ZoneRegion region) {
   public boolean isLand() {
      return this.region.isIsland();
   }

   public boolean isWater() {
      return this.region.isOcean();
   }
}

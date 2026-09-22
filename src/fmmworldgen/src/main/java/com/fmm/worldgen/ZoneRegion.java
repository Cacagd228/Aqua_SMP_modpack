package com.fmm.worldgen;

public enum ZoneRegion {
   INLAND("Inland Landmass"),
   BEACH("Sandy/Snowy Beach"),
   WARM_LAGOON("Warm Coral Lagoon"),
   COLD_LAGOON("Cold Winter Waters"),
   DEEP_OCEAN("Deep Ocean Abyss");

   private final String displayName;

   private ZoneRegion(String displayName) {
      this.displayName = displayName;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public boolean isIsland() {
      return this == INLAND || this == BEACH;
   }

   public boolean isOcean() {
      return this == WARM_LAGOON || this == COLD_LAGOON || this == DEEP_OCEAN;
   }
}

package com.fmm.worldgen;

public enum IslandClimate {
   HOT("Hot / Arid", true, false, false, false, false),
   WARM("Warm / Tropical", false, true, false, false, false),
   TEMPERATE("Temperate / Forested", false, false, true, false, false),
   COLD("Cold / Taiga (No Snow)", false, false, false, true, false),
   SNOWY("Snowy / Frozen (Winter)", false, false, false, false, true);

   private final String displayName;
   private final boolean isHot;
   private final boolean isWarm;
   private final boolean isTemperate;
   private final boolean isCold;
   private final boolean isSnowy;

   private IslandClimate(String displayName, boolean isHot, boolean isWarm, boolean isTemperate, boolean isCold, boolean isSnowy) {
      this.displayName = displayName;
      this.isHot = isHot;
      this.isWarm = isWarm;
      this.isTemperate = isTemperate;
      this.isCold = isCold;
      this.isSnowy = isSnowy;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public boolean isHot() {
      return this.isHot;
   }

   public boolean isWarm() {
      return this.isWarm;
   }

   public boolean isTemperate() {
      return this.isTemperate;
   }

   public boolean isCold() {
      return this.isCold;
   }

   public boolean isSnowy() {
      return this.isSnowy;
   }
}

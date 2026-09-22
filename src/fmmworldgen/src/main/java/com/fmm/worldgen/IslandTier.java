package com.fmm.worldgen;

public enum IslandTier {
   SPAWN("Spawn", 80.0, 0),
   SMALL("Small", 81.25, 1),
   MEDIUM("Medium", 150.0, 3),
   LARGE("Large", 200.0, 7);

   private final String displayName;
   private final double typicalRadius;
   private final int cost;

   private IslandTier(String displayName, double typicalRadius, int cost) {
      this.displayName = displayName;
      this.typicalRadius = typicalRadius;
      this.cost = cost;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public double getTypicalRadius() {
      return this.typicalRadius;
   }

   public int getCost() {
      return cost;
   }
}

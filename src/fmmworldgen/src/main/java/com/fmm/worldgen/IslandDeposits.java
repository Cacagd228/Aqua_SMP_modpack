package com.fmm.worldgen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Deterministic per-island Create: Rock &amp; Stone ore veins.
 *
 * <p>Every island described by {@link IslandHelper} gets exactly one guaranteed
 * vein (spawn island optionally gets a second bonus vein). The vein anchor,
 * ore type and size are pure functions of the island data, so all clients and
 * the server agree without any extra sync:
 * <ul>
 *   <li>anchor: near the island center, refined to {@link ZoneRegion#INLAND};</li>
 *   <li>ore type: deterministic pick among enabled overworld RNS deposits;</li>
 *   <li>size: scales with island base radius (bigger island = bigger vein).</li>
 * </ul>
 */
public final class IslandDeposits {
   public static final String RNS_NAMESPACE = "create_rns";
   public static final ResourceLocation DEPOSIT_BLOCKS_TAG_ID =
      ResourceLocation.fromNamespaceAndPath(RNS_NAMESPACE, "deposit_blocks");

   /** Depth of the vein top below the surface (matches RNS overworld presets). */
   public static final int VEIN_DEPTH_BELOW_SURFACE = 4;
   /** Radius (blocks) used to adopt an already generated vein instead of placing a new one. */
   public static final int ADOPT_SCAN_MARGIN = 3;
   /** Minimum deposit blocks that count as "vein already exists". */
   public static final int ADOPT_MIN_BLOCKS = 5;
   /** Radius (blocks) to look for an already registered custom deposit. */
   public static final double REGISTERED_LOOKUP_RADIUS = 96.0;

   private IslandDeposits() {
   }

   /** Deposit type: RNS structure key (for the scanner) + deposit block (for placement). */
   public record DepositChoice(ResourceKey<Structure> structureKey, Block block) {
   }

   /** Fully resolved vein plan for one island. */
   public record VeinPlan(DepositChoice deposit, int anchorX, int anchorZ, int rx, int ry, int rz, long veinSeed) {
   }

   // ------------------------------------------------------------------
   // Deterministic helpers (no level access, safe to call anywhere)
   // ------------------------------------------------------------------

   /** SplitMix64 mixer for deterministic hashes. */
   public static long mix64(long z) {
      z += 0x9E3779B97F4A7C15L;
      z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
      z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
      return z ^ (z >>> 31);
   }

   private static boolean isInland(int x, int z) {
      return isInlandXZ(x, z);
   }

   /** True if the block column is inland (under land, not beach or ocean). */
   public static boolean isInlandXZ(int x, int z) {
      try {
         return IslandHelper.getZoneInfo(x + 0.5, z + 0.5).region() == ZoneRegion.INLAND;
      } catch (Exception e) {
         return false;
      }
   }

   /**
    * Shifts the anchor inside its own chunk so the whole vein (plus a
    * one-block margin) fits into already loaded blocks. This is required:
    * touching blocks outside the currently loading chunk can deadlock the
    * server thread. Prefers an inland point, falls back to the clamped
    * point even under beach (a vein is still guaranteed).
    */
   public static int[] fitAnchorInChunk(int anchorX, int anchorZ, int margin) {
      int m = Math.clamp(margin, 0, 7);
      int chunkMinX = (anchorX >> 4) << 4;
      int chunkMinZ = (anchorZ >> 4) << 4;
      int loX = chunkMinX + m;
      int hiX = chunkMinX + 15 - m;
      int loZ = chunkMinZ + m;
      int hiZ = chunkMinZ + 15 - m;
      if (hiX < loX || hiZ < loZ) {
         return new int[]{anchorX, anchorZ};
      }
      int ax = Math.min(Math.max(anchorX, loX), hiX);
      int az = Math.min(Math.max(anchorZ, loZ), hiZ);
      if (ax != anchorX || az != anchorZ) {
         if (isInlandXZ(ax, az)) {
            return new int[]{ax, az};
         }
         // Inland not available at the clamped point: still stay in-chunk
         // (deadlock safety wins over perfect placement).
         return new int[]{ax, az};
      }
      return new int[]{anchorX, anchorZ};
   }

   /**
    * Anchor (x, z) of the guaranteed vein: deterministic offset near the
    * island center, refined to inland so the vein sits under land.
    */
   public static int[] computeAnchorXZ(IslandData island) {
      double cx = island.centerX();
      double cz = island.centerZ();
      double r = island.baseRadius();
      long h = mix64(island.zoneId() * 31L + island.islandSeed() * 0x100000001L);
      double angle = (double) (h >>> 11) * (Math.PI * 2.0 / (double) (1L << 53));
      double frac = (double) Long.remainderUnsigned(h >>> 22, 1000L) / 1000.0 * 0.35;
      double dist = frac * r;
      int bx = (int) Math.round(cx + Math.cos(angle) * dist);
      int bz = (int) Math.round(cz + Math.sin(angle) * dist);
      if (isInland(bx, bz)) {
         return new int[]{bx, bz};
      }
      double maxRing = Math.max(16.0, r * 0.6);
      for (double ring = 8.0; ring <= maxRing; ring += 8.0) {
         for (int k = 0; k < 8; k++) {
            double a = angle + k * Math.PI / 4.0;
            int x = (int) Math.round(cx + Math.cos(a) * Math.min(ring, r * 0.6));
            int z = (int) Math.round(cz + Math.sin(a) * Math.min(ring, r * 0.6));
            if (isInland(x, z)) {
               return new int[]{x, z};
            }
         }
      }
      return new int[]{bx, bz};
   }

   /**
    * Vein radii from the island base radius. Small islands (~75-87) get
    * rx=2/ry=2, medium (~125-175) get rx=4/ry=3, large (~187-212) get rx=6/ry=4.
    */
   public static int[] computeRadii(double baseRadius) {
      double t = Math.clamp((baseRadius - 75.0) / (212.5 - 75.0), 0.0, 1.0);
      double mult = WorldGenConfig.CACHED_DEPOSIT_SIZE_MULT;
      if (mult <= 0.0) {
         mult = 1.0;
      }
      int rx = Math.max(2, (int) Math.round((2.0 + 4.0 * t) * mult));
      int ry = Math.max(2, (int) Math.round((2.0 + 2.0 * t) * mult));
      return new int[]{rx, ry, rx};
   }

   // ------------------------------------------------------------------
   // Registry lookups (server only)
   // ------------------------------------------------------------------

   /**
    * All enabled overworld RNS deposits, sorted by id for determinism.
    * Structure {@code create_rns:deposit_X} is paired with block
    * {@code create_rns:X_deposit_block}.
    */
   public static List<DepositChoice> overworldDeposits(ServerLevel level) {
      List<DepositChoice> out = new ArrayList<>();
      Registry<Structure> structReg;
      Registry<Block> blockReg;
      try {
         structReg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
         blockReg = level.registryAccess().registryOrThrow(Registries.BLOCK);
      } catch (Exception e) {
         return out;
      }
      TagKey<Block> depositTag = TagKey.create(Registries.BLOCK, DEPOSIT_BLOCKS_TAG_ID);
      List<ResourceLocation> ids = new ArrayList<>(structReg.keySet());
      ids.sort(Comparator.comparing(ResourceLocation::toString));
      for (ResourceLocation id : ids) {
         if (!RNS_NAMESPACE.equals(id.getNamespace())) {
            continue;
         }
         String path = id.getPath();
         if (!path.startsWith("deposit_") || path.startsWith("deposit_nether_")) {
            continue;
         }
         ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, id);
         String keyword = path.substring("deposit_".length());
         ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(RNS_NAMESPACE, keyword + "_deposit_block");
         ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, blockId);
         if (!blockReg.containsKey(blockKey)) {
            continue;
         }
         Block block;
         try {
            block = blockReg.getOrThrow(blockKey);
            Holder.Reference<Block> holder = blockReg.getHolderOrThrow(blockKey);
            if (!holder.is(depositTag)) {
               continue;
            }
         } catch (Exception e) {
            continue;
         }
         out.add(new DepositChoice(key, block));
      }
      return out;
   }

   /** Deterministic pick of a deposit for an island. */
   public static DepositChoice pickDeposit(List<DepositChoice> list, IslandData island, int salt) {
      if (list.isEmpty()) {
         return null;
      }
      long h = mix64(island.zoneId() * 0x9E3779B97F4A7C15L + island.islandSeed() + salt * 0xD1B54A32D192ED03L);
      int idx = (int) Long.remainderUnsigned(h, (long) list.size());
      return list.get(idx);
   }

   /** Find a deposit by keyword (e.g. "iron"), or null. */
   public static DepositChoice findByKeyword(List<DepositChoice> list, String keyword) {
      String want = "deposit_" + keyword;
      for (DepositChoice c : list) {
         if (c.structureKey().location().getPath().equals(want)) {
            return c;
         }
      }
      return null;
   }

   // ------------------------------------------------------------------
   // Vein plans
   // ------------------------------------------------------------------

   /**
    * One guaranteed plan per island; spawn island gets a second bonus plan
    * (copper next to iron) when enabled in the config.
    */
   public static List<VeinPlan> plansForIsland(IslandData island, List<DepositChoice> deposits) {
      List<VeinPlan> plans = new ArrayList<>();
      if (deposits.isEmpty()) {
         return plans;
      }
      boolean isSpawn = island.tier() == IslandTier.SPAWN || island.zoneId() == 1L;
      int[] anchor = computeAnchorXZ(island);
      int[] radii = computeRadii(island.baseRadius());
      long veinSeed = mix64(island.zoneId() * 0xC2B2AE3D27D4EB4FL + island.islandSeed());

      if (isSpawn && WorldGenConfig.CACHED_DEPOSIT_EXTRA_SPAWN) {
         DepositChoice iron = findByKeyword(deposits, "iron");
         DepositChoice copper = findByKeyword(deposits, "copper");
         if (iron == null) {
            iron = pickDeposit(deposits, island, 11);
         }
         if (copper == null || copper.equals(iron)) {
            copper = pickDeposit(deposits, island, 12);
            if (copper != null && copper.equals(iron)) {
               copper = deposits.size() > 1 ? deposits.get((deposits.indexOf(iron) + 1) % deposits.size()) : null;
            }
         }
         if (iron != null) {
            plans.add(new VeinPlan(iron, anchor[0], anchor[1], radii[0], radii[1], radii[2], veinSeed));
         }
         if (copper != null) {
            int[] anchor2 = mirrorAnchor(island, anchor);
            plans.add(new VeinPlan(copper, anchor2[0], anchor2[1], radii[0], radii[1], radii[2], mix64(veinSeed + 1L)));
         }
         return plans;
      }

      DepositChoice main = pickDeposit(deposits, island, 0);
      if (main != null) {
         plans.add(new VeinPlan(main, anchor[0], anchor[1], radii[0], radii[1], radii[2], veinSeed));
      }
      return plans;
   }

   /** Second anchor for the spawn bonus vein: mirrored through the center. */
   private static int[] mirrorAnchor(IslandData island, int[] anchor) {
      int mx = (int) Math.round(2.0 * island.centerX() - anchor[0]);
      int mz = (int) Math.round(2.0 * island.centerZ() - anchor[1]);
      long dx = (long) mx - anchor[0];
      long dz = (long) mz - anchor[1];
      if (dx * dx + dz * dz < 36L) {
         mx = anchor[0] + 12;
         mz = anchor[1];
      }
      if (isInland(mx, mz)) {
         return new int[]{mx, mz};
      }
      return new int[]{anchor[0], anchor[1]};
   }

   // ------------------------------------------------------------------
   // Placement (server only)
   // ------------------------------------------------------------------

   /**
    * Underground Y for the vein center: a few blocks below the surface.
    * Reads the heightmap of the given (already loaded) chunk only, so it
    * can never trigger chunk loading (deadlock safety).
    */
   public static int computeVeinY(ServerLevel level, LevelChunk chunk, int x, int z, int ry) {
      int surface;
      try {
         surface = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
      } catch (Exception e) {
         return level.getMinBuildHeight() + ry + 2;
      }
      int y = surface - VEIN_DEPTH_BELOW_SURFACE - ry;
      int minY = level.getMinBuildHeight() + ry + 2;
      if (y < minY) {
         y = minY;
      }
      int maxY = level.getMaxBuildHeight() - ry - 1;
      if (y > maxY) {
         y = maxY;
      }
      return y;
   }

   /**
    * Places an ellipsoid vein of deposit blocks. Only replaces solid
    * non-fluid blocks (never air, fluids, bedrock or existing deposits).
    * Only touches blocks inside {@code chunkX/chunkZ}: accessing other
    * chunks here can deadlock the server thread during chunk loading.
    *
    * @return number of placed blocks
    */
   public static int placeVein(
      ServerLevel level, BlockPos center, Block block, int rx, int ry, int rz, long veinSeed, int chunkX, int chunkZ
   ) {
      BlockState deposit = block.defaultBlockState();
      TagKey<Block> depositTag = TagKey.create(Registries.BLOCK, DEPOSIT_BLOCKS_TAG_ID);
      int placed = 0;
      for (int dx = -rx; dx <= rx; dx++) {
         for (int dy = -ry; dy <= ry; dy++) {
            for (int dz = -rz; dz <= rz; dz++) {
               double q = (double) dx * dx / (rx * rx) + (double) dy * dy / (ry * ry) + (double) dz * dz / (rz * rz);
               if (q > 1.0) {
                  continue;
               }
               long h = mix64(veinSeed + dx * 0x8DA6B343L + dy * 0xD8163841L + dz * 0xCB1AB31FL);
               if (Long.remainderUnsigned(h >>> 8, 100L) < 12L) {
                  continue;
               }
               BlockPos p = center.offset(dx, dy, dz);
               if (p.getY() < level.getMinBuildHeight() || p.getY() >= level.getMaxBuildHeight()) {
                  continue;
               }
               if ((p.getX() >> 4) != chunkX || (p.getZ() >> 4) != chunkZ) {
                  continue;
               }
               BlockState cur;
               try {
                  cur = level.getBlockState(p);
               } catch (Exception e) {
                  continue;
               }
               if (cur.isAir()) {
                  continue;
               }
               if (!cur.getFluidState().isEmpty()) {
                  continue;
               }
               Block curBlock = cur.getBlock();
               if (curBlock == Blocks.BEDROCK) {
                  continue;
               }
               if (curBlock == block) {
                  continue;
               }
               try {
                  if (cur.is(depositTag)) {
                     continue;
                  }
               } catch (Exception e) {
                  // tag not loaded; fall through
               }
               try {
                  if (level.setBlock(p, deposit, 3)) {
                     placed++;
                  }
               } catch (Exception e) {
                  // read-only level or out of bounds; skip
               }
            }
         }
      }
      return placed;
   }

   /** Counts RNS deposit blocks in a cube around the center, inside one chunk only. */
   public static int countNearbyDepositBlocks(ServerLevel level, BlockPos center, int radius, int chunkX, int chunkZ) {
      TagKey<Block> depositTag = TagKey.create(Registries.BLOCK, DEPOSIT_BLOCKS_TAG_ID);
      int count = 0;
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dy = -radius; dy <= radius; dy++) {
            for (int dz = -radius; dz <= radius; dz++) {
               BlockPos p = center.offset(dx, dy, dz);
               if (p.getY() < level.getMinBuildHeight() || p.getY() >= level.getMaxBuildHeight()) {
                  continue;
               }
               if ((p.getX() >> 4) != chunkX || (p.getZ() >> 4) != chunkZ) {
                  continue;
               }
               try {
                  if (level.getBlockState(p).is(depositTag)) {
                     count++;
                  }
               } catch (Exception e) {
                  // ignore
               }
            }
         }
      }
      return count;
   }

   /** True if RNS deposit blocks exist at all (i.e. the mod is installed). */
   public static boolean rnsBlocksPresent(ServerLevel level) {
      try {
         Registry<Block> blockReg = level.registryAccess().registryOrThrow(Registries.BLOCK);
         for (ResourceLocation id : blockReg.keySet()) {
            if (RNS_NAMESPACE.equals(id.getNamespace()) && id.getPath().endsWith("_deposit_block")) {
               return true;
            }
         }
      } catch (Exception e) {
         return false;
      }
      return BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath(RNS_NAMESPACE, "iron_deposit_block"));
   }
}

package com.fmm.worldgen;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Places the guaranteed Create: Rock &amp; Stone vein on every island.
 *
 * <p>When the chunk holding an island's vein anchor loads on the server, the
 * vein is placed underground (size scales with island radius) and registered
 * as an RNS custom deposit so the deposit scanner finds it. Registration is
 * persistent (stored in RNS level data), so each island is processed exactly
 * once: already processed islands are skipped, which also means mined veins
 * are never restored.
 */
public final class IslandDepositHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static volatile List<IslandDeposits.DepositChoice> cachedDeposits;
   private static volatile long cachedAtMs;

   private IslandDepositHandler() {
   }

   @SubscribeEvent
   public static void onChunkLoad(ChunkEvent.Load event) {
      try {
         handleChunkLoad(event);
      } catch (Throwable t) {
         // Never break chunk loading because of deposits.
      }
   }

   private static void handleChunkLoad(ChunkEvent.Load event) {
      if (!(event.getLevel() instanceof ServerLevel level)) {
         return;
      }
      if (!level.dimension().equals(Level.OVERWORLD)) {
         return;
      }
      if (!WorldGenConfig.CACHED_DEPOSIT_ENABLED) {
         return;
      }
      if (!(event.getChunk() instanceof LevelChunk chunk)) {
         return;
      }
      ChunkPos cp = chunk.getPos();

      IslandData island;
      try {
         island = IslandHelper.sample(cp.getMinBlockX() + 8.0, cp.getMinBlockZ() + 8.0).islandData();
      } catch (Exception e) {
         return;
      }
      if (island == null) {
         return;
      }

      List<IslandDeposits.DepositChoice> deposits = deposits(level);
      if (deposits.isEmpty()) {
         return;
      }

      List<IslandDeposits.VeinPlan> plans;
      try {
         plans = IslandDeposits.plansForIsland(island, deposits);
      } catch (Exception e) {
         return;
      }
      for (IslandDeposits.VeinPlan plan : plans) {
         try {
            if ((plan.anchorX() >> 4) != cp.x || (plan.anchorZ() >> 4) != cp.z) {
               continue;
            }
            ensureVein(level, chunk, cp, plan);
         } catch (Exception e) {
            // skip this plan, keep loading the chunk
         }
      }
   }

   private static void ensureVein(ServerLevel level, LevelChunk chunk, ChunkPos cp, IslandDeposits.VeinPlan plan) {
      int[] fitted = IslandDeposits.fitAnchorInChunk(plan.anchorX(), plan.anchorZ(), plan.rx() + 1);
      int y = IslandDeposits.computeVeinY(level, chunk, fitted[0], fitted[1], plan.ry());
      BlockPos center = new BlockPos(fitted[0], y, fitted[1]);

      if (RnsBridge.hasCustomDepositNear(
         level, plan.deposit().structureKey(), center, IslandDeposits.REGISTERED_LOOKUP_RADIUS
      )) {
         return;
      }

      int scanR = Math.max(plan.rx(), Math.max(plan.ry(), plan.rz())) + IslandDeposits.ADOPT_SCAN_MARGIN;
      int existing = IslandDeposits.countNearbyDepositBlocks(level, center, scanR, cp.x, cp.z);
      if (existing >= IslandDeposits.ADOPT_MIN_BLOCKS) {
         RnsBridge.registerCustomDeposit(level, plan.deposit().structureKey(), center);
         return;
      }

      int placed = IslandDeposits.placeVein(
         level, center, plan.deposit().block(), plan.rx(), plan.ry(), plan.rz(), plan.veinSeed(), cp.x, cp.z
      );
      if (placed > 0) {
         RnsBridge.registerCustomDeposit(level, plan.deposit().structureKey(), center);
         LOGGER.info(
            "[FMMWorldgen] Island vein: {} rx{} at {} ({} blocks)",
            plan.deposit().structureKey().location(), plan.rx(), center, placed
         );
      }
   }

   /** Cached (60s) list of enabled overworld RNS deposits. */
   private static List<IslandDeposits.DepositChoice> deposits(ServerLevel level) {
      long now = System.currentTimeMillis();
      List<IslandDeposits.DepositChoice> cached = cachedDeposits;
      if (cached != null && now - cachedAtMs < 60_000L) {
         return cached;
      }
      List<IslandDeposits.DepositChoice> fresh;
      try {
         if (!IslandDeposits.rnsBlocksPresent(level)) {
            fresh = List.of();
         } else {
            fresh = IslandDeposits.overworldDeposits(level);
         }
      } catch (Exception e) {
         return cached != null ? cached : List.of();
      }
      cachedDeposits = fresh;
      cachedAtMs = now;
      return fresh;
   }
}

package com.fmm.worldgen.client;

import com.fmm.worldgen.IslandData;
import com.fmm.worldgen.IslandHelper;
import com.fmm.worldgen.IslandZoneInfo;
import com.fmm.worldgen.ZoneRegion;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.DebugText;

@EventBusSubscriber(modid = "fmm_worldgen", value = Dist.CLIENT)
public final class IslandDebugOverlay {
   @SubscribeEvent
   public static void onDebugText(DebugText event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         LocalPlayer player = mc.player;
         double px = player.getX();
         double pz = player.getZ();
         IslandZoneInfo info = IslandHelper.getZoneInfo(px, pz);
         IslandData island = info.island();
         List<String> left = event.getLeft();
         left.add("");
         if (info.region().isIsland()) {
            left.add(
               String.format(
                  "[FMM WorldGen] Island #%d [%s | %s | %s] Center: [%.0f, %.0f]",
                  island.zoneId(),
                  island.islandType().name(),
                  island.islandType().getClimate().name(),
                  island.tier().name(),
                  island.centerX(),
                  island.centerZ()
               )
            );
            left.add(
               String.format(
                  "[FMM WorldGen] Zone: %s | DistCenter: %.1fm | Coast: %.1fm (Radius: %.0fm)",
                  info.region().getDisplayName(),
                  info.distFromCenter(),
                  info.distFromCoast(),
                  island.baseRadius()
               )
            );
         } else if (info.region() != ZoneRegion.WARM_LAGOON && info.region() != ZoneRegion.COLD_LAGOON) {
            left.add("[FMM WorldGen] Ocean: Deep Ocean Abyss (Seabed: Y=-20, Depth: ~116m)");
            left.add(String.format("[FMM WorldGen] Open Waters | Distance to Nearest Island: %.1fm", info.distFromCoast()));
         } else {
            left.add(String.format("[FMM WorldGen] Ocean: %s (Depth: 10-15m)", info.region().getDisplayName()));
            left.add(String.format("[FMM WorldGen] Distance to Island Coast: %.1fm", info.distFromCoast()));
         }
      }
   }
}

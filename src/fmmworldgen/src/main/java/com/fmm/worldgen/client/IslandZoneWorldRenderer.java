package com.fmm.worldgen.client;

import com.fmm.worldgen.IslandClimate;
import com.fmm.worldgen.IslandData;
import com.fmm.worldgen.IslandHelper;
import com.fmm.worldgen.IslandZoneInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = "fmm_worldgen", value = Dist.CLIENT)
public final class IslandZoneWorldRenderer {
   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         if (IslandClientHandler.showIslandBorders) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
               LocalPlayer player = mc.player;
               double px = player.getX();
               double pz = player.getZ();
               IslandZoneInfo info = IslandHelper.getZoneInfo(px, pz);
               IslandData island = info.island();
               double dx = px - island.centerX();
               double dz = pz - island.centerZ();
               double distSq = dx * dx + dz * dz;
               double maxRenderRadius = island.baseRadius() + 300.0;
               if (!(distSq > maxRenderRadius * maxRenderRadius)) {
                  Camera camera = event.getCamera();
                  Vec3 camPos = camera.getPosition();
                  PoseStack poseStack = event.getPoseStack();
                  BufferSource bufferSource = mc.renderBuffers().bufferSource();
                  poseStack.pushPose();
                  poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
                  Matrix4f pose = poseStack.last().pose();
                  VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
renderChunkAlignedIslandBoundary(pose, lineConsumer, island, mc.level.getMinBuildHeight(), mc.level.getMaxBuildHeight());
                   renderCenterBeacon(pose, lineConsumer, island.centerX(), island.centerZ(), mc.level.getMaxBuildHeight(), island.islandType().getClimate());
                  poseStack.popPose();
               }
            }
         }
      }
   }

   private static void renderChunkAlignedIslandBoundary(Matrix4f pose, VertexConsumer lines, IslandData island, int minWorldY, int maxWorldY) {
      IslandClimate climate = island.islandType().getClimate();
      float a = 0.9F;
      float r;
      float g;
      float b;
      if (climate == IslandClimate.HOT) {
         r = 1.0F;
         g = 0.6F;
         b = 0.1F;
      } else if (climate == IslandClimate.WARM) {
         r = 0.7F;
         g = 1.0F;
         b = 0.1F;
      } else if (climate == IslandClimate.TEMPERATE) {
         r = 0.1F;
         g = 1.0F;
         b = 0.4F;
      } else if (climate == IslandClimate.COLD) {
         r = 0.1F;
         g = 0.9F;
         b = 0.9F;
      } else {
         r = 0.7F;
         g = 0.9F;
         b = 1.0F;
      }

      double searchRadius = island.baseRadius() + 64.0;
      int minChunkX = (int)Math.floor((island.centerX() - searchRadius) / 16.0);
      int maxChunkX = (int)Math.floor((island.centerX() + searchRadius) / 16.0);
      int minChunkZ = (int)Math.floor((island.centerZ() - searchRadius) / 16.0);
      int maxChunkZ = (int)Math.floor((island.centerZ() + searchRadius) / 16.0);
      float minY = Math.max((float)minWorldY, 60.0F);
      float maxY = maxWorldY;

      for (int cx = minChunkX; cx <= maxChunkX; cx++) {
         for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
            if (IslandHelper.isIslandChunk(cx, cz)) {
               float minX = cx * 16.0F;
               float maxX = minX + 16.0F;
               float minZ = cz * 16.0F;
               float maxZ = minZ + 16.0F;
               if (!IslandHelper.isIslandChunk(cx, cz - 1)) {
                  renderWallSegment(pose, lines, minX, minZ, maxX, minZ, minY, maxY, r, g, b, a);
               }

               if (!IslandHelper.isIslandChunk(cx, cz + 1)) {
                  renderWallSegment(pose, lines, minX, maxZ, maxX, maxZ, minY, maxY, r, g, b, a);
               }

               if (!IslandHelper.isIslandChunk(cx - 1, cz)) {
                  renderWallSegment(pose, lines, minX, minZ, minX, maxZ, minY, maxY, r, g, b, a);
               }

               if (!IslandHelper.isIslandChunk(cx + 1, cz)) {
                  renderWallSegment(pose, lines, maxX, minZ, maxX, maxZ, minY, maxY, r, g, b, a);
               }
            }
         }
      }
   }

   private static void renderWallSegment(
      Matrix4f pose, VertexConsumer lines, float x1, float z1, float x2, float z2, float minY, float maxY, float r, float g, float b, float a
   ) {
      lines.addVertex(pose, x1, minY, z1).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
      lines.addVertex(pose, x1, maxY, z1).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
      lines.addVertex(pose, x2, minY, z2).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
      lines.addVertex(pose, x2, maxY, z2).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);

      for (float y = 96.0F; y <= maxY; y += 32.0F) {
         lines.addVertex(pose, x1, y, z1).setColor(r, g, b, 0.5F).setNormal(0.0F, 1.0F, 0.0F);
         lines.addVertex(pose, x2, y, z2).setColor(r, g, b, 0.5F).setNormal(0.0F, 1.0F, 0.0F);
      }

      lines.addVertex(pose, x1, maxY, z1).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
      lines.addVertex(pose, x2, maxY, z2).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
      lines.addVertex(pose, x1, 96.0F, z1).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
      lines.addVertex(pose, x2, 96.0F, z2).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
   }

   private static void renderCenterBeacon(Matrix4f pose, VertexConsumer lines, double cx, double cz, int maxWorldY, IslandClimate climate) {
      float x = (float)cx;
      float z = (float)cz;
      float minY = 96.0F;
      float maxY = maxWorldY;
      float r;
      float g;
      float b;
      if (climate == IslandClimate.HOT) {
         r = 1.0F;
         g = 0.6F;
         b = 0.1F;
      } else if (climate == IslandClimate.WARM) {
         r = 0.7F;
         g = 1.0F;
         b = 0.1F;
      } else if (climate == IslandClimate.TEMPERATE) {
         r = 0.1F;
         g = 1.0F;
         b = 0.4F;
      } else if (climate == IslandClimate.COLD) {
         r = 0.1F;
         g = 0.9F;
         b = 0.9F;
      } else {
         r = 0.7F;
         g = 0.9F;
         b = 1.0F;
      }

      lines.addVertex(pose, x, minY, z).setColor(r, g, b, 1.0F).setNormal(0.0F, 1.0F, 0.0F);
      lines.addVertex(pose, x, maxY, z).setColor(r, g, b, 0.4F).setNormal(0.0F, 1.0F, 0.0F);
   }
}

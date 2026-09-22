package com.fmm.worldgen.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent.Key;

@EventBusSubscriber(modid = "fmm_worldgen", value = Dist.CLIENT)
public final class IslandClientHandler {
   public static boolean showIslandBorders = false;

   @SubscribeEvent
   public static void onKeyInput(Key event) {
      if (event.getAction() == 1) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.getWindow() != null) {
            long windowHandle = mc.getWindow().getWindow();
            boolean isF3Down = InputConstants.isKeyDown(windowHandle, 292);
            if (isF3Down && (event.getKey() == 90 || event.getKey() == 74)) {
               showIslandBorders = !showIslandBorders;
               Component message = Component.literal(
                  "§e[Debug]: §f3D-границы зоны острова: " + (showIslandBorders ? "§aпоказаны §7(F3 + Z)" : "§cскрыты §7(F3 + Z)")
               );
               mc.gui.getChat().addMessage(message);
            }
         }
      }
   }
}

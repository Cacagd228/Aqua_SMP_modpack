package com.meowaddons.client.nether;

import com.meowaddons.MeowAddons;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Регистрирует HUD-слой индикатора воздуха в Незере (см. NetherAirOverlay). */
@EventBusSubscriber(modid = MeowAddons.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NetherClientEvents {
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL, NetherAirOverlay.ID, NetherAirOverlay.instance());
    }
}
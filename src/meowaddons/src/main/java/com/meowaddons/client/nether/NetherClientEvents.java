package com.meowaddons.client.nether;
import com.meowaddons.MeowAddons;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
@EventBusSubscriber(modid=MeowAddons.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class NetherClientEvents{
 @SubscribeEvent public static void registerLayers(RegisterGuiLayersEvent e){
  e.registerAbove(VanillaGuiLayers.AIR_LEVEL,NetherAirOverlay.ID,NetherAirOverlay.INSTANCE);
 }
}

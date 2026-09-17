package com.meowaddons;
import com.meowaddons.transmitter.TransmitterBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
@EventBusSubscriber(modid=MeowAddons.MODID,bus=EventBusSubscriber.Bus.MOD)
public class ModCapabilities{
 @SubscribeEvent public static void onRegisterCapabilities(RegisterCapabilitiesEvent e){
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.INTERDIMENSIONAL_TRANSMITTER.get(),(TransmitterBlockEntity be,net.minecraft.core.Direction s)->be.getExposedInventory());
 }
}

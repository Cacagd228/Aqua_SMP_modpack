package com.meowaddons;
import com.meowaddons.tier.millstone.TieredMillstoneBlockEntity;
import com.meowaddons.tier.saw.TieredSawBlockEntity;
import com.meowaddons.transmitter.TransmitterBlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
@EventBusSubscriber(modid=MeowAddons.MODID,bus=EventBusSubscriber.Bus.MOD)
public class ModCapabilities{
 @SubscribeEvent public static void onRegisterCapabilities(RegisterCapabilitiesEvent e){
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.INTERDIMENSIONAL_TRANSMITTER.get(),(TransmitterBlockEntity be,net.minecraft.core.Direction s)->be.getExposedInventory());
  // Пилы: как ванильный SawBlockEntity.registerCapabilities (всё кроме низа)
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_SAW_T1.get(),(TieredSawBlockEntity be,Direction s)->s!=Direction.DOWN?be.inventory:null);
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_SAW_T2.get(),(TieredSawBlockEntity be,Direction s)->s!=Direction.DOWN?be.inventory:null);
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_SAW_T3.get(),(TieredSawBlockEntity be,Direction s)->s!=Direction.DOWN?be.inventory:null);
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_SAW_T4.get(),(TieredSawBlockEntity be,Direction s)->s!=Direction.DOWN?be.inventory:null);
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_SAW_T5.get(),(TieredSawBlockEntity be,Direction s)->s!=Direction.DOWN?be.inventory:null);
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_SAW_T6.get(),(TieredSawBlockEntity be,Direction s)->s!=Direction.DOWN?be.inventory:null);
  // Жернова: как ванильный MillstoneBlockEntity.registerCapabilities
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_MILLSTONE_T1.get(),(TieredMillstoneBlockEntity be,Direction s)->be.capability);
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_MILLSTONE_T2.get(),(TieredMillstoneBlockEntity be,Direction s)->be.capability);
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_MILLSTONE_T3.get(),(TieredMillstoneBlockEntity be,Direction s)->be.capability);
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_MILLSTONE_T4.get(),(TieredMillstoneBlockEntity be,Direction s)->be.capability);
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_MILLSTONE_T5.get(),(TieredMillstoneBlockEntity be,Direction s)->be.capability);
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_MILLSTONE_T6.get(),(TieredMillstoneBlockEntity be,Direction s)->be.capability);
 }
}

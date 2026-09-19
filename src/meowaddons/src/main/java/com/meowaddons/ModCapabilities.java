package com.meowaddons;
import com.meowaddons.tier.TieredCrushingWheelControllerBlockEntity;
import com.meowaddons.tier.TieredDeployerBlockEntity;
import com.meowaddons.transmitter.TransmitterBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerItemHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
@EventBusSubscriber(modid=MeowAddons.MODID,bus=EventBusSubscriber.Bus.MOD)
public class ModCapabilities{
 @SubscribeEvent public static void onRegisterCapabilities(RegisterCapabilitiesEvent e){
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.INTERDIMENSIONAL_TRANSMITTER.get(),(TransmitterBlockEntity be,net.minecraft.core.Direction s)->be.getExposedInventory());
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_CRUSHING_WHEEL_CONTROLLER.get(),(TieredCrushingWheelControllerBlockEntity be,net.minecraft.core.Direction s)->be.inventory);
  //DeployerItemHandler public — оборачивает фейк-игрока так же, как ванильный деплойер
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_DEPLOYER_T1.get(),(TieredDeployerBlockEntity be,net.minecraft.core.Direction s)->new DeployerItemHandler(be));
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_DEPLOYER_T2.get(),(TieredDeployerBlockEntity be,net.minecraft.core.Direction s)->new DeployerItemHandler(be));
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_DEPLOYER_T3.get(),(TieredDeployerBlockEntity be,net.minecraft.core.Direction s)->new DeployerItemHandler(be));
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_DEPLOYER_T4.get(),(TieredDeployerBlockEntity be,net.minecraft.core.Direction s)->new DeployerItemHandler(be));
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_DEPLOYER_T5.get(),(TieredDeployerBlockEntity be,net.minecraft.core.Direction s)->new DeployerItemHandler(be));
  e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ModBlockEntities.TIERED_DEPLOYER_T6.get(),(TieredDeployerBlockEntity be,net.minecraft.core.Direction s)->new DeployerItemHandler(be));
 }
}

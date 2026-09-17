package com.meowaddons;
import com.meowaddons.nether.NetherHeatHandler;
import com.meowaddons.recipe.ModRecipeSerializers;
import com.meowaddons.recipe.ModRecipeTypes;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.item.ItemDescription;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MeowAddons.MODID)
public class MeowAddons {
 public static final String MODID = "meowaddons";
 public MeowAddons(IEventBus b, ModContainer c){
  ModBlocks.register(b);
  ModBlockEntities.register(b);
  ModMenus.register(b);
  ModItems.register(b);
  ModCreativeTabs.register(b);
  ModRecipeTypes.register(b);
   ModRecipeSerializers.register(b);
   NeoForge.EVENT_BUS.addListener(NetherHeatHandler::onPlayerTick);
  b.addListener(this::commonSetup);
  b.addListener(this::clientSetup);
 }
 private void commonSetup(FMLCommonSetupEvent e){
  e.enqueueWork(() -> {
   // T1 8 → 2k @256 RPM, T2 32 → 8k, T3 128 → 32k, T4 512 → 128k, T5 2048 → 512k, T6 8192 → 2048k (x4 на тир как в Tier.speedFactor)
   BlockStressValues.IMPACTS.register(ModBlocks.PRESS_T1.get(), () -> 8.0);
   BlockStressValues.IMPACTS.register(ModBlocks.PRESS_T2.get(), () -> 32.0);
   BlockStressValues.IMPACTS.register(ModBlocks.PRESS_T3.get(), () -> 128.0);
   BlockStressValues.IMPACTS.register(ModBlocks.PRESS_T4.get(), () -> 512.0);
   BlockStressValues.IMPACTS.register(ModBlocks.PRESS_T5.get(), () -> 2048.0);
   BlockStressValues.IMPACTS.register(ModBlocks.PRESS_T6.get(), () -> 8192.0);
  });
 }
 private void clientSetup(FMLClientSetupEvent e){
  e.enqueueWork(() -> {
   try {
    ItemDescription.referKey(ModItems.PRESS_T1.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
    ItemDescription.referKey(ModItems.PRESS_T2.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
    ItemDescription.referKey(ModItems.PRESS_T3.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
    ItemDescription.referKey(ModItems.PRESS_T4.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
    ItemDescription.referKey(ModItems.PRESS_T5.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
    ItemDescription.referKey(ModItems.PRESS_T6.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
   } catch(Exception ex){}
  });
 }
}

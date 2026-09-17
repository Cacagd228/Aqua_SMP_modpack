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
    // Остальные машинки: ванильная база (mixer/saw/mill/deployer 4, crusher 8, fan 2), скейл x4 на тир как у прессов
    net.minecraft.world.level.block.Block[][] groups = {
     {ModBlocks.MIXER_T1.get(), ModBlocks.MIXER_T2.get(), ModBlocks.MIXER_T3.get(), ModBlocks.MIXER_T4.get(), ModBlocks.MIXER_T5.get(), ModBlocks.MIXER_T6.get()},
     {ModBlocks.SAW_T1.get(), ModBlocks.SAW_T2.get(), ModBlocks.SAW_T3.get(), ModBlocks.SAW_T4.get(), ModBlocks.SAW_T5.get(), ModBlocks.SAW_T6.get()},
     {ModBlocks.MILLSTONE_T1.get(), ModBlocks.MILLSTONE_T2.get(), ModBlocks.MILLSTONE_T3.get(), ModBlocks.MILLSTONE_T4.get(), ModBlocks.MILLSTONE_T5.get(), ModBlocks.MILLSTONE_T6.get()},
     {ModBlocks.CRUSHER_T1.get(), ModBlocks.CRUSHER_T2.get(), ModBlocks.CRUSHER_T3.get(), ModBlocks.CRUSHER_T4.get(), ModBlocks.CRUSHER_T5.get(), ModBlocks.CRUSHER_T6.get()},
     {ModBlocks.DEPLOYER_T1.get(), ModBlocks.DEPLOYER_T2.get(), ModBlocks.DEPLOYER_T3.get(), ModBlocks.DEPLOYER_T4.get(), ModBlocks.DEPLOYER_T5.get(), ModBlocks.DEPLOYER_T6.get()},
     {ModBlocks.FAN_T1.get(), ModBlocks.FAN_T2.get(), ModBlocks.FAN_T3.get(), ModBlocks.FAN_T4.get(), ModBlocks.FAN_T5.get(), ModBlocks.FAN_T6.get()}
    };
    double[] bases = {4.0, 4.0, 4.0, 8.0, 4.0, 2.0};
    for(int g = 0; g < groups.length; g++){
     for(int t = 0; t < 6; t++){
      double v = bases[g] * Math.pow(4, t);
      BlockStressValues.IMPACTS.register(groups[g][t], () -> v);
     }
    }
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

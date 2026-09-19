package com.meowaddons;
import com.meowaddons.config.SpeedFactorConfig;
import com.meowaddons.recipe.ModRecipeSerializers;
import com.meowaddons.recipe.ModRecipeTypes;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(MeowAddons.MODID)
public class MeowAddons {
 public static final String MODID = "meowaddons";
public MeowAddons(IEventBus b, ModContainer c){
    c.registerConfig(ModConfig.Type.COMMON, SpeedFactorConfig.COMMON_SPEC);
    ModBlocks.register(b);
    ModBlockEntities.register(b);
    ModMenus.register(b);
    ModItems.register(b);
    ModCreativeTabs.register(b);
    ModRecipeTypes.register(b);
    ModRecipeSerializers.register(b);
    b.addListener(this::commonSetup);
    b.addListener(this::clientSetup);
    // BowEnchantmentEvents и AutoShot-логика висят на @EventBusSubscriber —
    // ручной NeoForge.EVENT_BUS.register здесь дублировал события.
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
     // crushing_wheel база 8.0 — те же x4 per-tier
     BlockStressValues.IMPACTS.register(ModBlocks.CRUSHING_WHEEL_T1.get(), () -> 8.0);
     BlockStressValues.IMPACTS.register(ModBlocks.CRUSHING_WHEEL_T2.get(), () -> 32.0);
     BlockStressValues.IMPACTS.register(ModBlocks.CRUSHING_WHEEL_T3.get(), () -> 128.0);
     BlockStressValues.IMPACTS.register(ModBlocks.CRUSHING_WHEEL_T4.get(), () -> 512.0);
     BlockStressValues.IMPACTS.register(ModBlocks.CRUSHING_WHEEL_T5.get(), () -> 2048.0);
     BlockStressValues.IMPACTS.register(ModBlocks.CRUSHING_WHEEL_T6.get(), () -> 8192.0);
     // millstone база 4.0 — x4 per-tier
     BlockStressValues.IMPACTS.register(ModBlocks.MILLSTONE_T1.get(), () -> 4.0);
     BlockStressValues.IMPACTS.register(ModBlocks.MILLSTONE_T2.get(), () -> 16.0);
     BlockStressValues.IMPACTS.register(ModBlocks.MILLSTONE_T3.get(), () -> 64.0);
     BlockStressValues.IMPACTS.register(ModBlocks.MILLSTONE_T4.get(), () -> 256.0);
     BlockStressValues.IMPACTS.register(ModBlocks.MILLSTONE_T5.get(), () -> 1024.0);
     BlockStressValues.IMPACTS.register(ModBlocks.MILLSTONE_T6.get(), () -> 4096.0);
     // mixer база 4.0 — x4 per-tier как у жерновов (было завышено x8)
     BlockStressValues.IMPACTS.register(ModBlocks.MIXER_T1.get(), () -> 4.0);
     BlockStressValues.IMPACTS.register(ModBlocks.MIXER_T2.get(), () -> 16.0);
     BlockStressValues.IMPACTS.register(ModBlocks.MIXER_T3.get(), () -> 64.0);
     BlockStressValues.IMPACTS.register(ModBlocks.MIXER_T4.get(), () -> 256.0);
     BlockStressValues.IMPACTS.register(ModBlocks.MIXER_T5.get(), () -> 1024.0);
     BlockStressValues.IMPACTS.register(ModBlocks.MIXER_T6.get(), () -> 4096.0);
     // saw база 4.0 — x4 per-tier как у пилы Create (4.0)
     BlockStressValues.IMPACTS.register(ModBlocks.SAW_T1.get(), () -> 4.0);
     BlockStressValues.IMPACTS.register(ModBlocks.SAW_T2.get(), () -> 16.0);
     BlockStressValues.IMPACTS.register(ModBlocks.SAW_T3.get(), () -> 64.0);
     BlockStressValues.IMPACTS.register(ModBlocks.SAW_T4.get(), () -> 256.0);
      BlockStressValues.IMPACTS.register(ModBlocks.SAW_T5.get(), () -> 1024.0);
      BlockStressValues.IMPACTS.register(ModBlocks.SAW_T6.get(), () -> 4096.0);
      // deployer база 4.25 (как в Create) — x4 per-tier
      BlockStressValues.IMPACTS.register(ModBlocks.DEPLOYER_T1.get(), () -> 4.25);
      BlockStressValues.IMPACTS.register(ModBlocks.DEPLOYER_T2.get(), () -> 17.0);
      BlockStressValues.IMPACTS.register(ModBlocks.DEPLOYER_T3.get(), () -> 68.0);
      BlockStressValues.IMPACTS.register(ModBlocks.DEPLOYER_T4.get(), () -> 272.0);
      BlockStressValues.IMPACTS.register(ModBlocks.DEPLOYER_T5.get(), () -> 1088.0);
      BlockStressValues.IMPACTS.register(ModBlocks.DEPLOYER_T6.get(), () -> 4352.0);
     });
   }
  private void clientSetup(FMLClientSetupEvent e){
   e.enqueueWork(() -> {
    try {
     // Верх "Удерживайте W для размышлений" — через Ponder (MeowPonderPlugin, 2 сцены pressing/compacting из Create)
     // Середина — пропуск (referKey на mechanical_press, у которого нет tooltip.summary)
     // Низ — потребление (кинетика) — регистрируем KineticStats как в Create (иначе TooltipModifier.REGISTRY пуст для наших предметов)
     ItemDescription.referKey(ModItems.PRESS_T1.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
     ItemDescription.referKey(ModItems.PRESS_T2.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
     ItemDescription.referKey(ModItems.PRESS_T3.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
     ItemDescription.referKey(ModItems.PRESS_T4.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
     ItemDescription.referKey(ModItems.PRESS_T5.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
     ItemDescription.referKey(ModItems.PRESS_T6.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
     // KineticStats даёт низ тултипа "Stress Impact — High/Medium/Low + x RPM" с учётом BlockStressValues.IMPACTS (8/32/128/512/2048/8192)
      TooltipModifier.REGISTRY.register(ModItems.PRESS_T1.get(), KineticStats.create(ModItems.PRESS_T1.get()));
      TooltipModifier.REGISTRY.register(ModItems.PRESS_T2.get(), KineticStats.create(ModItems.PRESS_T2.get()));
      TooltipModifier.REGISTRY.register(ModItems.PRESS_T3.get(), KineticStats.create(ModItems.PRESS_T3.get()));
      TooltipModifier.REGISTRY.register(ModItems.PRESS_T4.get(), KineticStats.create(ModItems.PRESS_T4.get()));
      TooltipModifier.REGISTRY.register(ModItems.PRESS_T5.get(), KineticStats.create(ModItems.PRESS_T5.get()));
      TooltipModifier.REGISTRY.register(ModItems.PRESS_T6.get(), KineticStats.create(ModItems.PRESS_T6.get()));
      // crushing wheels — тот же принцип: вверх W (Ponder crushing_wheels), середина пропуск, внизу стресс per-tier
      ItemDescription.referKey(ModItems.CRUSHING_WHEEL_T1.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "crushing_wheel")));
      ItemDescription.referKey(ModItems.CRUSHING_WHEEL_T2.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "crushing_wheel")));
      ItemDescription.referKey(ModItems.CRUSHING_WHEEL_T3.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "crushing_wheel")));
      ItemDescription.referKey(ModItems.CRUSHING_WHEEL_T4.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "crushing_wheel")));
      ItemDescription.referKey(ModItems.CRUSHING_WHEEL_T5.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "crushing_wheel")));
      ItemDescription.referKey(ModItems.CRUSHING_WHEEL_T6.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "crushing_wheel")));
      TooltipModifier.REGISTRY.register(ModItems.CRUSHING_WHEEL_T1.get(), KineticStats.create(ModItems.CRUSHING_WHEEL_T1.get()));
      TooltipModifier.REGISTRY.register(ModItems.CRUSHING_WHEEL_T2.get(), KineticStats.create(ModItems.CRUSHING_WHEEL_T2.get()));
      TooltipModifier.REGISTRY.register(ModItems.CRUSHING_WHEEL_T3.get(), KineticStats.create(ModItems.CRUSHING_WHEEL_T3.get()));
      TooltipModifier.REGISTRY.register(ModItems.CRUSHING_WHEEL_T4.get(), KineticStats.create(ModItems.CRUSHING_WHEEL_T4.get()));
      TooltipModifier.REGISTRY.register(ModItems.CRUSHING_WHEEL_T5.get(), KineticStats.create(ModItems.CRUSHING_WHEEL_T5.get()));
      TooltipModifier.REGISTRY.register(ModItems.CRUSHING_WHEEL_T6.get(), KineticStats.create(ModItems.CRUSHING_WHEEL_T6.get()));
      // millstones — аналогично
      ItemDescription.referKey(ModItems.MILLSTONE_T1.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "millstone")));
      ItemDescription.referKey(ModItems.MILLSTONE_T2.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "millstone")));
      ItemDescription.referKey(ModItems.MILLSTONE_T3.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "millstone")));
      ItemDescription.referKey(ModItems.MILLSTONE_T4.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "millstone")));
      ItemDescription.referKey(ModItems.MILLSTONE_T5.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "millstone")));
      ItemDescription.referKey(ModItems.MILLSTONE_T6.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "millstone")));
      TooltipModifier.REGISTRY.register(ModItems.MILLSTONE_T1.get(), KineticStats.create(ModItems.MILLSTONE_T1.get()));
      TooltipModifier.REGISTRY.register(ModItems.MILLSTONE_T2.get(), KineticStats.create(ModItems.MILLSTONE_T2.get()));
      TooltipModifier.REGISTRY.register(ModItems.MILLSTONE_T3.get(), KineticStats.create(ModItems.MILLSTONE_T3.get()));
      TooltipModifier.REGISTRY.register(ModItems.MILLSTONE_T4.get(), KineticStats.create(ModItems.MILLSTONE_T4.get()));
      TooltipModifier.REGISTRY.register(ModItems.MILLSTONE_T5.get(), KineticStats.create(ModItems.MILLSTONE_T5.get()));
      TooltipModifier.REGISTRY.register(ModItems.MILLSTONE_T6.get(), KineticStats.create(ModItems.MILLSTONE_T6.get()));
      // mixers — аналогично
      ItemDescription.referKey(ModItems.MIXER_T1.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_mixer")));
      ItemDescription.referKey(ModItems.MIXER_T2.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_mixer")));
      ItemDescription.referKey(ModItems.MIXER_T3.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_mixer")));
      ItemDescription.referKey(ModItems.MIXER_T4.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_mixer")));
      ItemDescription.referKey(ModItems.MIXER_T5.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_mixer")));
      ItemDescription.referKey(ModItems.MIXER_T6.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_mixer")));
      TooltipModifier.REGISTRY.register(ModItems.MIXER_T1.get(), KineticStats.create(ModItems.MIXER_T1.get()));
      TooltipModifier.REGISTRY.register(ModItems.MIXER_T2.get(), KineticStats.create(ModItems.MIXER_T2.get()));
      TooltipModifier.REGISTRY.register(ModItems.MIXER_T3.get(), KineticStats.create(ModItems.MIXER_T3.get()));
      TooltipModifier.REGISTRY.register(ModItems.MIXER_T4.get(), KineticStats.create(ModItems.MIXER_T4.get()));
      TooltipModifier.REGISTRY.register(ModItems.MIXER_T5.get(), KineticStats.create(ModItems.MIXER_T5.get()));
      TooltipModifier.REGISTRY.register(ModItems.MIXER_T6.get(), KineticStats.create(ModItems.MIXER_T6.get()));
      // saws — аналогично
      ItemDescription.referKey(ModItems.SAW_T1.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_saw")));
      ItemDescription.referKey(ModItems.SAW_T2.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_saw")));
      ItemDescription.referKey(ModItems.SAW_T3.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_saw")));
      ItemDescription.referKey(ModItems.SAW_T4.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_saw")));
      ItemDescription.referKey(ModItems.SAW_T5.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_saw")));
      ItemDescription.referKey(ModItems.SAW_T6.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_saw")));
      TooltipModifier.REGISTRY.register(ModItems.SAW_T1.get(), KineticStats.create(ModItems.SAW_T1.get()));
      TooltipModifier.REGISTRY.register(ModItems.SAW_T2.get(), KineticStats.create(ModItems.SAW_T2.get()));
      TooltipModifier.REGISTRY.register(ModItems.SAW_T3.get(), KineticStats.create(ModItems.SAW_T3.get()));
      TooltipModifier.REGISTRY.register(ModItems.SAW_T4.get(), KineticStats.create(ModItems.SAW_T4.get()));
       TooltipModifier.REGISTRY.register(ModItems.SAW_T5.get(), KineticStats.create(ModItems.SAW_T5.get()));
       TooltipModifier.REGISTRY.register(ModItems.SAW_T6.get(), KineticStats.create(ModItems.SAW_T6.get()));
       // deployers — аналогично
       ItemDescription.referKey(ModItems.DEPLOYER_T1.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "deployer")));
       ItemDescription.referKey(ModItems.DEPLOYER_T2.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "deployer")));
       ItemDescription.referKey(ModItems.DEPLOYER_T3.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "deployer")));
       ItemDescription.referKey(ModItems.DEPLOYER_T4.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "deployer")));
       ItemDescription.referKey(ModItems.DEPLOYER_T5.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "deployer")));
       ItemDescription.referKey(ModItems.DEPLOYER_T6.get(), () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "deployer")));
       TooltipModifier.REGISTRY.register(ModItems.DEPLOYER_T1.get(), KineticStats.create(ModItems.DEPLOYER_T1.get()));
       TooltipModifier.REGISTRY.register(ModItems.DEPLOYER_T2.get(), KineticStats.create(ModItems.DEPLOYER_T2.get()));
       TooltipModifier.REGISTRY.register(ModItems.DEPLOYER_T3.get(), KineticStats.create(ModItems.DEPLOYER_T3.get()));
       TooltipModifier.REGISTRY.register(ModItems.DEPLOYER_T4.get(), KineticStats.create(ModItems.DEPLOYER_T4.get()));
       TooltipModifier.REGISTRY.register(ModItems.DEPLOYER_T5.get(), KineticStats.create(ModItems.DEPLOYER_T5.get()));
       TooltipModifier.REGISTRY.register(ModItems.DEPLOYER_T6.get(), KineticStats.create(ModItems.DEPLOYER_T6.get()));
     } catch(Exception ex){ ex.printStackTrace(); }
   });
  }
}

package com.meowaddons.recipe;
import com.meowaddons.MeowAddons;
import net.minecraft.resources.ResourceLocation;
import com.meowaddons.tier.Tier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public class ModRecipeTypes {
 public static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE, MeowAddons.MODID);
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredPressingRecipe>> PRESSING_T1=TYPES.register("pressing_t1",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "pressing_t1")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredPressingRecipe>> PRESSING_T2=TYPES.register("pressing_t2",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "pressing_t2")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredPressingRecipe>> PRESSING_T3=TYPES.register("pressing_t3",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "pressing_t3")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredPressingRecipe>> PRESSING_T4=TYPES.register("pressing_t4",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "pressing_t4")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredPressingRecipe>> PRESSING_T5=TYPES.register("pressing_t5",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "pressing_t5")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredPressingRecipe>> PRESSING_T6=TYPES.register("pressing_t6",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "pressing_t6")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredCrushingRecipe>> CRUSHING_T1=TYPES.register("crushing_t1",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "crushing_t1")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredCrushingRecipe>> CRUSHING_T2=TYPES.register("crushing_t2",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "crushing_t2")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredCrushingRecipe>> CRUSHING_T3=TYPES.register("crushing_t3",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "crushing_t3")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredCrushingRecipe>> CRUSHING_T4=TYPES.register("crushing_t4",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "crushing_t4")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredCrushingRecipe>> CRUSHING_T5=TYPES.register("crushing_t5",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "crushing_t5")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredCrushingRecipe>> CRUSHING_T6=TYPES.register("crushing_t6",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "crushing_t6")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredMillingRecipe>> MILLING_T1=TYPES.register("milling_t1",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "milling_t1")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredMillingRecipe>> MILLING_T2=TYPES.register("milling_t2",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "milling_t2")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredMillingRecipe>> MILLING_T3=TYPES.register("milling_t3",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "milling_t3")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredMillingRecipe>> MILLING_T4=TYPES.register("milling_t4",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "milling_t4")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredMillingRecipe>> MILLING_T5=TYPES.register("milling_t5",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "milling_t5")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredMillingRecipe>> MILLING_T6=TYPES.register("milling_t6",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "milling_t6")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredMixingRecipe>> MIXING_T1=TYPES.register("mixing_t1",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "mixing_t1")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredMixingRecipe>> MIXING_T2=TYPES.register("mixing_t2",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "mixing_t2")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredMixingRecipe>> MIXING_T3=TYPES.register("mixing_t3",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "mixing_t3")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredMixingRecipe>> MIXING_T4=TYPES.register("mixing_t4",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "mixing_t4")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredMixingRecipe>> MIXING_T5=TYPES.register("mixing_t5",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "mixing_t5")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredMixingRecipe>> MIXING_T6=TYPES.register("mixing_t6",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "mixing_t6")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredCuttingRecipe>> CUTTING_T1=TYPES.register("cutting_t1",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "cutting_t1")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredCuttingRecipe>> CUTTING_T2=TYPES.register("cutting_t2",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "cutting_t2")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredCuttingRecipe>> CUTTING_T3=TYPES.register("cutting_t3",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "cutting_t3")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredCuttingRecipe>> CUTTING_T4=TYPES.register("cutting_t4",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "cutting_t4")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredCuttingRecipe>> CUTTING_T5=TYPES.register("cutting_t5",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "cutting_t5")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredCuttingRecipe>> CUTTING_T6=TYPES.register("cutting_t6",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "cutting_t6")));
 public static RecipeType<TieredPressingRecipe> pressingTier(Tier t){
  return switch(t){
   case ANDESITE->PRESSING_T1.get();
   case BRASS->PRESSING_T2.get();
   case STEEL->PRESSING_T3.get();
   case SHADOW_STEEL->PRESSING_T4.get();
   case REFINED_RADIANCE->PRESSING_T5.get();
   case CHROMATIC->PRESSING_T6.get();
  };
 }
 public static RecipeType<TieredCrushingRecipe> crushingTier(Tier t){
  return switch(t){
   case ANDESITE->CRUSHING_T1.get();
   case BRASS->CRUSHING_T2.get();
   case STEEL->CRUSHING_T3.get();
   case SHADOW_STEEL->CRUSHING_T4.get();
   case REFINED_RADIANCE->CRUSHING_T5.get();
   case CHROMATIC->CRUSHING_T6.get();
  };
 }
 public static RecipeType<TieredMillingRecipe> millingTier(Tier t){
  return switch(t){
   case ANDESITE->MILLING_T1.get();
   case BRASS->MILLING_T2.get();
   case STEEL->MILLING_T3.get();
   case SHADOW_STEEL->MILLING_T4.get();
   case REFINED_RADIANCE->MILLING_T5.get();
   case CHROMATIC->MILLING_T6.get();
  };
 }
 public static RecipeType<TieredMixingRecipe> mixingTier(Tier t){
  return switch(t){
   case ANDESITE->MIXING_T1.get();
   case BRASS->MIXING_T2.get();
   case STEEL->MIXING_T3.get();
   case SHADOW_STEEL->MIXING_T4.get();
   case REFINED_RADIANCE->MIXING_T5.get();
   case CHROMATIC->MIXING_T6.get();
  };
 }
 public static RecipeType<TieredCuttingRecipe> cuttingTier(Tier t){
  return switch(t){
   case ANDESITE->CUTTING_T1.get();
   case BRASS->CUTTING_T2.get();
   case STEEL->CUTTING_T3.get();
   case SHADOW_STEEL->CUTTING_T4.get();
   case REFINED_RADIANCE->CUTTING_T5.get();
   case CHROMATIC->CUTTING_T6.get();
  };
 }
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredDeployerApplicationRecipe>> DEPLOYING_T1=TYPES.register("deploying_t1",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "deploying_t1")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredDeployerApplicationRecipe>> DEPLOYING_T2=TYPES.register("deploying_t2",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "deploying_t2")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredDeployerApplicationRecipe>> DEPLOYING_T3=TYPES.register("deploying_t3",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "deploying_t3")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredDeployerApplicationRecipe>> DEPLOYING_T4=TYPES.register("deploying_t4",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "deploying_t4")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredDeployerApplicationRecipe>> DEPLOYING_T5=TYPES.register("deploying_t5",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "deploying_t5")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredDeployerApplicationRecipe>> DEPLOYING_T6=TYPES.register("deploying_t6",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "deploying_t6")));
 public static RecipeType<TieredDeployerApplicationRecipe> deployingTier(Tier t){
  return switch(t){
   case ANDESITE->DEPLOYING_T1.get();
   case BRASS->DEPLOYING_T2.get();
   case STEEL->DEPLOYING_T3.get();
   case SHADOW_STEEL->DEPLOYING_T4.get();
   case REFINED_RADIANCE->DEPLOYING_T5.get();
   case CHROMATIC->DEPLOYING_T6.get();
  };
 }
 // Типы нужны как id-якоря: сам тировый рецепт сборки отдаёт в getType() vanilla SEQUENCED_ASSEMBLY
 // (поиск сборки в машинах Create зашит на этот тип), см. TieredSequencedAssemblyRecipe
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredSequencedAssemblyRecipe>> SEQUENCED_ASSEMBLY_T1=TYPES.register("sequenced_assembly_t1",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "sequenced_assembly_t1")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredSequencedAssemblyRecipe>> SEQUENCED_ASSEMBLY_T2=TYPES.register("sequenced_assembly_t2",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "sequenced_assembly_t2")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredSequencedAssemblyRecipe>> SEQUENCED_ASSEMBLY_T3=TYPES.register("sequenced_assembly_t3",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "sequenced_assembly_t3")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredSequencedAssemblyRecipe>> SEQUENCED_ASSEMBLY_T4=TYPES.register("sequenced_assembly_t4",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "sequenced_assembly_t4")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredSequencedAssemblyRecipe>> SEQUENCED_ASSEMBLY_T5=TYPES.register("sequenced_assembly_t5",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "sequenced_assembly_t5")));
 public static final DeferredHolder<RecipeType<?>,RecipeType<TieredSequencedAssemblyRecipe>> SEQUENCED_ASSEMBLY_T6=TYPES.register("sequenced_assembly_t6",()->RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "sequenced_assembly_t6")));
 public static RecipeType<TieredSequencedAssemblyRecipe> sequencedAssemblyTier(Tier t){
  return switch(t){
   case ANDESITE->SEQUENCED_ASSEMBLY_T1.get();
   case BRASS->SEQUENCED_ASSEMBLY_T2.get();
   case STEEL->SEQUENCED_ASSEMBLY_T3.get();
   case SHADOW_STEEL->SEQUENCED_ASSEMBLY_T4.get();
   case REFINED_RADIANCE->SEQUENCED_ASSEMBLY_T5.get();
   case CHROMATIC->SEQUENCED_ASSEMBLY_T6.get();
  };
 }
 public static void register(IEventBus b){ TYPES.register(b); }
}

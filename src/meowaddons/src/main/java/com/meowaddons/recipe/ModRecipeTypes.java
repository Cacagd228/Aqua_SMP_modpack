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
 public static void register(IEventBus b){ TYPES.register(b); }
}

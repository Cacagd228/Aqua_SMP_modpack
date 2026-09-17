package com.meowaddons.recipe;
import com.meowaddons.MeowAddons;
import com.meowaddons.tier.Tier;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public class ModRecipeSerializers {
 public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER, MeowAddons.MODID);
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredPressingRecipe>> PRESSING_T1=SERIALIZERS.register("pressing_t1",()->new TieredPressingSerializer(Tier.ANDESITE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredPressingRecipe>> PRESSING_T2=SERIALIZERS.register("pressing_t2",()->new TieredPressingSerializer(Tier.BRASS));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredPressingRecipe>> PRESSING_T3=SERIALIZERS.register("pressing_t3",()->new TieredPressingSerializer(Tier.STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredPressingRecipe>> PRESSING_T4=SERIALIZERS.register("pressing_t4",()->new TieredPressingSerializer(Tier.SHADOW_STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredPressingRecipe>> PRESSING_T5=SERIALIZERS.register("pressing_t5",()->new TieredPressingSerializer(Tier.REFINED_RADIANCE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredPressingRecipe>> PRESSING_T6=SERIALIZERS.register("pressing_t6",()->new TieredPressingSerializer(Tier.CHROMATIC));
 public static RecipeSerializer<TieredPressingRecipe> pressingTier(Tier t){
  return switch(t){
   case ANDESITE->PRESSING_T1.get();
   case BRASS->PRESSING_T2.get();
   case STEEL->PRESSING_T3.get();
   case SHADOW_STEEL->PRESSING_T4.get();
   case REFINED_RADIANCE->PRESSING_T5.get();
   case CHROMATIC->PRESSING_T6.get();
  };
 }
 public static void register(IEventBus b){ SERIALIZERS.register(b); }
 public static class TieredPressingSerializer implements RecipeSerializer<TieredPressingRecipe> {
  private final Tier tier;
  private final MapCodec<TieredPressingRecipe> codec;
  private final StreamCodec<RegistryFriendlyByteBuf,TieredPressingRecipe> streamCodec;
  public TieredPressingSerializer(Tier tier){
   this.tier=tier;
   this.codec=TieredPressingRecipe.codec(tier);
   this.streamCodec=TieredPressingRecipe.streamCodec(tier);
  }
  @Override public MapCodec<TieredPressingRecipe> codec(){ return codec; }
  @Override public StreamCodec<RegistryFriendlyByteBuf,TieredPressingRecipe> streamCodec(){ return streamCodec; }
 }
}

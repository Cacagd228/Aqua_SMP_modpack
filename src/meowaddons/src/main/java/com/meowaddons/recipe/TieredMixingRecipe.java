package com.meowaddons.recipe;
import com.meowaddons.tier.Tier;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
public class TieredMixingRecipe extends MixingRecipe implements TieredRecipe {
 private final Tier tier;
 public TieredMixingRecipe(ProcessingRecipeParams params, Tier tier){ super(params); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public RecipeType<?> getType(){ return ModRecipeTypes.mixingTier(tier); }
 @Override public RecipeSerializer<?> getSerializer(){ return ModRecipeSerializers.mixingTier(tier); }
 public static MapCodec<TieredMixingRecipe> codec(Tier tier){
  return ProcessingRecipe.codec((ProcessingRecipeParams p)->new TieredMixingRecipe(p,tier), ProcessingRecipeParams.CODEC);
 }
 public static StreamCodec<RegistryFriendlyByteBuf,TieredMixingRecipe> streamCodec(Tier tier){
  return ProcessingRecipe.streamCodec((ProcessingRecipeParams p)->new TieredMixingRecipe(p,tier), ProcessingRecipeParams.STREAM_CODEC);
 }
}

package com.meowaddons.recipe;
import com.meowaddons.tier.Tier;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
public class TieredPressingRecipe extends PressingRecipe {
 private final Tier tier;
 public TieredPressingRecipe(ProcessingRecipeParams params, Tier tier){ super(params); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public RecipeType<?> getType(){ return ModRecipeTypes.pressingTier(tier); }
 @Override public RecipeSerializer<?> getSerializer(){ return ModRecipeSerializers.pressingTier(tier); }
 public static MapCodec<TieredPressingRecipe> codec(Tier tier){
  return ProcessingRecipe.codec((ProcessingRecipeParams p)->new TieredPressingRecipe(p,tier), ProcessingRecipeParams.CODEC);
 }
 public static StreamCodec<RegistryFriendlyByteBuf,TieredPressingRecipe> streamCodec(Tier tier){
  return ProcessingRecipe.streamCodec((ProcessingRecipeParams p)->new TieredPressingRecipe(p,tier), ProcessingRecipeParams.STREAM_CODEC);
 }
}

package com.meowaddons.recipe;
import com.meowaddons.tier.Tier;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
public class TieredCrushingRecipe extends CrushingRecipe implements TieredRecipe {
 private final Tier tier;
 public TieredCrushingRecipe(ProcessingRecipeParams params, Tier tier){ super(params); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public RecipeType<?> getType(){ return ModRecipeTypes.crushingTier(tier); }
 @Override public RecipeSerializer<?> getSerializer(){ return ModRecipeSerializers.crushingTier(tier); }
 public static MapCodec<TieredCrushingRecipe> codec(Tier tier){
  return ProcessingRecipe.codec((ProcessingRecipeParams p)->new TieredCrushingRecipe(p,tier), ProcessingRecipeParams.CODEC);
 }
 public static StreamCodec<RegistryFriendlyByteBuf,TieredCrushingRecipe> streamCodec(Tier tier){
  return ProcessingRecipe.streamCodec((ProcessingRecipeParams p)->new TieredCrushingRecipe(p,tier), ProcessingRecipeParams.STREAM_CODEC);
 }
}

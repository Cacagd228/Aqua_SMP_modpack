package com.meowaddons.recipe;
import com.meowaddons.tier.Tier;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
public class TieredCuttingRecipe extends CuttingRecipe implements TieredRecipe {
 private final Tier tier;
 public TieredCuttingRecipe(ProcessingRecipeParams params, Tier tier){ super(params); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public RecipeType<?> getType(){ return ModRecipeTypes.cuttingTier(tier); }
 @Override public RecipeSerializer<?> getSerializer(){ return ModRecipeSerializers.cuttingTier(tier); }
 public static MapCodec<TieredCuttingRecipe> codec(Tier tier){
  return ProcessingRecipe.codec((ProcessingRecipeParams p)->new TieredCuttingRecipe(p,tier), ProcessingRecipeParams.CODEC);
 }
 public static StreamCodec<RegistryFriendlyByteBuf,TieredCuttingRecipe> streamCodec(Tier tier){
  return ProcessingRecipe.streamCodec((ProcessingRecipeParams p)->new TieredCuttingRecipe(p,tier), ProcessingRecipeParams.STREAM_CODEC);
 }
}

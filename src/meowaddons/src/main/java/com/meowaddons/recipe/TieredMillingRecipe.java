package com.meowaddons.recipe;
import com.meowaddons.tier.Tier;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
public class TieredMillingRecipe extends MillingRecipe implements TieredRecipe {
 private final Tier tier;
 public TieredMillingRecipe(ProcessingRecipeParams params, Tier tier){ super(params); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public RecipeType<?> getType(){ return ModRecipeTypes.millingTier(tier); }
 @Override public RecipeSerializer<?> getSerializer(){ return ModRecipeSerializers.millingTier(tier); }
 public static MapCodec<TieredMillingRecipe> codec(Tier tier){
  return ProcessingRecipe.codec((ProcessingRecipeParams p)->new TieredMillingRecipe(p,tier), ProcessingRecipeParams.CODEC);
 }
 public static StreamCodec<RegistryFriendlyByteBuf,TieredMillingRecipe> streamCodec(Tier tier){
  return ProcessingRecipe.streamCodec((ProcessingRecipeParams p)->new TieredMillingRecipe(p,tier), ProcessingRecipeParams.STREAM_CODEC);
 }
}

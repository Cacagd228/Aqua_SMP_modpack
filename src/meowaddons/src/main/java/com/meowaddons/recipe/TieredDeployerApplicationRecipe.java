package com.meowaddons.recipe;
import com.meowaddons.tier.Tier;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipeParams;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
public class TieredDeployerApplicationRecipe extends DeployerApplicationRecipe implements TieredRecipe {
	private final Tier tier;
	public TieredDeployerApplicationRecipe(ItemApplicationRecipeParams params, Tier tier){ super(params); this.tier=tier; }
	@Override public Tier getTier(){ return tier; }
	@Override public RecipeType<?> getType(){ return ModRecipeTypes.deployingTier(tier); }
	@Override public RecipeSerializer<?> getSerializer(){ return ModRecipeSerializers.deployingTier(tier); }
	public static MapCodec<TieredDeployerApplicationRecipe> codec(Tier tier){
		return ProcessingRecipe.codec((ItemApplicationRecipeParams p)->new TieredDeployerApplicationRecipe(p,tier), ItemApplicationRecipeParams.CODEC);
	}
	public static StreamCodec<RegistryFriendlyByteBuf,TieredDeployerApplicationRecipe> streamCodec(Tier tier){
		return ProcessingRecipe.streamCodec((ItemApplicationRecipeParams p)->new TieredDeployerApplicationRecipe(p,tier), ItemApplicationRecipeParams.STREAM_CODEC);
	}
}

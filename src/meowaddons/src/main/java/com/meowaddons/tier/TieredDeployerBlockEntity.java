package com.meowaddons.tier;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.meowaddons.config.SpeedFactorConfig;

public class TieredDeployerBlockEntity extends DeployerBlockEntity {
	private final Tier tier;
	public TieredDeployerBlockEntity(BlockPos pos, BlockState state, Tier tier){ this(getType(tier),pos,state,tier); }
	public TieredDeployerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Tier tier){ super(type,pos,state); this.tier=tier; }
	public Tier getTier(){ return tier; }
	public static BlockEntityType<? extends TieredDeployerBlockEntity> getType(Tier tier){
		return switch(tier){
			case ANDESITE-> com.meowaddons.ModBlockEntities.TIERED_DEPLOYER_T1.get();
			case BRASS-> com.meowaddons.ModBlockEntities.TIERED_DEPLOYER_T2.get();
			case STEEL-> com.meowaddons.ModBlockEntities.TIERED_DEPLOYER_T3.get();
			case SHADOW_STEEL-> com.meowaddons.ModBlockEntities.TIERED_DEPLOYER_T4.get();
			case REFINED_RADIANCE-> com.meowaddons.ModBlockEntities.TIERED_DEPLOYER_T5.get();
			case CHROMATIC-> com.meowaddons.ModBlockEntities.TIERED_DEPLOYER_T6.get();
		};
	}
	@Override public float getSpeed() {
		float base = super.getSpeed();
		if (base == 0) return 0;
		if (!SpeedFactorConfig.isDeployingEnabled()) return base;
		int factor = tier.speedFactor(Tier.ANDESITE);
		return base * factor;
	}
	// шаги тировой последовательной сборки: только meowaddons:deploying_t{this.tier} (exact tier match)
	@Override public net.minecraft.world.item.crafting.RecipeHolder<? extends net.minecraft.world.item.crafting.Recipe<? extends net.minecraft.world.item.crafting.RecipeInput>> getRecipe(net.minecraft.world.item.ItemStack stack){
		net.minecraft.world.level.Level level = getLevel();
		if (level != null && !level.isClientSide) {
			// Only check exact tier match for sequential assembly
			@SuppressWarnings("unchecked")
			net.minecraft.world.item.crafting.RecipeType<com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe> asmType = (net.minecraft.world.item.crafting.RecipeType<com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe>)(net.minecraft.world.item.crafting.RecipeType<?>) com.meowaddons.recipe.ModRecipeTypes.deployingTier(tier);
			var asmT = com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.getRecipe(level, stack, asmType, com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe.class);
			if (asmT.isPresent()) {
				var stepRecipe = asmT.get().value();
				// Проверяем ингредиент игрока: для deploying рецептов в sequential assembly
				// ingredients[0] = transitional item (сам предмет сборки), ingredients[1] = предмет в руке игрока
				// Игрок должен держать ingredients[1] в руке
				boolean validIngredient = true;
				if (getPlayer() != null) {
					var heldItem = getPlayer().getMainHandItem();
					// DeployerApplicationRecipe в sequential assembly имеет 2 ингредиента:
					// [0] = transitional item (подменяется на сборку), [1] = предмет для деплоя
					if (stepRecipe.getIngredients().size() >= 2) {
						if (!stepRecipe.getIngredients().get(1).test(heldItem)) {
							validIngredient = false;
						}
					}
				}
				if (!validIngredient) {
					// Ингредиент не подходит - не используем этот рецепт сборки
					return super.getRecipe(stack);
				}
				return asmT.get();
			}
		}
		return super.getRecipe(stack);
	}
}
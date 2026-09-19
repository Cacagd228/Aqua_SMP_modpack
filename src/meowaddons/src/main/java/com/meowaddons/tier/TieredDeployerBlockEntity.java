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
	// Ускорение через конфиг (по умолчанию выключено, как у пресса):
	// enable_deploying=false -> базовая скорость без множителя
	// enable_deploying=true -> getSpeed() * tier.speedFactor(ANDESITE)
	@Override protected Vec3 getMovementVector(){
		BlockState state = getBlockState();
		if (state.getBlock() instanceof TieredDeployerBlock && state.hasProperty(DirectionalKineticBlock.FACING))
			return Vec3.atLowerCornerOf(state.getValue(DirectionalKineticBlock.FACING).getNormal());
		return super.getMovementVector();
	}
	// шаги тировой последовательной сборки: vanilla deploying + meowaddons:deploying_t<=tier
	@Override public net.minecraft.world.item.crafting.RecipeHolder<? extends net.minecraft.world.item.crafting.Recipe<? extends net.minecraft.world.item.crafting.RecipeInput>> getRecipe(net.minecraft.world.item.ItemStack stack){
		net.minecraft.world.level.Level level = getLevel();
		if (level != null && !level.isClientSide) {
			var asm = com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.getRecipe(level, stack, com.simibubi.create.AllRecipeTypes.DEPLOYING.getType(), com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe.class);
			if (asm.isPresent()) return asm.get();
			for (int l = tier.level; l >= 1; l--) {
				@SuppressWarnings("unchecked")
				net.minecraft.world.item.crafting.RecipeType<com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe> asmType = (net.minecraft.world.item.crafting.RecipeType<com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe>)(net.minecraft.world.item.crafting.RecipeType<?>) com.meowaddons.recipe.ModRecipeTypes.deployingTier(Tier.fromLevel(l));
				var asmT = com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.getRecipe(level, stack, asmType, com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe.class);
				if (asmT.isPresent()) return asmT.get();
			}
		}
		return super.getRecipe(stack);
	}
}

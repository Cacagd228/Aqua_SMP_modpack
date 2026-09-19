package com.meowaddons.tier;

import com.meowaddons.ModBlockEntities;
import com.meowaddons.recipe.ModRecipeTypes;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.Optional;

/**
 * Ванильный findRecipe знает только AllRecipeTypes.CRUSHING/MILLING, поэтому
 * tiered-рецепты (meowaddons:crushing_tN / milling_tN) не матчились никогда.
 * Ускорение 4^delta здесь не нужно — оно уже зашит в getSpeed() тирового колеса
 * (crushingspeed = |speed|/50, а processingSpeed пропорционален crushingspeed).
 */
public class TieredCrushingWheelControllerBlockEntity extends CrushingWheelControllerBlockEntity {
	public TieredCrushingWheelControllerBlockEntity(BlockPos pos, BlockState state) {
		this(ModBlockEntities.TIERED_CRUSHING_WHEEL_CONTROLLER.get(), pos, state);
	}

	public TieredCrushingWheelControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> findRecipe() {
		Level lvl = getLevel();
		Tier tier = lvl != null ? tierFromNeighbours() : null;
		ItemStack stack = inventory.getStackInSlot(0);
		if (lvl != null && tier != null && !stack.isEmpty()) {
			SingleRecipeInput input = new SingleRecipeInput(stack);
			RecipeManager manager = lvl.getRecipeManager();
			for (int l = tier.level; l >= 1; l--) {
				Tier rt = Tier.fromLevel(l);
				var crushing = manager.getRecipeFor(ModRecipeTypes.crushingTier(rt), input, lvl);
				if (crushing.isPresent())
					return Optional.of((RecipeHolder) (Object) crushing.get());
				var milling = manager.getRecipeFor(ModRecipeTypes.millingTier(rt), input, lvl);
				if (milling.isPresent())
					return Optional.of((RecipeHolder) (Object) milling.get());
			}
		}
		return super.findRecipe();
	}

	private Tier tierFromNeighbours() {
		Level lvl = getLevel();
		if (lvl == null)
			return null;
		for (Direction d : Iterate.directions) {
			if (lvl.getBlockEntity(worldPosition.relative(d)) instanceof TieredCrushingWheelBlockEntity wheel)
				return wheel.getTier();
		}
		return null;
	}
}

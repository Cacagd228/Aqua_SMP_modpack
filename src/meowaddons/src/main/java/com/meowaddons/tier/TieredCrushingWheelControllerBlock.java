package com.meowaddons.tier;

import com.meowaddons.ModBlockEntities;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import com.mojang.serialization.MapCodec;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Ванильный контроллер в updateSpeed() признаёт только точный блок
 * AllBlocks.CRUSHING_WHEEL.has(neighbour), поэтому рядом с тировыми колёсами
 * crushingspeed навсегда оставался 0 -> tick() и checkEntityForProcessing()
 * выходили сразу (мобы не зажевывались, рецепты не выполнялись).
 * Этот подкласс принимает любые CrushingWheelBlock-соседи (ванильные и тировые).
 */
public class TieredCrushingWheelControllerBlock extends CrushingWheelControllerBlock {
	public static final MapCodec<TieredCrushingWheelControllerBlock> CODEC =
		simpleCodec(TieredCrushingWheelControllerBlock::new);

	public TieredCrushingWheelControllerBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends CrushingWheelControllerBlock> codec() {
		return CODEC;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<CrushingWheelControllerBlockEntity> getBlockEntityClass() {
		return (Class) TieredCrushingWheelControllerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CrushingWheelControllerBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TIERED_CRUSHING_WHEEL_CONTROLLER.get();
	}

	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (type == ModBlockEntities.TIERED_CRUSHING_WHEEL_CONTROLLER.get())
			return (BlockEntityTicker<T>) new SmartBlockEntityTicker<CrushingWheelControllerBlockEntity>();
		return null;
	}

	@Override
	public void updateSpeed(BlockState state, LevelAccessor world, BlockPos pos) {
		withBlockEntityDo(world, pos, be -> {
			if (!state.getValue(VALID)) {
				if (be.crushingspeed != 0) {
					be.crushingspeed = 0;
					be.sendData();
				}
				return;
			}
			for (Direction d : Iterate.directions) {
				BlockState neighbour = world.getBlockState(pos.relative(d));
				if (!(neighbour.getBlock() instanceof CrushingWheelBlock))
					continue;
				if (neighbour.getValue(BlockStateProperties.AXIS) == d.getAxis())
					continue;
				BlockEntity adjBE = world.getBlockEntity(pos.relative(d));
				if (!(adjBE instanceof CrushingWheelBlockEntity cwbe))
					continue;
				float target = Math.abs(cwbe.getSpeed() / 50f);
				if (be.crushingspeed != target) {
					be.crushingspeed = target;
					be.sendData();
				}
				return;
			}
		});
	}

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
		super.neighborChanged(state, world, pos, neighborBlock, neighborPos, isMoving);
		if (world.isClientSide) return;
		// Проверяем, есть ли валидные колёса рядом
		boolean hasValidWheel = false;
		for (Direction d : Iterate.directions) {
			BlockState neighbour = world.getBlockState(pos.relative(d));
			if (!(neighbour.getBlock() instanceof CrushingWheelBlock))
				continue;
			if (neighbour.getValue(BlockStateProperties.AXIS) == d.getAxis())
				continue;
			BlockEntity adjBE = world.getBlockEntity(pos.relative(d));
			if (adjBE instanceof CrushingWheelBlockEntity) {
				hasValidWheel = true;
				break;
			}
		}
		if (!hasValidWheel && state.getValue(VALID)) {
			// Нет колёс — удаляем контроллер
			world.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
		}
	}
}

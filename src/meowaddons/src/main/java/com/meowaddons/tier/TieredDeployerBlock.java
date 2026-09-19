package com.meowaddons.tier;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TieredDeployerBlock extends DeployerBlock {
	private final Tier tier;
	public TieredDeployerBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties props, Tier tier){ super(props); this.tier=tier; }
	public Tier getTier(){ return tier; }
	@Override public Class<DeployerBlockEntity> getBlockEntityClass(){ return (Class)TieredDeployerBlockEntity.class; }
	@Override public BlockEntityType<? extends DeployerBlockEntity> getBlockEntityType(){ return TieredDeployerBlockEntity.getType(tier); }

	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (type == getBlockEntityType())
			return (BlockEntityTicker<T>) new SmartBlockEntityTicker<DeployerBlockEntity>();
		return null;
	}
}

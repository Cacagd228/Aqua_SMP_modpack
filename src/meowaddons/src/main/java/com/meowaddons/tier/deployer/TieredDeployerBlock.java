package com.meowaddons.tier.deployer;
import com.meowaddons.tier.Tier;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public class TieredDeployerBlock extends DeployerBlock {
 private final Tier tier;
 public TieredDeployerBlock(BlockBehaviour.Properties props, Tier tier){ super(props); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public Class<DeployerBlockEntity> getBlockEntityClass(){ return (Class)TieredDeployerBlockEntity.class; }
 @Override public BlockEntityType<? extends DeployerBlockEntity> getBlockEntityType(){ return TieredDeployerBlockEntity.getType(tier); }
}

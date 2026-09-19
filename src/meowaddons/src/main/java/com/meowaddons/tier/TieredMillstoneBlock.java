package com.meowaddons.tier;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlock;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public class TieredMillstoneBlock extends MillstoneBlock {
 private final Tier tier;
 public TieredMillstoneBlock(BlockBehaviour.Properties props, Tier tier){ super(props); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public Class<MillstoneBlockEntity> getBlockEntityClass(){ return (Class)TieredMillstoneBlockEntity.class; }
 @Override public BlockEntityType<? extends MillstoneBlockEntity> getBlockEntityType(){ return TieredMillstoneBlockEntity.getType(tier); }
}

package com.meowaddons.tier;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlock;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public class TieredPressBlock extends MechanicalPressBlock {
 private final Tier tier;
 public TieredPressBlock(BlockBehaviour.Properties props, Tier tier){ super(props); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public Class<MechanicalPressBlockEntity> getBlockEntityClass(){ return (Class)TieredPressBlockEntity.class; }
 @Override public BlockEntityType<? extends MechanicalPressBlockEntity> getBlockEntityType(){ return TieredPressBlockEntity.getType(tier); }
}

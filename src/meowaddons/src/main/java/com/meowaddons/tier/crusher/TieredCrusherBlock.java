package com.meowaddons.tier.crusher;
import com.meowaddons.tier.Tier;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public class TieredCrusherBlock extends CrushingWheelBlock {
 private final Tier tier;
 public TieredCrusherBlock(BlockBehaviour.Properties props, Tier tier){ super(props); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public Class<CrushingWheelBlockEntity> getBlockEntityClass(){ return (Class)TieredCrusherBlockEntity.class; }
 @Override public BlockEntityType<? extends CrushingWheelBlockEntity> getBlockEntityType(){ return TieredCrusherBlockEntity.getType(tier); }
}

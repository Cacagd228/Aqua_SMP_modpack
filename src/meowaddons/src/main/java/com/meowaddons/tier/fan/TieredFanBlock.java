package com.meowaddons.tier.fan;
import com.meowaddons.tier.Tier;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public class TieredFanBlock extends EncasedFanBlock {
 private final Tier tier;
 public TieredFanBlock(BlockBehaviour.Properties props, Tier tier){ super(props); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public Class<EncasedFanBlockEntity> getBlockEntityClass(){ return (Class)TieredFanBlockEntity.class; }
 @Override public BlockEntityType<? extends EncasedFanBlockEntity> getBlockEntityType(){ return TieredFanBlockEntity.getType(tier); }
}

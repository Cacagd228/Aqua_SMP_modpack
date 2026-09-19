package com.meowaddons.tier;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlock;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public class TieredMixerBlock extends MechanicalMixerBlock {
 private final Tier tier;
 public TieredMixerBlock(BlockBehaviour.Properties props, Tier tier){ super(props); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public Class<MechanicalMixerBlockEntity> getBlockEntityClass(){ return (Class)TieredMixerBlockEntity.class; }
 @Override public BlockEntityType<? extends MechanicalMixerBlockEntity> getBlockEntityType(){ return TieredMixerBlockEntity.getType(tier); }
}

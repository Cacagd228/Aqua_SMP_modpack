package com.meowaddons.tier;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public class TieredSawBlock extends SawBlock {
 private final Tier tier;
 public TieredSawBlock(BlockBehaviour.Properties props, Tier tier){ super(props); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public Class<SawBlockEntity> getBlockEntityClass(){ return (Class)TieredSawBlockEntity.class; }
 @Override public BlockEntityType<? extends SawBlockEntity> getBlockEntityType(){ return TieredSawBlockEntity.getType(tier); }
}

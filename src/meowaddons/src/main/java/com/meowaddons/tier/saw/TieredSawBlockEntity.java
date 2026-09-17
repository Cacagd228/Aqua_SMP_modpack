package com.meowaddons.tier.saw;
import com.meowaddons.tier.Tier;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
public class TieredSawBlockEntity extends SawBlockEntity {
 private final Tier tier;
 public TieredSawBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Tier tier){ super(type,pos,state); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking){
  boolean res = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
  tooltip.add(Component.translatable("tooltip.meowaddons.tier", tier.level).withStyle(ChatFormatting.GRAY));
  int speed = tier.speedFactor(Tier.ANDESITE);
  if(speed > 1) tooltip.add(Component.translatable("tooltip.meowaddons.press_speed", speed).withStyle(ChatFormatting.GRAY));
  return true;
 }
 @Override public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking){
  boolean res = super.addToTooltip(tooltip, isPlayerSneaking);
  if(!isPlayerSneaking) tooltip.add(Component.translatable("tooltip.meowaddons.tier", tier.level).withStyle(ChatFormatting.GRAY));
  return res;
 }
 public TieredSawBlockEntity(BlockPos pos, BlockState state, Tier tier){ this(getType(tier),pos,state,tier); }
 public static BlockEntityType<? extends TieredSawBlockEntity> getType(Tier tier){
  return switch(tier){
   case ANDESITE-> com.meowaddons.ModBlockEntities.TIERED_SAW_T1.get();
   case BRASS-> com.meowaddons.ModBlockEntities.TIERED_SAW_T2.get();
   case STEEL-> com.meowaddons.ModBlockEntities.TIERED_SAW_T3.get();
   case SHADOW_STEEL-> com.meowaddons.ModBlockEntities.TIERED_SAW_T4.get();
   case REFINED_RADIANCE-> com.meowaddons.ModBlockEntities.TIERED_SAW_T5.get();
   case CHROMATIC-> com.meowaddons.ModBlockEntities.TIERED_SAW_T6.get();
  };
 }
}

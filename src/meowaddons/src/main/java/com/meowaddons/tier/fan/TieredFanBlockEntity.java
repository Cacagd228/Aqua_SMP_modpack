package com.meowaddons.tier.fan;
import com.meowaddons.ModBlockEntities;
import com.meowaddons.tier.Tier;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
public class TieredFanBlockEntity extends EncasedFanBlockEntity {
 private final Tier tier;
 public TieredFanBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Tier tier){ super(type,pos,state); this.tier=tier; }
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
 public TieredFanBlockEntity(BlockPos pos, BlockState state, Tier tier){ this(getType(tier),pos,state,tier); }
 public static BlockEntityType<? extends TieredFanBlockEntity> getType(Tier tier){
  return switch(tier){
   case ANDESITE-> ModBlockEntities.TIERED_FAN_T1.get();
   case BRASS-> ModBlockEntities.TIERED_FAN_T2.get();
   case STEEL-> ModBlockEntities.TIERED_FAN_T3.get();
   case SHADOW_STEEL-> ModBlockEntities.TIERED_FAN_T4.get();
   case REFINED_RADIANCE-> ModBlockEntities.TIERED_FAN_T5.get();
   case CHROMATIC-> ModBlockEntities.TIERED_FAN_T6.get();
  };
 }
}

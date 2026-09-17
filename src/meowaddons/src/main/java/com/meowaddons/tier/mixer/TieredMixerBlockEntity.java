package com.meowaddons.tier.mixer;
import com.meowaddons.tier.Tier;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
public class TieredMixerBlockEntity extends MechanicalMixerBlockEntity {
 private final Tier tier;
 public TieredMixerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Tier tier){ super(type,pos,state); this.tier=tier; }
 public TieredMixerBlockEntity(BlockPos pos, BlockState state, Tier tier){ this(getType(tier),pos,state,tier); }
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
 public static BlockEntityType<? extends TieredMixerBlockEntity> getType(Tier tier){
  return switch(tier){
   case ANDESITE-> com.meowaddons.ModBlockEntities.TIERED_MIXER_T1.get();
   case BRASS-> com.meowaddons.ModBlockEntities.TIERED_MIXER_T2.get();
   case STEEL-> com.meowaddons.ModBlockEntities.TIERED_MIXER_T3.get();
   case SHADOW_STEEL-> com.meowaddons.ModBlockEntities.TIERED_MIXER_T4.get();
   case REFINED_RADIANCE-> com.meowaddons.ModBlockEntities.TIERED_MIXER_T5.get();
   case CHROMATIC-> com.meowaddons.ModBlockEntities.TIERED_MIXER_T6.get();
  };
 }
}

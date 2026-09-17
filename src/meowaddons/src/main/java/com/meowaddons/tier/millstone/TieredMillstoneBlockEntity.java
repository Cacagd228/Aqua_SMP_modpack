package com.meowaddons.tier.millstone;
import com.meowaddons.tier.Tier;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
public class TieredMillstoneBlockEntity extends MillstoneBlockEntity {
 private final Tier tier;
 public TieredMillstoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Tier tier){ super(type,pos,state); this.tier=tier; }
 public TieredMillstoneBlockEntity(BlockPos pos, BlockState state, Tier tier){ this(getType(tier),pos,state,tier); }
 public Tier getTier(){ return tier; }
 @Override public int getProcessingSpeed(){ int base = super.getProcessingSpeed(); return Math.min(8192, Math.max(1, base * (int)Math.pow(4, getTier().level - 1))); }
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
 public static BlockEntityType<? extends TieredMillstoneBlockEntity> getType(Tier tier){
  return switch(tier){
   case ANDESITE-> com.meowaddons.ModBlockEntities.TIERED_MILLSTONE_T1.get();
   case BRASS-> com.meowaddons.ModBlockEntities.TIERED_MILLSTONE_T2.get();
   case STEEL-> com.meowaddons.ModBlockEntities.TIERED_MILLSTONE_T3.get();
   case SHADOW_STEEL-> com.meowaddons.ModBlockEntities.TIERED_MILLSTONE_T4.get();
   case REFINED_RADIANCE-> com.meowaddons.ModBlockEntities.TIERED_MILLSTONE_T5.get();
   case CHROMATIC-> com.meowaddons.ModBlockEntities.TIERED_MILLSTONE_T6.get();
  };
 }
}

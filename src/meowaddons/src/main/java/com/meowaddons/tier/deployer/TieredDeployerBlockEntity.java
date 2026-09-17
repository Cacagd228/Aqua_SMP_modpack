package com.meowaddons.tier.deployer;
import com.meowaddons.tier.Tier;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.List;
public class TieredDeployerBlockEntity extends DeployerBlockEntity {
 private final Tier tier;
 public TieredDeployerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Tier tier){ super(type,pos,state); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override protected Vec3 getMovementVector(){ return Vec3.atLowerCornerOf(getBlockState().getValue(DirectionalKineticBlock.FACING).getNormal()); }
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
 public TieredDeployerBlockEntity(BlockPos pos, BlockState state, Tier tier){ this(getType(tier),pos,state,tier); }
 public static BlockEntityType<? extends TieredDeployerBlockEntity> getType(Tier tier){
  return switch(tier){
   case ANDESITE-> com.meowaddons.ModBlockEntities.TIERED_DEPLOYER_T1.get();
   case BRASS-> com.meowaddons.ModBlockEntities.TIERED_DEPLOYER_T2.get();
   case STEEL-> com.meowaddons.ModBlockEntities.TIERED_DEPLOYER_T3.get();
   case SHADOW_STEEL-> com.meowaddons.ModBlockEntities.TIERED_DEPLOYER_T4.get();
   case REFINED_RADIANCE-> com.meowaddons.ModBlockEntities.TIERED_DEPLOYER_T5.get();
   case CHROMATIC-> com.meowaddons.ModBlockEntities.TIERED_DEPLOYER_T6.get();
  };
 }
}

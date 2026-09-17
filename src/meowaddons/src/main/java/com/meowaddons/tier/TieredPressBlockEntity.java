package com.meowaddons.tier;
import com.meowaddons.recipe.ModRecipeTypes;
import com.meowaddons.recipe.TieredPressingRecipe;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.crafting.RecipeHolder;
import java.util.List;
 public class TieredPressBlockEntity extends MechanicalPressBlockEntity {
  private final Tier tier;
 public TieredPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Tier tier){ super(type,pos,state); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking){
  boolean res = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
  tooltip.add(Component.translatable("tooltip.meowaddons.press_tier", tier.level).withStyle(ChatFormatting.GRAY));
  // скорость относительно T1, как в Tier.speedFactor
  int speed = tier.speedFactor(Tier.ANDESITE);
  if(speed > 1) tooltip.add(Component.translatable("tooltip.meowaddons.press_speed", speed).withStyle(ChatFormatting.GRAY));
  return true;
 }
 @Override public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking){
  boolean res = super.addToTooltip(tooltip, isPlayerSneaking);
  // Показываем тир и при обычном ховере (без очков) как у Create блоков, дополнительно к стрессу
  if(!isPlayerSneaking) tooltip.add(Component.translatable("tooltip.meowaddons.press_tier", tier.level).withStyle(ChatFormatting.GRAY));
  return res;
 }
 public TieredPressBlockEntity(BlockPos pos, BlockState state, Tier tier){ this(getType(tier),pos,state,tier); }
 public static BlockEntityType<? extends TieredPressBlockEntity> getType(Tier tier){
  return switch(tier){
   case ANDESITE-> com.meowaddons.ModBlockEntities.TIERED_PRESS_T1.get();
   case BRASS-> com.meowaddons.ModBlockEntities.TIERED_PRESS_T2.get();
   case STEEL-> com.meowaddons.ModBlockEntities.TIERED_PRESS_T3.get();
   case SHADOW_STEEL-> com.meowaddons.ModBlockEntities.TIERED_PRESS_T4.get();
   case REFINED_RADIANCE-> com.meowaddons.ModBlockEntities.TIERED_PRESS_T5.get();
   case CHROMATIC-> com.meowaddons.ModBlockEntities.TIERED_PRESS_T6.get();
  };
 }
 @Override public Optional<RecipeHolder<com.simibubi.create.content.kinetics.press.PressingRecipe>> getRecipe(ItemStack stack){
  Level level=getLevel(); if(level==null) return Optional.empty();
  SingleRecipeInput input=new SingleRecipeInput(stack);
  for(int l=tier.level; l>=1; l--){
   Tier rt=Tier.fromLevel(l);
   var type=ModRecipeTypes.pressingTier(rt);
   var opt=level.getRecipeManager().getRecipeFor(type,input,level);
   if(opt.isPresent()){
    var holder=(RecipeHolder<TieredPressingRecipe>)opt.get();
    int base=holder.value().getProcessingDuration();
    int delta=tier.level - rt.level;
    int adj=Math.max(1, (int)(base / Math.pow(4,delta)));
    TieredPressingRecipe adjRecipe=new TieredPressingRecipe(holder.value().getParams(), rt){
     @Override public int getProcessingDuration(){ return adj; }
     @Override public net.minecraft.world.item.crafting.RecipeType<?> getType(){ return type; }
     @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer(){ return holder.value().getSerializer(); }
    };
    RecipeHolder<com.simibubi.create.content.kinetics.press.PressingRecipe> wrap=new RecipeHolder<>(holder.id(), (com.simibubi.create.content.kinetics.press.PressingRecipe)(Object)adjRecipe);
    return Optional.of(wrap);
   }
  }
  var vanilla=level.getRecipeManager().getRecipeFor(com.simibubi.create.AllRecipeTypes.PRESSING.getType(),input,level);
  if(vanilla.isPresent()){
   var holder=vanilla.get();
   int base=((ProcessingRecipe)holder.value()).getProcessingDuration();
   int delta=tier.level - 1;
   int adj=Math.max(1, (int)(base / Math.pow(4,delta)));
   var orig=(ProcessingRecipe)holder.value();
   TieredPressingRecipe adjR=new TieredPressingRecipe(orig.getParams(), Tier.ANDESITE){
    @Override public int getProcessingDuration(){ return adj; }
    @Override public net.minecraft.world.item.crafting.RecipeType<?> getType(){ return holder.value().getType(); }
    @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer(){ return holder.value().getSerializer(); }
   };
   RecipeHolder<com.simibubi.create.content.kinetics.press.PressingRecipe> wrap=new RecipeHolder<>(holder.id(), (com.simibubi.create.content.kinetics.press.PressingRecipe)(Object)adjR);
   return Optional.of(wrap);
  }
  return Optional.empty();
 }
}

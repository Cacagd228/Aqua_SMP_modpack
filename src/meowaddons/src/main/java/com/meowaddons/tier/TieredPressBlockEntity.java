package com.meowaddons.tier;
import com.meowaddons.recipe.ModRecipeTypes;
import com.meowaddons.recipe.TieredPressingRecipe;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
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
   return res;
  }
  @Override public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking){
   boolean res = super.addToTooltip(tooltip, isPlayerSneaking);
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
   var asm=SequencedAssemblyRecipe.getRecipe(level,stack,com.simibubi.create.AllRecipeTypes.PRESSING.getType(),com.simibubi.create.content.kinetics.press.PressingRecipe.class);
   if(asm.isPresent()) return asm;
   for(int l=tier.level; l>=1; l--){
    @SuppressWarnings("unchecked") net.minecraft.world.item.crafting.RecipeType<com.simibubi.create.content.kinetics.press.PressingRecipe> asmType=(net.minecraft.world.item.crafting.RecipeType<com.simibubi.create.content.kinetics.press.PressingRecipe>)(net.minecraft.world.item.crafting.RecipeType<?>)ModRecipeTypes.pressingTier(Tier.fromLevel(l));
    var asmT=SequencedAssemblyRecipe.getRecipe(level,stack,asmType,com.simibubi.create.content.kinetics.press.PressingRecipe.class);
    if(asmT.isPresent()) return asmT;
   }
   SingleRecipeInput input=new SingleRecipeInput(stack);
  for(int l=tier.level; l>=1; l--){
   Tier rt=Tier.fromLevel(l);
   var type=ModRecipeTypes.pressingTier(rt);
   var opt=level.getRecipeManager().getRecipeFor(type,input,level);
   if(opt.isPresent()){
    return opt.map(holder -> new RecipeHolder<>(holder.id(), (com.simibubi.create.content.kinetics.press.PressingRecipe)holder.value()));
   }
  }
var vanilla=level.getRecipeManager().getRecipeFor(com.simibubi.create.AllRecipeTypes.PRESSING.getType(),input,level);
   if(vanilla.isPresent()){
    return vanilla.map(holder -> new RecipeHolder<>(holder.id(), (com.simibubi.create.content.kinetics.press.PressingRecipe)holder.value()));
   }
  return Optional.empty();
  }
}

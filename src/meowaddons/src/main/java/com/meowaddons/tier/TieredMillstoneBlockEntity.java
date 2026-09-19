package com.meowaddons.tier;
import com.meowaddons.recipe.ModRecipeTypes;
import com.meowaddons.recipe.TieredMillingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.meowaddons.config.SpeedFactorConfig;
public class TieredMillstoneBlockEntity extends MillstoneBlockEntity {
  private final Tier tier;
  public TieredMillstoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Tier tier){ super(type,pos,state); this.tier=tier; }
  public Tier getTier(){ return tier; }
  @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking){
   boolean res = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
   return res;
  }
  @Override public boolean addToTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking){
   boolean res = super.addToTooltip(tooltip, isPlayerSneaking);
   return res;
  }
  public TieredMillstoneBlockEntity(BlockPos pos, BlockState state, Tier tier){ this(getType(tier),pos,state,tier); }
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
  public Optional<RecipeHolder<MillingRecipe>> getMillingRecipe(ItemStack stack){
   if (!SpeedFactorConfig.isMillingEnabled()) {
    return getMillingRecipeVanilla(stack);
   }
   Level level=getLevel(); if(level==null) return Optional.empty();
   SingleRecipeInput input=new SingleRecipeInput(stack);
   for(int l=tier.level; l>=1; l--){
    Tier rt=Tier.fromLevel(l);
    var type=ModRecipeTypes.millingTier(rt);
    var opt=level.getRecipeManager().getRecipeFor(type,input,level);
    if(opt.isPresent()){
     var holder=(RecipeHolder<TieredMillingRecipe>)opt.get();
     int base=holder.value().getProcessingDuration();
     int delta=tier.level - rt.level;
     int adj=Math.max(1, (int)(base / tier.speedFactor(rt)));
     TieredMillingRecipe adjRecipe=new TieredMillingRecipe(holder.value().getParams(), rt){
      @Override public int getProcessingDuration(){ return adj; }
      @Override public net.minecraft.world.item.crafting.RecipeType<?> getType(){ return type; }
      @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer(){ return holder.value().getSerializer(); }
     };
     RecipeHolder<MillingRecipe> wrap=new RecipeHolder<>(holder.id(), (MillingRecipe)(Object)adjRecipe);
     return Optional.of(wrap);
    }
   }
   return getMillingRecipeVanilla(stack);
  }

  private Optional<RecipeHolder<MillingRecipe>> getMillingRecipeVanilla(ItemStack stack) {
   Level level=getLevel(); if(level==null) return Optional.empty();
   SingleRecipeInput input=new SingleRecipeInput(stack);
   var vanilla=level.getRecipeManager().getRecipeFor(com.simibubi.create.AllRecipeTypes.MILLING.getType(),input,level);
   if(vanilla.isPresent()){
    var holder=vanilla.get();
    int base=((ProcessingRecipe)holder.value()).getProcessingDuration();
    int delta=tier.level - 1;
    int adj=Math.max(1, (int)(base / tier.speedFactor(Tier.ANDESITE)));
    var orig=(ProcessingRecipe)holder.value();
    TieredMillingRecipe adjR=new TieredMillingRecipe(orig.getParams(), Tier.ANDESITE){
     @Override public int getProcessingDuration(){ return adj; }
     @Override public net.minecraft.world.item.crafting.RecipeType<?> getType(){ return holder.value().getType(); }
     @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer(){ return holder.value().getSerializer(); }
    };
    RecipeHolder<MillingRecipe> wrap=new RecipeHolder<>(holder.id(), (MillingRecipe)(Object)adjR);
    return Optional.of(wrap);
   }
   return Optional.empty();
  }
}

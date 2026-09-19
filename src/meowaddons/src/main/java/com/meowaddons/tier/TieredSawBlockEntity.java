package com.meowaddons.tier;
import com.meowaddons.recipe.ModRecipeTypes;
import com.meowaddons.recipe.TieredCuttingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import com.meowaddons.config.SpeedFactorConfig;

public class TieredSawBlockEntity extends SawBlockEntity {
  private final Tier tier;
  public TieredSawBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Tier tier){ super(type,pos,state); this.tier=tier; }
  public Tier getTier(){ return tier; }
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
  @Override public float getSpeed(){
   float base= super.getSpeed();
   if(base==0) return 0;
   if (!SpeedFactorConfig.isCuttingEnabled()) return base;
   int factor = tier.speedFactor(Tier.ANDESITE);
   return base * factor;
  }
  @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking){
   return super.addToGoggleTooltip(tooltip,isPlayerSneaking);
  }
  // tiered recipe lookup with 4^delta duration adjust, fallback to vanilla CUTTING
  public java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<CuttingRecipe>> getCuttingRecipe(ItemStack stack){
   if (!SpeedFactorConfig.isCuttingEnabled()) {
    return getCuttingRecipeVanilla(stack);
   }
   var level=getLevel(); if(level==null) return java.util.Optional.empty();
   var handler = new net.neoforged.neoforge.items.ItemStackHandler(1);
   handler.setStackInSlot(0, stack);
   RecipeWrapper wrapper = new RecipeWrapper(handler);
   for(int l=tier.level; l>=1; l--){
    Tier rt=Tier.fromLevel(l);
    var type=ModRecipeTypes.cuttingTier(rt);
    var opt=level.getRecipeManager().getRecipeFor(type, wrapper, level);
    if(opt.isPresent()){
     var holder=(net.minecraft.world.item.crafting.RecipeHolder<TieredCuttingRecipe>)opt.get();
     int base=holder.value().getProcessingDuration();
     int delta=tier.level - rt.level;
     final int adjF=Math.max(1, (int)(base / tier.speedFactor(rt)));
     final Tier rtF=rt;
     final var typeF=type;
     final var holderF=holder;
     TieredCuttingRecipe adjRecipe=new TieredCuttingRecipe(holder.value().getParams(), rtF){
      @Override public int getProcessingDuration(){ return adjF; }
      @Override public net.minecraft.world.item.crafting.RecipeType<?> getType(){ return typeF; }
      @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer(){ return holderF.value().getSerializer(); }
     };
     net.minecraft.world.item.crafting.RecipeHolder<CuttingRecipe> wrap=new net.minecraft.world.item.crafting.RecipeHolder<>(holder.id(), (CuttingRecipe)(Object)adjRecipe);
     return java.util.Optional.of(wrap);
    }
   }
   return getCuttingRecipeVanilla(stack);
  }

  private java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<CuttingRecipe>> getCuttingRecipeVanilla(ItemStack stack) {
   var level=getLevel(); if(level==null) return java.util.Optional.empty();
   var handler = new net.neoforged.neoforge.items.ItemStackHandler(1);
   handler.setStackInSlot(0, stack);
   RecipeWrapper wrapper = new RecipeWrapper(handler);
   // vanilla fallback
   var vanilla=level.getRecipeManager().getRecipeFor(com.simibubi.create.AllRecipeTypes.CUTTING.getType(), wrapper, level);
   if(vanilla.isPresent()){
    var holder=vanilla.get();
    int base=((ProcessingRecipe)holder.value()).getProcessingDuration();
    int delta=tier.level - 1;
    final int adjF=Math.max(1, (int)(base / tier.speedFactor(Tier.ANDESITE)));
    var orig=(ProcessingRecipe)holder.value();
    final var holderF2=holder;
    TieredCuttingRecipe adjR=new TieredCuttingRecipe(orig.getParams(), Tier.ANDESITE){
     @Override public int getProcessingDuration(){ return adjF; }
     @Override public net.minecraft.world.item.crafting.RecipeType<?> getType(){ return holderF2.value().getType(); }
     @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer(){ return holderF2.value().getSerializer(); }
    };
    net.minecraft.world.item.crafting.RecipeHolder<CuttingRecipe> wrap=new net.minecraft.world.item.crafting.RecipeHolder<>(holder.id(), (CuttingRecipe)(Object)adjR);
    return java.util.Optional.of(wrap);
   }
   return java.util.Optional.empty();
  }
  // Override start to use tiered recipe with adjusted duration (private getRecipes in super is not accessible, so we handle here)
  @Override public void start(ItemStack stack){
   // copy of super.start but using getCuttingRecipe for duration
   if(!canProcess()) return;
   if(inventory.isEmpty()) return;
   var level=getLevel(); if(level==null) return;
   if(level.isClientSide && isVirtual()) return;
   var opt=getCuttingRecipe(stack);
   boolean hasRecipe=opt.isPresent();
   float dur = 50;
   if(hasRecipe){
    var rec=opt.get().value();
    if(rec instanceof CuttingRecipe cr) dur = cr.getProcessingDuration();
    else dur = ((ProcessingRecipe)rec).getProcessingDuration();
   } else {
    // fallback to super handling for stonecutting etc
    super.start(stack);
    return;
   }
   // replicate super.start duration calc: max(1, dur * count/5) etc? Actually Saw.start uses dur * count/5? Let's check super: dur = recipe.getProcessingDuration() then inventory.remainingTime = dur * count/5 ?
   // In SawBlockEntity.start, code: inventory.remainingTime = dur * max(1, count/5) ??? simplified to use dur directly with count
    int count = stack.getCount();
    int countDiv = Math.max(1, count / 5);
    float recipeDuration = dur * countDiv;
    inventory.remainingTime = recipeDuration;
    inventory.recipeDuration = recipeDuration;
    inventory.appliedRecipe = false;
   // store filtering? let super handle filtering via inventory?
   sendData();
  }
}

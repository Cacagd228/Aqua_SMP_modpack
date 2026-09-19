package com.meowaddons.tier;
import com.meowaddons.recipe.ModRecipeTypes;
import com.meowaddons.recipe.TieredMixingRecipe;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;
import com.meowaddons.config.SpeedFactorConfig;
public class TieredMixerBlockEntity extends MechanicalMixerBlockEntity {
  private final Tier tier;
  public TieredMixerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Tier tier){ super(type,pos,state); this.tier=tier; }
  public Tier getTier(){ return tier; }
  @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking){
   boolean res = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
   return res;
  }
  @Override public boolean addToTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking){
   boolean res = super.addToTooltip(tooltip, isPlayerSneaking);
   return res;
  }
  public TieredMixerBlockEntity(BlockPos pos, BlockState state, Tier tier){ this(getType(tier),pos,state,tier); }
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
  @Override protected boolean matchStaticFilters(net.minecraft.world.item.crafting.RecipeHolder<? extends Recipe<?>> holder){
   var recipe = holder.value();
   for(Tier t : Tier.values()){
    if(t.level > tier.level) continue;
    if(recipe.getType() == ModRecipeTypes.mixingTier(t)) return true;
   }
   return super.matchStaticFilters(holder);
  }
  @Override protected List<Recipe<?>> getMatchingRecipes(){
   List<Recipe<?>> base = super.getMatchingRecipes();
   if(base.isEmpty()) return base;
   if (!SpeedFactorConfig.isMixingEnabled()) {
    return base.stream()
        .filter(r -> {
            Tier recipeTier = null;
            if(r instanceof TieredMixingRecipe tm) recipeTier = tm.getTier();
            else if(r.getType() == com.simibubi.create.AllRecipeTypes.MIXING.getType()) recipeTier = Tier.ANDESITE;
            else return true;
            return recipeTier.level <= tier.level;
        })
        .toList();
   }
   List<Recipe<?>> out = new ArrayList<>(base.size());
   for(Recipe<?> r : base){
    Tier recipeTier = null;
    if(r instanceof TieredMixingRecipe tm) recipeTier = tm.getTier();
    else if(r.getType() == com.simibubi.create.AllRecipeTypes.MIXING.getType()) recipeTier = Tier.ANDESITE;
    else {
     out.add(r);
     continue;
    }
    if(recipeTier.level > tier.level) continue;
    int delta = tier.level - recipeTier.level;
    if(delta == 0){
     out.add(r);
    } else {
     int baseDuration = ((ProcessingRecipe)r).getProcessingDuration();
     final int adjF = Math.max(1, (int)(baseDuration / tier.speedFactor(recipeTier)));
     final Tier rt = recipeTier;
     if(r instanceof TieredMixingRecipe tm){
      TieredMixingRecipe adjRecipe = new TieredMixingRecipe(tm.getParams(), rt){
       @Override public int getProcessingDuration(){ return adjF; }
       @Override public net.minecraft.world.item.crafting.RecipeType<?> getType(){ return ModRecipeTypes.mixingTier(rt); }
       @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer(){ return tm.getSerializer(); }
      };
      out.add(adjRecipe);
     } else if(r instanceof com.simibubi.create.content.kinetics.mixer.MixingRecipe mr){
      TieredMixingRecipe adjR = new TieredMixingRecipe(mr.getParams(), Tier.ANDESITE){
       @Override public int getProcessingDuration(){ return adjF; }
       @Override public net.minecraft.world.item.crafting.RecipeType<?> getType(){ return mr.getType(); }
       @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer(){ return mr.getSerializer(); }
      };
      out.add(adjR);
     } else {
      out.add(r);
     }
    }
   }
   out.sort((a,b)->{
    int ta = (a instanceof TieredMixingRecipe tma ? tma.getTier().level : (a.getType()==com.simibubi.create.AllRecipeTypes.MIXING.getType()?1:0));
    int tb = (b instanceof TieredMixingRecipe tmb ? tmb.getTier().level : (b.getType()==com.simibubi.create.AllRecipeTypes.MIXING.getType()?1:0));
    int cmp = Integer.compare(tb, ta);
    if(cmp!=0) return cmp;
    return Integer.compare(b.getIngredients().size(), a.getIngredients().size());
   });
   return out;
  }
}

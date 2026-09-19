package com.meowaddons.tier;
import com.meowaddons.recipe.ModRecipeTypes;
import com.meowaddons.recipe.TieredCrushingRecipe;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlockEntity;
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
public class TieredCrushingWheelBlockEntity extends CrushingWheelBlockEntity {
  private final Tier tier;
  public TieredCrushingWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Tier tier){ super(type,pos,state); this.tier=tier; }
  public Tier getTier(){ return tier; }
  @Override public float getSpeed(){
   float base = super.getSpeed();
   if (base == 0) return 0;
   if (!SpeedFactorConfig.isCrushingEnabled()) return base;
   int factor = tier.speedFactor(Tier.ANDESITE);
   return base * factor;
  }
  @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking){
   boolean res = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
   return res;
  }
  @Override public boolean addToTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking){
   boolean res = super.addToTooltip(tooltip, isPlayerSneaking);
   return res;
  }
  public TieredCrushingWheelBlockEntity(BlockPos pos, BlockState state, Tier tier){ this(getType(tier),pos,state,tier); }
  public static BlockEntityType<? extends TieredCrushingWheelBlockEntity> getType(Tier tier){
   return switch(tier){
    case ANDESITE-> com.meowaddons.ModBlockEntities.TIERED_CRUSHING_WHEEL_T1.get();
    case BRASS-> com.meowaddons.ModBlockEntities.TIERED_CRUSHING_WHEEL_T2.get();
    case STEEL-> com.meowaddons.ModBlockEntities.TIERED_CRUSHING_WHEEL_T3.get();
    case SHADOW_STEEL-> com.meowaddons.ModBlockEntities.TIERED_CRUSHING_WHEEL_T4.get();
    case REFINED_RADIANCE-> com.meowaddons.ModBlockEntities.TIERED_CRUSHING_WHEEL_T5.get();
    case CHROMATIC-> com.meowaddons.ModBlockEntities.TIERED_CRUSHING_WHEEL_T6.get();
   };
  }
  // Для тированного дробления: ищем рецепт среди crushing_t1..crushing_tN с ускорением 4^delta (как у пресса). Пока не используется контроллером ванили, но готов для будущего контроллера/прямого дробления
  public Optional<RecipeHolder<com.simibubi.create.content.kinetics.crusher.CrushingRecipe>> getCrushingRecipe(ItemStack stack){
   if (!SpeedFactorConfig.isCrushingEnabled()) {
    return getCrushingRecipeVanilla(stack);
   }
   Level level=getLevel(); if(level==null) return Optional.empty();
   SingleRecipeInput input=new SingleRecipeInput(stack);
   for(int l=tier.level; l>=1; l--){
    Tier rt=Tier.fromLevel(l);
    var type=ModRecipeTypes.crushingTier(rt);
    var opt=level.getRecipeManager().getRecipeFor(type,input,level);
    if(opt.isPresent()){
     var holder=(RecipeHolder<TieredCrushingRecipe>)opt.get();
     int base=holder.value().getProcessingDuration();
     int delta=tier.level - rt.level;
     int adj=Math.max(1, (int)(base / tier.speedFactor(rt)));
     TieredCrushingRecipe adjRecipe=new TieredCrushingRecipe(holder.value().getParams(), rt){
      @Override public int getProcessingDuration(){ return adj; }
      @Override public net.minecraft.world.item.crafting.RecipeType<?> getType(){ return type; }
      @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer(){ return holder.value().getSerializer(); }
     };
     RecipeHolder<com.simibubi.create.content.kinetics.crusher.CrushingRecipe> wrap=new RecipeHolder<>(holder.id(), (com.simibubi.create.content.kinetics.crusher.CrushingRecipe)(Object)adjRecipe);
     return Optional.of(wrap);
    }
   }
   return getCrushingRecipeVanilla(stack);
  }

  private Optional<RecipeHolder<com.simibubi.create.content.kinetics.crusher.CrushingRecipe>> getCrushingRecipeVanilla(ItemStack stack) {
   Level level=getLevel(); if(level==null) return Optional.empty();
   SingleRecipeInput input=new SingleRecipeInput(stack);
   var vanilla=level.getRecipeManager().getRecipeFor(com.simibubi.create.AllRecipeTypes.CRUSHING.getType(),input,level);
   if(vanilla.isPresent()){
    var holder=vanilla.get();
    int base=((ProcessingRecipe)holder.value()).getProcessingDuration();
    int delta=tier.level - 1;
    int adj=Math.max(1, (int)(base / tier.speedFactor(Tier.ANDESITE)));
    var orig=(ProcessingRecipe)holder.value();
    TieredCrushingRecipe adjR=new TieredCrushingRecipe(orig.getParams(), Tier.ANDESITE){
     @Override public int getProcessingDuration(){ return adj; }
     @Override public net.minecraft.world.item.crafting.RecipeType<?> getType(){ return holder.value().getType(); }
     @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer(){ return holder.value().getSerializer(); }
    };
    RecipeHolder<com.simibubi.create.content.kinetics.crusher.CrushingRecipe> wrap=new RecipeHolder<>(holder.id(), (com.simibubi.create.content.kinetics.crusher.CrushingRecipe)(Object)adjR);
    return Optional.of(wrap);
   }
   return Optional.empty();
  }
}

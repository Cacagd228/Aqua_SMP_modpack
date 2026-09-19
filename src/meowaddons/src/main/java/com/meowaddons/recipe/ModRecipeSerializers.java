package com.meowaddons.recipe;
import com.meowaddons.MeowAddons;
import com.meowaddons.tier.Tier;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public class ModRecipeSerializers {
 public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER, MeowAddons.MODID);
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredPressingRecipe>> PRESSING_T1=SERIALIZERS.register("pressing_t1",()->new TieredPressingSerializer(Tier.ANDESITE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredPressingRecipe>> PRESSING_T2=SERIALIZERS.register("pressing_t2",()->new TieredPressingSerializer(Tier.BRASS));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredPressingRecipe>> PRESSING_T3=SERIALIZERS.register("pressing_t3",()->new TieredPressingSerializer(Tier.STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredPressingRecipe>> PRESSING_T4=SERIALIZERS.register("pressing_t4",()->new TieredPressingSerializer(Tier.SHADOW_STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredPressingRecipe>> PRESSING_T5=SERIALIZERS.register("pressing_t5",()->new TieredPressingSerializer(Tier.REFINED_RADIANCE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredPressingRecipe>> PRESSING_T6=SERIALIZERS.register("pressing_t6",()->new TieredPressingSerializer(Tier.CHROMATIC));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredCrushingRecipe>> CRUSHING_T1=SERIALIZERS.register("crushing_t1",()->new TieredCrushingSerializer(Tier.ANDESITE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredCrushingRecipe>> CRUSHING_T2=SERIALIZERS.register("crushing_t2",()->new TieredCrushingSerializer(Tier.BRASS));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredCrushingRecipe>> CRUSHING_T3=SERIALIZERS.register("crushing_t3",()->new TieredCrushingSerializer(Tier.STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredCrushingRecipe>> CRUSHING_T4=SERIALIZERS.register("crushing_t4",()->new TieredCrushingSerializer(Tier.SHADOW_STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredCrushingRecipe>> CRUSHING_T5=SERIALIZERS.register("crushing_t5",()->new TieredCrushingSerializer(Tier.REFINED_RADIANCE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredCrushingRecipe>> CRUSHING_T6=SERIALIZERS.register("crushing_t6",()->new TieredCrushingSerializer(Tier.CHROMATIC));
 public static RecipeSerializer<TieredPressingRecipe> pressingTier(Tier t){
  return switch(t){
   case ANDESITE->PRESSING_T1.get();
   case BRASS->PRESSING_T2.get();
   case STEEL->PRESSING_T3.get();
   case SHADOW_STEEL->PRESSING_T4.get();
   case REFINED_RADIANCE->PRESSING_T5.get();
   case CHROMATIC->PRESSING_T6.get();
  };
 }
 public static RecipeSerializer<TieredCrushingRecipe> crushingTier(Tier t){
  return switch(t){
   case ANDESITE->CRUSHING_T1.get();
   case BRASS->CRUSHING_T2.get();
   case STEEL->CRUSHING_T3.get();
   case SHADOW_STEEL->CRUSHING_T4.get();
   case REFINED_RADIANCE->CRUSHING_T5.get();
   case CHROMATIC->CRUSHING_T6.get();
  };
 }
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredMillingRecipe>> MILLING_T1=SERIALIZERS.register("milling_t1",()->new TieredMillingSerializer(Tier.ANDESITE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredMillingRecipe>> MILLING_T2=SERIALIZERS.register("milling_t2",()->new TieredMillingSerializer(Tier.BRASS));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredMillingRecipe>> MILLING_T3=SERIALIZERS.register("milling_t3",()->new TieredMillingSerializer(Tier.STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredMillingRecipe>> MILLING_T4=SERIALIZERS.register("milling_t4",()->new TieredMillingSerializer(Tier.SHADOW_STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredMillingRecipe>> MILLING_T5=SERIALIZERS.register("milling_t5",()->new TieredMillingSerializer(Tier.REFINED_RADIANCE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredMillingRecipe>> MILLING_T6=SERIALIZERS.register("milling_t6",()->new TieredMillingSerializer(Tier.CHROMATIC));
 public static RecipeSerializer<TieredMillingRecipe> millingTier(Tier t){
  return switch(t){
   case ANDESITE->MILLING_T1.get();
   case BRASS->MILLING_T2.get();
   case STEEL->MILLING_T3.get();
   case SHADOW_STEEL->MILLING_T4.get();
   case REFINED_RADIANCE->MILLING_T5.get();
   case CHROMATIC->MILLING_T6.get();
  };
 }
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredMixingRecipe>> MIXING_T1=SERIALIZERS.register("mixing_t1",()->new TieredMixingSerializer(Tier.ANDESITE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredMixingRecipe>> MIXING_T2=SERIALIZERS.register("mixing_t2",()->new TieredMixingSerializer(Tier.BRASS));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredMixingRecipe>> MIXING_T3=SERIALIZERS.register("mixing_t3",()->new TieredMixingSerializer(Tier.STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredMixingRecipe>> MIXING_T4=SERIALIZERS.register("mixing_t4",()->new TieredMixingSerializer(Tier.SHADOW_STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredMixingRecipe>> MIXING_T5=SERIALIZERS.register("mixing_t5",()->new TieredMixingSerializer(Tier.REFINED_RADIANCE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredMixingRecipe>> MIXING_T6=SERIALIZERS.register("mixing_t6",()->new TieredMixingSerializer(Tier.CHROMATIC));
 public static RecipeSerializer<TieredMixingRecipe> mixingTier(Tier t){
  return switch(t){
   case ANDESITE->MIXING_T1.get();
   case BRASS->MIXING_T2.get();
   case STEEL->MIXING_T3.get();
   case SHADOW_STEEL->MIXING_T4.get();
   case REFINED_RADIANCE->MIXING_T5.get();
   case CHROMATIC->MIXING_T6.get();
  };
 }
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredCuttingRecipe>> CUTTING_T1=SERIALIZERS.register("cutting_t1",()->new TieredCuttingSerializer(Tier.ANDESITE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredCuttingRecipe>> CUTTING_T2=SERIALIZERS.register("cutting_t2",()->new TieredCuttingSerializer(Tier.BRASS));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredCuttingRecipe>> CUTTING_T3=SERIALIZERS.register("cutting_t3",()->new TieredCuttingSerializer(Tier.STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredCuttingRecipe>> CUTTING_T4=SERIALIZERS.register("cutting_t4",()->new TieredCuttingSerializer(Tier.SHADOW_STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredCuttingRecipe>> CUTTING_T5=SERIALIZERS.register("cutting_t5",()->new TieredCuttingSerializer(Tier.REFINED_RADIANCE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredCuttingRecipe>> CUTTING_T6=SERIALIZERS.register("cutting_t6",()->new TieredCuttingSerializer(Tier.CHROMATIC));
 public static RecipeSerializer<TieredCuttingRecipe> cuttingTier(Tier t){
  return switch(t){
   case ANDESITE->CUTTING_T1.get();
   case BRASS->CUTTING_T2.get();
   case STEEL->CUTTING_T3.get();
   case SHADOW_STEEL->CUTTING_T4.get();
   case REFINED_RADIANCE->CUTTING_T5.get();
   case CHROMATIC->CUTTING_T6.get();
  };
 }
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredDeployerApplicationRecipe>> DEPLOYING_T1=SERIALIZERS.register("deploying_t1",()->new TieredDeployingSerializer(Tier.ANDESITE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredDeployerApplicationRecipe>> DEPLOYING_T2=SERIALIZERS.register("deploying_t2",()->new TieredDeployingSerializer(Tier.BRASS));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredDeployerApplicationRecipe>> DEPLOYING_T3=SERIALIZERS.register("deploying_t3",()->new TieredDeployingSerializer(Tier.STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredDeployerApplicationRecipe>> DEPLOYING_T4=SERIALIZERS.register("deploying_t4",()->new TieredDeployingSerializer(Tier.SHADOW_STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredDeployerApplicationRecipe>> DEPLOYING_T5=SERIALIZERS.register("deploying_t5",()->new TieredDeployingSerializer(Tier.REFINED_RADIANCE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredDeployerApplicationRecipe>> DEPLOYING_T6=SERIALIZERS.register("deploying_t6",()->new TieredDeployingSerializer(Tier.CHROMATIC));
 public static RecipeSerializer<TieredDeployerApplicationRecipe> deployingTier(Tier t){
  return switch(t){
   case ANDESITE->DEPLOYING_T1.get();
   case BRASS->DEPLOYING_T2.get();
   case STEEL->DEPLOYING_T3.get();
   case SHADOW_STEEL->DEPLOYING_T4.get();
   case REFINED_RADIANCE->DEPLOYING_T5.get();
   case CHROMATIC->DEPLOYING_T6.get();
  };
 }
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredSequencedAssemblyRecipe>> SEQUENCED_ASSEMBLY_T1=SERIALIZERS.register("sequenced_assembly_t1",()->new TieredSequencedAssemblyRecipeSerializer(Tier.ANDESITE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredSequencedAssemblyRecipe>> SEQUENCED_ASSEMBLY_T2=SERIALIZERS.register("sequenced_assembly_t2",()->new TieredSequencedAssemblyRecipeSerializer(Tier.BRASS));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredSequencedAssemblyRecipe>> SEQUENCED_ASSEMBLY_T3=SERIALIZERS.register("sequenced_assembly_t3",()->new TieredSequencedAssemblyRecipeSerializer(Tier.STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredSequencedAssemblyRecipe>> SEQUENCED_ASSEMBLY_T4=SERIALIZERS.register("sequenced_assembly_t4",()->new TieredSequencedAssemblyRecipeSerializer(Tier.SHADOW_STEEL));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredSequencedAssemblyRecipe>> SEQUENCED_ASSEMBLY_T5=SERIALIZERS.register("sequenced_assembly_t5",()->new TieredSequencedAssemblyRecipeSerializer(Tier.REFINED_RADIANCE));
 public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TieredSequencedAssemblyRecipe>> SEQUENCED_ASSEMBLY_T6=SERIALIZERS.register("sequenced_assembly_t6",()->new TieredSequencedAssemblyRecipeSerializer(Tier.CHROMATIC));
 public static RecipeSerializer<TieredSequencedAssemblyRecipe> sequencedAssemblyTier(Tier t){
  return switch(t){
   case ANDESITE->SEQUENCED_ASSEMBLY_T1.get();
   case BRASS->SEQUENCED_ASSEMBLY_T2.get();
   case STEEL->SEQUENCED_ASSEMBLY_T3.get();
   case SHADOW_STEEL->SEQUENCED_ASSEMBLY_T4.get();
   case REFINED_RADIANCE->SEQUENCED_ASSEMBLY_T5.get();
   case CHROMATIC->SEQUENCED_ASSEMBLY_T6.get();
  };
 }
 public static void register(IEventBus b){ SERIALIZERS.register(b); }
 public static class TieredPressingSerializer implements RecipeSerializer<TieredPressingRecipe> {
  private final Tier tier;
  private final MapCodec<TieredPressingRecipe> codec;
  private final StreamCodec<RegistryFriendlyByteBuf,TieredPressingRecipe> streamCodec;
  public TieredPressingSerializer(Tier tier){
   this.tier=tier;
   this.codec=TieredPressingRecipe.codec(tier);
   this.streamCodec=TieredPressingRecipe.streamCodec(tier);
  }
  @Override public MapCodec<TieredPressingRecipe> codec(){ return codec; }
  @Override public StreamCodec<RegistryFriendlyByteBuf,TieredPressingRecipe> streamCodec(){ return streamCodec; }
 }
 public static class TieredCrushingSerializer implements RecipeSerializer<TieredCrushingRecipe> {
  private final Tier tier;
  private final MapCodec<TieredCrushingRecipe> codec;
  private final StreamCodec<RegistryFriendlyByteBuf,TieredCrushingRecipe> streamCodec;
  public TieredCrushingSerializer(Tier tier){
   this.tier=tier;
   this.codec=TieredCrushingRecipe.codec(tier);
   this.streamCodec=TieredCrushingRecipe.streamCodec(tier);
  }
  @Override public MapCodec<TieredCrushingRecipe> codec(){ return codec; }
  @Override public StreamCodec<RegistryFriendlyByteBuf,TieredCrushingRecipe> streamCodec(){ return streamCodec; }
 }
 public static class TieredMillingSerializer implements RecipeSerializer<TieredMillingRecipe> {
  private final Tier tier;
  private final MapCodec<TieredMillingRecipe> codec;
  private final StreamCodec<RegistryFriendlyByteBuf,TieredMillingRecipe> streamCodec;
  public TieredMillingSerializer(Tier tier){
   this.tier=tier;
   this.codec=TieredMillingRecipe.codec(tier);
   this.streamCodec=TieredMillingRecipe.streamCodec(tier);
  }
  @Override public MapCodec<TieredMillingRecipe> codec(){ return codec; }
  @Override public StreamCodec<RegistryFriendlyByteBuf,TieredMillingRecipe> streamCodec(){ return streamCodec; }
 }
 public static class TieredMixingSerializer implements RecipeSerializer<TieredMixingRecipe> {
  private final Tier tier;
  private final MapCodec<TieredMixingRecipe> codec;
  private final StreamCodec<RegistryFriendlyByteBuf,TieredMixingRecipe> streamCodec;
  public TieredMixingSerializer(Tier tier){
   this.tier=tier;
   this.codec=TieredMixingRecipe.codec(tier);
   this.streamCodec=TieredMixingRecipe.streamCodec(tier);
  }
  @Override public MapCodec<TieredMixingRecipe> codec(){ return codec; }
  @Override public StreamCodec<RegistryFriendlyByteBuf,TieredMixingRecipe> streamCodec(){ return streamCodec; }
 }
  public static class TieredCuttingSerializer implements RecipeSerializer<TieredCuttingRecipe> {
   private final Tier tier;
   private final MapCodec<TieredCuttingRecipe> codec;
   private final StreamCodec<RegistryFriendlyByteBuf,TieredCuttingRecipe> streamCodec;
   public TieredCuttingSerializer(Tier tier){
    this.tier=tier;
    this.codec=TieredCuttingRecipe.codec(tier);
    this.streamCodec=TieredCuttingRecipe.streamCodec(tier);
   }
   @Override public MapCodec<TieredCuttingRecipe> codec(){ return codec; }
   @Override public StreamCodec<RegistryFriendlyByteBuf,TieredCuttingRecipe> streamCodec(){ return streamCodec; }
  }
  public static class TieredDeployingSerializer implements RecipeSerializer<TieredDeployerApplicationRecipe> {
   private final Tier tier;
   private final MapCodec<TieredDeployerApplicationRecipe> codec;
   private final StreamCodec<RegistryFriendlyByteBuf,TieredDeployerApplicationRecipe> streamCodec;
   public TieredDeployingSerializer(Tier tier){
    this.tier=tier;
    this.codec=TieredDeployerApplicationRecipe.codec(tier);
    this.streamCodec=TieredDeployerApplicationRecipe.streamCodec(tier);
   }
   @Override public MapCodec<TieredDeployerApplicationRecipe> codec(){ return codec; }
   @Override public StreamCodec<RegistryFriendlyByteBuf,TieredDeployerApplicationRecipe> streamCodec(){ return streamCodec; }
  }
}

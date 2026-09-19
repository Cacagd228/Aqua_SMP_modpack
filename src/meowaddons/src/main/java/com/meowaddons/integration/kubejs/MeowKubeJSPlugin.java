package com.meowaddons.integration.kubejs;
import com.meowaddons.MeowAddons;
import com.meowaddons.tier.Tier;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ComponentRole;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentTypeRegistry;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeOptional;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
public class MeowKubeJSPlugin implements KubeJSPlugin {
 public static ResourceLocation id(String name){ return ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, name); }
	// Create ProcessingOutput JSON ({id, count, chance}) as-is через vanilla codec
	public static final RecipeComponentType<ProcessingOutput> PROCESSING_OUTPUT = RecipeComponentType.unit(id("processing_output"), t -> new KubeCodecComponent<>(t, ProcessingOutput.CODEC_NEW, ProcessingOutput.class, true));
	// шаг сборки: весь вложенный рецепт (type+ingredients+results) через SequencedRecipe.CODEC
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static final RecipeComponentType<SequencedRecipe<?>> SEQUENCED_STEP = RecipeComponentType.unit(id("sequenced_step"), t -> new KubeCodecComponent(t, (com.mojang.serialization.Codec) SequencedRecipe.CODEC, SequencedRecipe.class, false));
 private static RecipeKey<List<Ingredient>> ingInput(String name){
  return IngredientComponent.INGREDIENT.instance().asListOrSelf().inputKey(name);
 }
 private static RecipeKey<List<ProcessingOutput>> outList(String name){
  return PROCESSING_OUTPUT.instance().asListOrSelf().outputKey(name);
 }
 @Override public void registerRecipeComponents(RecipeComponentTypeRegistry registry){
  registry.register(PROCESSING_OUTPUT);
  registry.register(SEQUENCED_STEP);
 }
 @Override public void registerRecipeSchemas(RecipeSchemaRegistry registry){
  RecipeKey<Integer> duration = NumberComponent.NON_NEGATIVE_INT.key("processing_time", ComponentRole.OTHER);
  RecipeKey<String> heat = StringComponent.STRING.key("heat_requirement", ComponentRole.OTHER);
  RecipeKey<Integer> loops = NumberComponent.POSITIVE_INT.key("loops", ComponentRole.OTHER);
  for(Tier tier : Tier.values()){
    int n = tier.level;
    for(String cat : List.of("pressing", "crushing", "milling", "mixing", "cutting", "deploying")){
     RecipeKey<List<Ingredient>> in = ingInput("ingredients");
     RecipeKey<List<ProcessingOutput>> out = outList("results");
     // ключи exposed по возможностям родительских рецептов Create:
     // processing_time разрешён crushing/milling/cutting/mixing, heat_requirement — только mixing
     Map<RecipeKey<?>, RecipeOptional<?>> optional = new LinkedHashMap<>();
     List<RecipeKey<?>> keys = new java.util.ArrayList<>();
     keys.add(in); keys.add(out);
     if(cat.equals("crushing") || cat.equals("milling") || cat.equals("cutting") || cat.equals("mixing")){
      optional.put(duration, RecipeOptional.unit(0));
      keys.add(duration);
     }
     if(cat.equals("mixing")){
      optional.put(heat, RecipeOptional.unit("none"));
      keys.add(heat);
     }
     registry.register(id(cat + "_t" + n), new RecipeSchema(optional, keys).constructor(in, out));
    }
   // последовательная сборка: ingredient + transitional_item + sequence + results + loops
   RecipeKey<Ingredient> base = IngredientComponent.INGREDIENT.inputKey("ingredient");
   RecipeKey<ProcessingOutput> transitional = PROCESSING_OUTPUT.instance().key("transitional_item", ComponentRole.OTHER);
   RecipeKey<List<SequencedRecipe<?>>> sequence = SEQUENCED_STEP.instance().asList().key("sequence", ComponentRole.OTHER);
   RecipeKey<List<ProcessingOutput>> results = PROCESSING_OUTPUT.instance().asListOrSelf().outputKey("results");
   Map<RecipeKey<?>, RecipeOptional<?>> asmOptional = new LinkedHashMap<>();
    asmOptional.put(transitional, RecipeOptional.unit(ProcessingOutput.EMPTY));
    asmOptional.put(loops, RecipeOptional.unit(1));
    registry.register(id("sequenced_assembly_t" + n), new RecipeSchema(asmOptional, List.of(base, transitional, sequence, results, loops)).constructor(base, sequence, results));
  }
 }
}

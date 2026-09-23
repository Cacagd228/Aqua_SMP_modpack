package com.meowaddons.integration.jei;
import com.meowaddons.MeowAddons;
import com.meowaddons.ModBlocks;
import com.meowaddons.recipe.ModRecipeTypes;
import com.meowaddons.tier.Tier;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory.Builder;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory.Factory;
import com.simibubi.create.compat.jei.category.CrushingCategory;
import com.simibubi.create.compat.jei.category.DeployingCategory;
import com.simibubi.create.compat.jei.category.MillingCategory;
import com.simibubi.create.compat.jei.category.MixingCategory;
import com.simibubi.create.compat.jei.category.PressingCategory;
import com.simibubi.create.compat.jei.category.SawingCategory;
import com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.Block;
import java.util.ArrayList;
import java.util.List;
// Категории Create для тировых типов (title-ключ = meowaddons.recipe.<тип>_t<тир>, см. CreateRecipeCategory.Builder.build).
// Тип сборки не регистрируется: TieredSequencedAssemblyRecipe extends SequencedAssemblyRecipe и уже
// попадает в категорию create:sequenced_assembly; шаги тировых рецептов рисуют иконки
// тировых механизмов через TieredAssemblySteps (см. getJEISubCategory тировых рецептов).
@JeiPlugin
public class MeowJeiPlugin implements IModPlugin {
	private final List<CreateRecipeCategory<?>> all = new ArrayList<>();
	@Override public ResourceLocation getPluginUid(){ return id("jei_plugin"); }
	private static ResourceLocation id(String p){ return ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, p); }
	@Override public void registerCategories(IRecipeCategoryRegistration r){ load(); r.addRecipeCategories(all.toArray(new IRecipeCategory[0])); }
	@Override public void registerRecipes(IRecipeRegistration r){ all.forEach(c -> c.registerRecipes(r)); }
	@Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){ all.forEach(c -> c.registerCatalysts(r)); }
	private void load(){
		all.clear();
		for(Tier t : Tier.values()){
			int n = t.level;
			builder(PressingRecipe.class).addTypedRecipes(() -> ModRecipeTypes.pressingTier(t))
				.catalyst(() -> press(t)).itemIcon(press(t))
				.emptyBackground(177, 70).build(id("pressing_t" + n), PressingCategory::new);
			builder(AbstractCrushingRecipe.class).addTypedRecipes(() -> ModRecipeTypes.crushingTier(t))
				.catalyst(() -> crush(t)).itemIcon(crush(t))
				.emptyBackground(177, 100).build(id("crushing_t" + n), CrushingCategory::new);
			builder(AbstractCrushingRecipe.class).addTypedRecipes(() -> ModRecipeTypes.millingTier(t))
				.catalyst(() -> mill(t)).itemIcon(mill(t))
				.emptyBackground(177, 53).build(id("milling_t" + n), MillingCategory::new);
			builder(BasinRecipe.class).addTypedRecipes(() -> ModRecipeTypes.mixingTier(t))
				.catalyst(() -> mix(t)).itemIcon(mix(t))
				.emptyBackground(177, 103).build(id("mixing_t" + n), MixingCategory::standard);
			builder(CuttingRecipe.class).addTypedRecipes(() -> ModRecipeTypes.cuttingTier(t))
				.catalyst(() -> saw(t)).itemIcon(saw(t))
				.emptyBackground(177, 70).build(id("cutting_t" + n), SawingCategory::new);
			builder(DeployerApplicationRecipe.class).addTypedRecipes(() -> ModRecipeTypes.deployingTier(t))
				.catalyst(() -> deploy(t)).itemIcon(deploy(t))
				.emptyBackground(177, 70).build(id("deploying_t" + n), DeployingCategory::new);
		}
	}
	public static Block press(Tier t){ return switch(t){ case ANDESITE->ModBlocks.PRESS_T1.get(); case BRASS->ModBlocks.PRESS_T2.get(); case STEEL->ModBlocks.PRESS_T3.get(); case SHADOW_STEEL->ModBlocks.PRESS_T4.get(); case REFINED_RADIANCE->ModBlocks.PRESS_T5.get(); case CHROMATIC->ModBlocks.PRESS_T6.get(); }; }
	private static Block crush(Tier t){ return switch(t){ case ANDESITE->ModBlocks.CRUSHING_WHEEL_T1.get(); case BRASS->ModBlocks.CRUSHING_WHEEL_T2.get(); case STEEL->ModBlocks.CRUSHING_WHEEL_T3.get(); case SHADOW_STEEL->ModBlocks.CRUSHING_WHEEL_T4.get(); case REFINED_RADIANCE->ModBlocks.CRUSHING_WHEEL_T5.get(); case CHROMATIC->ModBlocks.CRUSHING_WHEEL_T6.get(); }; }
	private static Block mill(Tier t){ return switch(t){ case ANDESITE->ModBlocks.MILLSTONE_T1.get(); case BRASS->ModBlocks.MILLSTONE_T2.get(); case STEEL->ModBlocks.MILLSTONE_T3.get(); case SHADOW_STEEL->ModBlocks.MILLSTONE_T4.get(); case REFINED_RADIANCE->ModBlocks.MILLSTONE_T5.get(); case CHROMATIC->ModBlocks.MILLSTONE_T6.get(); }; }
	private static Block mix(Tier t){ return switch(t){ case ANDESITE->ModBlocks.MIXER_T1.get(); case BRASS->ModBlocks.MIXER_T2.get(); case STEEL->ModBlocks.MIXER_T3.get(); case SHADOW_STEEL->ModBlocks.MIXER_T4.get(); case REFINED_RADIANCE->ModBlocks.MIXER_T5.get(); case CHROMATIC->ModBlocks.MIXER_T6.get(); }; }
	public static Block saw(Tier t){ return switch(t){ case ANDESITE->ModBlocks.SAW_T1.get(); case BRASS->ModBlocks.SAW_T2.get(); case STEEL->ModBlocks.SAW_T3.get(); case SHADOW_STEEL->ModBlocks.SAW_T4.get(); case REFINED_RADIANCE->ModBlocks.SAW_T5.get(); case CHROMATIC->ModBlocks.SAW_T6.get(); }; }
	public static Block deploy(Tier t){ return switch(t){ case ANDESITE->ModBlocks.DEPLOYER_T1.get(); case BRASS->ModBlocks.DEPLOYER_T2.get(); case STEEL->ModBlocks.DEPLOYER_T3.get(); case SHADOW_STEEL->ModBlocks.DEPLOYER_T4.get(); case REFINED_RADIANCE->ModBlocks.DEPLOYER_T5.get(); case CHROMATIC->ModBlocks.DEPLOYER_T6.get(); }; }
	private <T extends Recipe<? extends RecipeInput>> Builder<T> builder(Class<T> c){ return new CategoryBuilder<>(c); }
	@SuppressWarnings({"rawtypes"})
	private class CategoryBuilder<T extends Recipe<?>> extends Builder<T>{
		CategoryBuilder(Class<? extends T> recipeClass){ super(recipeClass); }
		@Override public CreateRecipeCategory<T> build(ResourceLocation id, Factory<T> factory){
			CreateRecipeCategory<T> cat = super.build(id, factory);
			all.add(cat);
			return cat;
		}
	}
}

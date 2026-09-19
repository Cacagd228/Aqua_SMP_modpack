package com.meowaddons.recipe;
import com.meowaddons.tier.Tier;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import java.lang.reflect.Method;
import java.util.List;
public class TieredSequencedAssemblyRecipe extends SequencedAssemblyRecipe implements TieredRecipe {
	// SequencedRecipe.initFromSequencedAssembly package-private в Create — вызываем через reflection,
	// иначе шаги не подменяют ingredient[0] на составной (input | transitional) и сборка не матчится
	private static final Method INIT_STEP;
	static {
		try {
			INIT_STEP = SequencedRecipe.class.getDeclaredMethod("initFromSequencedAssembly", SequencedAssemblyRecipe.class, boolean.class);
			INIT_STEP.setAccessible(true);
		} catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
	}
	private final Tier tier;
	public TieredSequencedAssemblyRecipe(Tier tier){ super(null); this.tier=tier; }
	@Override public Tier getTier(){ return tier; }
	// базовый getTransitionalItem() возвращает ItemStack, кодек-геттеры:
	public ProcessingOutput getTransitionalOutput(){ return transitionalItem; }
	public List<ProcessingOutput> getResults(){ return resultPool; }
	// getType() НЕ переопределяем: базовый возвращает vanilla create:sequenced_assembly —
	// поиск сборки в машинах зашит в getAllRecipesFor(SEQUENCED_ASSEMBLY)
	@Override public RecipeSerializer<?> getSerializer(){ return ModRecipeSerializers.sequencedAssemblyTier(tier); }
	public void apply(Ingredient ingredient, ProcessingOutput transitional, List<SequencedRecipe<?>> sequence, List<ProcessingOutput> results, int loops){
		this.ingredient = ingredient;
		this.transitionalItem = transitional;
		this.loops = loops;
		this.sequence.clear();
		this.sequence.addAll(sequence);
		this.resultPool.clear();
		this.resultPool.addAll(results);
		for (int i = 0; i < this.sequence.size(); i++) {
			try {
				INIT_STEP.invoke(this.sequence.get(i), this, i == 0);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Failed to init assembly step " + i + " of " + this + " — Create API mismatch?", e);
			}
		}
	}
}

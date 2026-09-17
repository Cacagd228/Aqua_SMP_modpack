package me.nanorasmus.nanodev.hex_js.jei;

import me.nanorasmus.nanodev.hex_js.casting.OvidRecipeRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class HexJsJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("meowhex", "jei_plugin");
    private static IJeiRuntime runtime;
    private static final List<OvidJeiRecipe> PENDING = new ArrayList<>();

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new OvidJeiCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<OvidJeiRecipe> recipes = new ArrayList<>();
        OvidRecipeRegistry.all().forEach((key, result) -> {
            ItemStack a = new ItemStack(key.a());
            ItemStack b = new ItemStack(key.b());
            ItemStack out = result.copy();
            recipes.add(new OvidJeiRecipe(a, b, out));
        });
        registration.addRecipes(OvidJeiCategory.TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.BLAST_FURNACE), OvidJeiCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE), OvidJeiCategory.TYPE);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        if (!PENDING.isEmpty()) {
            runtime.getRecipeManager().addRecipes(OvidJeiCategory.TYPE, List.copyOf(PENDING));
            PENDING.clear();
        }
    }

    public static void addRecipeToJeiRuntime(ItemStack a, ItemStack b, ItemStack result) {
        OvidJeiRecipe recipe = new OvidJeiRecipe(a.copyWithCount(1), b.copyWithCount(1), result.copy());
        // Deduplicate: same inputs already pending or already in runtime
        for (OvidJeiRecipe r : PENDING) {
            if (ItemStack.isSameItemSameComponents(r.inputA(), recipe.inputA())
                    && ItemStack.isSameItemSameComponents(r.inputB(), recipe.inputB())
                    && ItemStack.isSameItemSameComponents(r.output(), recipe.output())) {
                return;
            }
        }
        if (runtime != null) {
            // Check already displayed (simple check via manager would require iteration; skip if duplicate inputs/output already exists)
            // For now, also check that we don't add duplicate of existing registry-snapped recipes already shown
            // We rely on OvidRecipeRegistry deduplication, but extra guard: query manager
            try {
                var existing = runtime.getRecipeManager().createRecipeLookup(OvidJeiCategory.TYPE).get().toList();
                for (Object o : existing) {
                    if (o instanceof OvidJeiRecipe r) {
                        if (ItemStack.isSameItemSameComponents(r.inputA(), recipe.inputA())
                                && ItemStack.isSameItemSameComponents(r.inputB(), recipe.inputB())
                                && ItemStack.isSameItemSameComponents(r.output(), recipe.output())) {
                            return;
                        }
                    }
                }
            } catch (Throwable ignored) {}
            runtime.getRecipeManager().addRecipes(OvidJeiCategory.TYPE, List.of(recipe));
        } else {
            PENDING.add(recipe);
        }
    }
}

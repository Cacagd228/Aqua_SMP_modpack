package me.nanorasmus.nanodev.hex_js.addon;

import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.addon.recipe.BoundSpellbookRecipe;
import me.nanorasmus.nanodev.hex_js.addon.recipe.LanisSealThingsRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The two custom CraftingRecipe serializers of the hextended item set, referenced by
 * {@code data/hex_js/recipes/dynamic*} — {@code hex_js:bound_spellbook} (assembles the
 * spellbook from a cover + focuses/scrolls) and {@code hex_js:seal_drawing_orb} (seals
 * a filled drawing orb with a Hex Casting seal material).
 */
public final class HextendedRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, HexJS.MOD_ID);

    static {
        SERIALIZERS.register("bound_spellbook", () -> BoundSpellbookRecipe.SERIALIZER);
        SERIALIZERS.register("seal_drawing_orb", () -> LanisSealThingsRecipe.SERIALIZER);
    }

    private HextendedRecipes() {
    }

    public static void init(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
package me.nanorasmus.nanodev.hex_js.jei;

import me.nanorasmus.nanodev.hex_js.assembly.AssemblyRecipe;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI view of one assembly recipe: the workpiece, the ordered step list, the
 * aether it needs and the finished product.
 *
 * <p>A flat record rather than the recipe itself. {@link AssemblyRecipe} holds an
 * {@code Ingredient}, which JEI cannot draw as a single slot and which has no
 * stable identity for lookups, so the display resolves the ingredient against
 * the recipe manager's catalogue once and keeps the concrete stacks.
 *
 * <p>The step list is shown as a column of scroll glyphs with their localised
 * names rather than as raw ids, because {@code "merge"} on its own tells a
 * player nothing.
 */
public record AssemblyJeiRecipe(List<ItemStack> inputs, List<String> steps, long mana, ItemStack output) {

    /** Localised name for a step id, falling back to the id itself. */
    public static net.minecraft.network.chat.Component stepName(String step) {
        return net.minecraft.network.chat.Component.translatable("meowhex.jei.step." + step);
    }
}

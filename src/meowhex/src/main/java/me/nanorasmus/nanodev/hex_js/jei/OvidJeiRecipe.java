package me.nanorasmus.nanodev.hex_js.jei;

import net.minecraft.world.item.ItemStack;

/**
 * JEI wrapper for Ovid's Distillation recipe.
 * Input counts are ignored for display (shown as 1), actual matching uses item type.
 */
public record OvidJeiRecipe(ItemStack inputA, ItemStack inputB, ItemStack output) {
}

package me.nanorasmus.nanodev.hex_js.addon.recipe;

import me.nanorasmus.nanodev.hex_js.addon.HextendedItems;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemDrawingOrb;
import at.petrak.hexcasting.api.mod.HexTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Dynamic recipe turning a filled, unsealed {@link ItemDrawingOrb} into a sealed one
 * by crafting it with any Hex Casting seal material. Ported from hextended-staves
 * (CC0); sealing prevents the orb's iota from being overwritten.
 */
public class LanisSealThingsRecipe extends CustomRecipe {
    public static final RecipeSerializer<LanisSealThingsRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(LanisSealThingsRecipe::new);

    public LanisSealThingsRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public boolean matches(CraftingInput container, Level level) {
        boolean foundSealMaterial = false;
        boolean foundSealee = false;

        for (int i = 0; i < container.size(); i++) {
            var stack = container.getItem(i);
            if (isCorrectSealee(stack)) {
                if (foundSealee) return false;
                foundSealee = true;
            } else if (stack.is(HexTags.Items.SEAL_MATERIALS)) {
                if (foundSealMaterial) return false;
                foundSealMaterial = true;
            }
        }
        return foundSealMaterial && foundSealee;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registryAccess) {
        ItemStack sealee = ItemStack.EMPTY;

        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getItem(i);
            if (isCorrectSealee(stack)) {
                sealee = stack.copy();
                break;
            }
        }
        if (!sealee.isEmpty()) {
            ItemDrawingOrb.seal(sealee);
            sealee.setCount(1);
        }
        return sealee;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    private boolean isCorrectSealee(ItemStack stack) {
        return stack.is(HextendedItems.DRAWING_ORB.get())
                && ((ItemDrawingOrb) HextendedItems.DRAWING_ORB.get()).readIotaTag(stack) != null
                && !ItemDrawingOrb.isSealed(stack);
    }
}
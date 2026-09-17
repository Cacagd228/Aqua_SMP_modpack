package me.nanorasmus.nanodev.hex_js.addon.recipe;

import me.nanorasmus.nanodev.hex_js.addon.HextendedItems;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemBoundSpellbook;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.common.lib.HexItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Dynamic recipe assembling a {@link me.nanorasmus.nanodev.hex_js.addon.item.ItemBoundSpellbook}
 * from a spellbook cover plus hex casting craftables. Ported from hextended-staves
 * (CC0); the vanilla book part grants the "opens the Hex book on use" tag, while
 * focuses and scrolls determine the page count of the resulting spellbook.
 */
public class BoundSpellbookRecipe extends CustomRecipe {
    public static final RecipeSerializer<BoundSpellbookRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(BoundSpellbookRecipe::new);

    public static final List<Item> BOOK_PARTS = List.of(
            HexItems.FOCUS, HexItems.SCROLL_LARGE, HexItems.SCROLL_MEDIUM, HexItems.SCROLL_SMOL,
            Items.BOOK);

    public BoundSpellbookRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 9;
    }

    @Override
    public boolean matches(CraftingInput container, Level level) {
        boolean foundCover = false;
        int foundParts = 0;
        // mutually exclusive and/or non-stackable parts

        for (int i = 0; i < container.size(); i++) {
            var stack = container.getItem(i);
            if (stack.is(HextendedItems.SPELLBOOK_COVER.get())) {
                if (foundCover) return false;
                foundCover = true;
            } else if (BOOK_PARTS.contains(stack.getItem())) {
                if (foundParts > 8) return false;
                foundParts++;
            } else if (!stack.isEmpty()) {
                return false; // don't eat non-part items
            }
        }
        return foundCover && foundParts > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registryAccess) {
        ItemStack result = HextendedItems.BOUND_SPELLBOOK.get().getDefaultInstance();
        int pageCount = 0;

        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getItem(i);
            if (stack.is(HexItems.FOCUS)) pageCount += 8;
            else if (stack.is(HexItems.SCROLL_LARGE)) pageCount += 4;
            else if (stack.is(HexItems.SCROLL_MEDIUM)) pageCount += 2;
            else if (stack.is(HexItems.SCROLL_SMOL)) pageCount += 1;
            else if (stack.is(Items.BOOK))
                NBTHelper.putString(result, ItemBoundSpellbook.TAG_BOOK_USE_ACTION, "hexbook");
        }
        NBTHelper.putInt(result, ItemBoundSpellbook.TAG_MAX_PAGES, pageCount);
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
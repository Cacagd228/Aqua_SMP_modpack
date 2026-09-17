package me.nanorasmus.nanodev.hex_js.casting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for Ovid's Distillation recipes.
 * Key is ordered pair (inputA, inputB) -> result.
 * Registration via KubeJS: Hexcasting.registerOvidRecipe(...)
 * Behaves like workbench/furnace — shapeless with two item inputs.
 */
public final class OvidRecipeRegistry {
    private static final Map<Key, ItemStack> RECIPES = new HashMap<>();

    static {
        // Default test recipe so JEI shows something even before KubeJS: iron + coal -> diamond
        try {
            RECIPES.put(new Key(Items.IRON_INGOT, Items.COAL), new ItemStack(Items.DIAMOND, 1));
        } catch (Throwable ignored) {
        }
    }

    private OvidRecipeRegistry() {
    }

    public record Key(Item a, Item b) {
    }

    public static void register(ItemStack inputA, ItemStack inputB, ItemStack result) {
        if (inputA == null || inputB == null || result == null || inputA.isEmpty() || inputB.isEmpty() || result.isEmpty()) {
            return;
        }
        Key key = new Key(inputA.getItem(), inputB.getItem());
        ItemStack existing = RECIPES.get(key);
        boolean isDuplicate = existing != null && ItemStack.isSameItemSameComponents(existing, result);
        RECIPES.put(key, result.copy());
        if (!isDuplicate) {
            tryNotifyJei(inputA, inputB, result);
        }
    }

    public static void register(String itemAId, String itemBId, ItemStack result) {
        Item a = itemById(itemAId);
        Item b = itemById(itemBId);
        if (a == null || b == null || result == null || result.isEmpty()) {
            return;
        }
        Key key = new Key(a, b);
        ItemStack existing = RECIPES.get(key);
        boolean isDuplicate = existing != null && ItemStack.isSameItemSameComponents(existing, result);
        RECIPES.put(key, result.copy());
        if (!isDuplicate) {
            tryNotifyJei(new ItemStack(a), new ItemStack(b), result);
        }
    }

    public static void register(Item a, Item b, ItemStack result) {
        if (a == null || b == null || result == null || result.isEmpty()) return;
        Key key = new Key(a, b);
        ItemStack existing = RECIPES.get(key);
        boolean isDuplicate = existing != null && ItemStack.isSameItemSameComponents(existing, result);
        RECIPES.put(key, result.copy());
        if (!isDuplicate) {
            tryNotifyJei(new ItemStack(a), new ItemStack(b), result);
        }
    }

    private static void tryNotifyJei(ItemStack a, ItemStack b, ItemStack result) {
        try {
            Class<?> cls = Class.forName("me.nanorasmus.nanodev.hex_js.jei.HexJsJeiPlugin");
            var m = cls.getMethod("addRecipeToJeiRuntime", ItemStack.class, ItemStack.class, ItemStack.class);
            m.invoke(null, a, b, result);
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    public static ItemStack get(Item a, Item b) {
        ItemStack res = RECIPES.get(new Key(a, b));
        if (res != null) return res.copy();
        // Fallback to swapped order for shapeless behaviour (so уголь+железо и железо+уголь оба работают)
        ItemStack swapped = RECIPES.get(new Key(b, a));
        return swapped == null ? null : swapped.copy();
    }

    @Nullable
    public static ItemStack get(ItemStack stackA, ItemStack stackB) {
        if (stackA.isEmpty() || stackB.isEmpty()) return null;
        return get(stackA.getItem(), stackB.getItem());
    }

    /** Strict ordered lookup without fallback, for JEI dedup and internal use */
    @Nullable
    public static ItemStack getStrict(Item a, Item b) {
        ItemStack res = RECIPES.get(new Key(a, b));
        return res == null ? null : res.copy();
    }

    public static Map<Key, ItemStack> all() {
        return Collections.unmodifiableMap(RECIPES);
    }

    public static void clear() {
        RECIPES.clear();
    }

    @Nullable
    private static Item itemById(String id) {
        if (id == null) return null;
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return null;
        var opt = BuiltInRegistries.ITEM.getOptional(loc);
        return opt.orElse(null);
    }
}

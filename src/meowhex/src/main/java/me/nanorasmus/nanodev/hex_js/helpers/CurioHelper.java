package me.nanorasmus.nanodev.hex_js.helpers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Small server-side helper for the (optional) Curios mod: scans every equipped
 * curio for a specific item. Returns {@code false} when Curios is not installed,
 * so callers must treat "no curios" as "artifact not worn".
 */
public final class CurioHelper {
    private CurioHelper() {
    }

    /** Whether the player wears {@code item} in any Curios slot. */
    public static boolean hasCurio(ServerPlayer player, Item item) {
        return !findCurio(player, item).isEmpty();
    }

    /** Returns the first equipped stack of {@code item}, or {@link ItemStack#EMPTY}. */
    public static ItemStack findCurio(ServerPlayer player, Item item) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) {
            return ItemStack.EMPTY;
        }
        var optional = CuriosApi.getCuriosInventory(player);
        if (optional.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var curios = optional.get().getEquippedCurios();
        for (int i = 0; i < curios.getSlots(); i++) {
            ItemStack stack = curios.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}

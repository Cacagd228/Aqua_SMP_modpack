package me.nanorasmus.nanodev.hex_js.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * Client-side tooltip normaliser for Curios-wearable items.
 *
 * <p>Hex Casting appends its own {@code hexcasting.tooltip.wearable*} lines
 * ("Can be worn in: ..."). Curios paints the canonical gold "Slot: ..." header
 * itself for every item that fits a slot, so those hexcasting lines are pure
 * duplicates — this handler strips them and does nothing else.
 *
 * <p>We deliberately do not paint a "Slot: ..." line of our own: Curios already
 * adds one for every fitting item (its listener evaluates slot fit with or
 * without an entity), and painting our own line in a second listener produced a
 * duplicate header (e.g. on hexcasting's scrying lens).
 */
public final class CurioTooltipHandler {
    private static final String WEARABLE = "hexcasting.tooltip.wearable";
    private static final String WEARABLE_LENS = "hexcasting.tooltip.wearable.lens";

    private CurioTooltipHandler() {
    }

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        List<Component> tooltip = event.getToolTip();

        // Remove hexcasting's duplicate "Can be worn in:" lines wherever they land;
        // Curios paints the "Slot:" header itself.
        tooltip.removeIf(c ->
                c.getContents() instanceof TranslatableContents t
                        && (WEARABLE.equals(t.getKey()) || WEARABLE_LENS.equals(t.getKey())));
    }
}
package me.nanorasmus.nanodev.hex_js.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.nanorasmus.nanodev.hex_js.addon.HextendedItems;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemBoundSpellbook;
import at.petrak.hexcasting.common.items.storage.ItemSpellbook;
import at.petrak.hexcasting.common.lib.HexItems;
import at.petrak.hexcasting.common.msgs.MsgShiftScrollC2S;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Dispatches Hex Casting's shift-scroll page-flipping to the hextended bound
 * spellbook. {@code MsgShiftScrollC2S#handleForHand} only acts on the exact vanilla
 * {@link HexItems#SPELLBOOK} instance, so a {@code hex_js:bound_spellbook} is never
 * reached; this mixin (1) makes the handler treat a held bound spellbook like the
 * vanilla one, and (2) reroutes {@code ItemSpellbook#rotatePageIdx}/{@code highestPage}
 * to the bound-spellbook counterparts that understand the {@code pages} NBT shape.
 *
 * <p>Ported from hextended-staves {@code MsgShiftScrollMixin} (CC0); only the dispatch
 * strategy differs ({@code @Redirect} instead of {@code @Shadow} + HEAD inject) so the
 * now-private {@code spellbook} helper needs no shadowing.
 */
@Mixin(MsgShiftScrollC2S.class)
public abstract class MsgShiftScrollMixin {
    /**
     * Redirects the single {@link ItemStack#getItem()} probe in {@code handleForHand}:
     * a held bound spellbook reports itself as the vanilla spellbook, so the handler
     * routes into its {@code spellbook(...)} branch (the delta!=0 guard still applies).
     */
    @Redirect(method = "handleForHand",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
    private Item hexjs$boundSpellbookDispatches(ItemStack stack) {
        if (stack.getItem() == HextendedItems.BOUND_SPELLBOOK.get()) {
            return HexItems.SPELLBOOK;
        }
        return stack.getItem();
    }

    /**
     * Bound spellbook pages are stored under {@code pages} with a {@code max_pages}
     * cap; the vanilla static helper (plain spellbook NBT) is wrong for them.
     */
    @WrapOperation(method = "spellbook",
            at = @At(value = "INVOKE",
                    target = "Lat/petrak/hexcasting/common/items/storage/ItemSpellbook;rotatePageIdx(Lnet/minecraft/world/item/ItemStack;Z)I"))
    private int hexjs$boundRotatePage(ItemStack stack, boolean increase, Operation<Integer> original) {
        if (stack.getItem() == HextendedItems.BOUND_SPELLBOOK.get()) {
            return ItemBoundSpellbook.rotatePageIdx(stack, increase);
        }
        return original.call(stack, increase);
    }

    @WrapOperation(method = "spellbook",
            at = @At(value = "INVOKE",
                    target = "Lat/petrak/hexcasting/common/items/storage/ItemSpellbook;highestPage(Lnet/minecraft/world/item/ItemStack;)I"))
    private int hexjs$boundHighestPage(ItemStack stack, Operation<Integer> original) {
        if (stack.getItem() == HextendedItems.BOUND_SPELLBOOK.get()) {
            return ItemBoundSpellbook.highestPage(stack);
        }
        return original.call(stack);
    }
}
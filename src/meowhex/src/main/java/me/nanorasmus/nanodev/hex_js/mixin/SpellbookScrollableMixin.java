package me.nanorasmus.nanodev.hex_js.mixin;

import me.nanorasmus.nanodev.hex_js.addon.HextendedItems;
import at.petrak.hexcasting.client.ShiftScrollListener;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Marks the hextended bound spellbook as scrollable on the client so shift-scroll
 * generates the {@code MsgShiftScrollC2S} packets that {@code MsgShiftScrollMixin}
 * handles server-side. Ported from hextended-staves {@code HextendedStavesMixin} (CC0).
 *
 * <p>Client-only: {@code ShiftScrollListener} lives under {@code hexcasting.client},
 * so this mixin is declared in the {@code client} section of {@code hex_js.mixins.json}
 * and never loads on a dedicated server.
 */
@Mixin(ShiftScrollListener.class)
public abstract class SpellbookScrollableMixin {
    @Inject(method = "IsScrollableItem", at = @At("RETURN"), cancellable = true)
    private static void hexjs$boundSpellbookScrollable(Item item, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValue() || item == HextendedItems.BOUND_SPELLBOOK.get());
    }
}
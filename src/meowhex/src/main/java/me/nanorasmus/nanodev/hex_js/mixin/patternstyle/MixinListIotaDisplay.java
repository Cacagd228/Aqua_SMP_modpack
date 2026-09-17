package me.nanorasmus.nanodev.hex_js.mixin.patternstyle;

import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import me.nanorasmus.nanodev.hex_js.helpers.PatternTextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * When a list of patterns is displayed (e.g. a whole recorded spell), each pattern
 * glyph gets a click event that carries the WHOLE set of patterns from that list —
 * so the Lens of Comprehension can copy the entire set to the stack, not just a
 * single glyph. Non-pattern lists render unchanged.
 */
@Mixin(targets = "at.petrak.hexcasting.api.casting.iota.ListIota$1")
public abstract class MixinListIotaDisplay {

    @Inject(method = "display(Lnet/minecraft/nbt/Tag;)Lnet/minecraft/network/chat/Component;",
        at = @At("HEAD"), cancellable = true)
    private void hexjs$listPatternsWholeSetClick(Tag tag, CallbackInfoReturnable<Component> cir) {
        ListTag list = HexUtils.downcast(tag, ListTag.TYPE);

        List<HexPattern> whole = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            HexPattern p = tryReadPattern(list.get(i));
            if (p != null) {
                whole.add(p);
            }
        }
        if (whole.isEmpty()) {
            return;
        }

        MutableComponent out = Component.empty();
        for (int i = 0; i < list.size(); i++) {
            Tag sub = list.get(i);
            HexPattern p = tryReadPattern(sub);
            if (p != null) {
                out.append(PatternTextUtils.patternDisplayComponent(p, whole));
            } else {
                out.append(IotaType.getDisplay(HexUtils.downcast(sub, CompoundTag.TYPE)));
            }
            if (i < list.size() - 1) {
                out.append(Component.literal(", "));
            }
        }

        cir.setReturnValue(Component.translatable("hexcasting.tooltip.list_contents", out)
            .withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static HexPattern tryReadPattern(Tag element) {
        CompoundTag csub = HexUtils.downcast(element, CompoundTag.TYPE);
        if (IotaType.getTypeFromTag(csub) != PatternIota.TYPE) {
            return null;
        }
        Tag data = csub.get(HexIotaTypes.KEY_DATA);
        if (data == null) {
            return null;
        }
        try {
            return PatternIota.deserialize(data).getPattern();
        } catch (Throwable t) {
            return null;
        }
    }
}
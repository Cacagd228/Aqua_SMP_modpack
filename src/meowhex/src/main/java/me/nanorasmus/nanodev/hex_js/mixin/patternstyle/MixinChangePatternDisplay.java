package me.nanorasmus.nanodev.hex_js.mixin.patternstyle;

import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import me.nanorasmus.nanodev.hex_js.helpers.PatternTextUtils;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the plain-text pattern iota display ("HexPattern(...)") with a
 * {@code "<dir,sig>"} marker that the client renders as an actual pattern glyph.
 * Runs on the server too so the marker (plain text + native hover/click events)
 * survives network serialization and renders as a glyph for any client with the mod.
 */
@Mixin(PatternIota.class)
public abstract class MixinChangePatternDisplay {

    @Inject(method = "display(Lat/petrak/hexcasting/api/casting/math/HexPattern;)Lnet/minecraft/network/chat/Component;",
        at = @At("HEAD"), cancellable = true)
    private static void hexjs$patternIotaDisplay(HexPattern pattern, CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(PatternTextUtils.patternDisplayComponent(pattern));
    }
}
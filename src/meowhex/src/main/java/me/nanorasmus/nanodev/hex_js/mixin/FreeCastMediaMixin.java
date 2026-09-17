package me.nanorasmus.nanodev.hex_js.mixin;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv;
import me.nanorasmus.nanodev.hex_js.HexFreeCast;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Waives media during a script-invoked {@code forceCast}. While the player is in
 * {@link HexFreeCast}, {@link CastingEnvironment#extractMedia} reports nothing
 * missing, so every {@code ConsumeMedia} side effect during the forced cast is free —
 * same guarantee the original addon made for staff-less, media-less forced casts.
 */
@Mixin(StaffCastEnv.class)
public abstract class FreeCastMediaMixin {
    @Inject(method = "extractMediaEnvironment", at = @At("HEAD"), cancellable = true)
    private void hexjs$freeMediaForForcedCast(long amount, CallbackInfoReturnable<Long> cir) {
        ServerPlayer caster = ((CastingEnvironment) (Object) this).getCaster();
        if (caster != null && HexFreeCast.isActive(caster.getUUID())) {
            cir.setReturnValue(0L);
        }
    }
}
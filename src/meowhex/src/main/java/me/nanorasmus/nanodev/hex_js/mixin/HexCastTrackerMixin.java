package me.nanorasmus.nanodev.hex_js.mixin;

import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.iota.Iota;
import me.nanorasmus.nanodev.hex_js.casting.PingPongHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Отслеживает активный гекс-каст (со стеком, на случай вложенных VM),
 * чтобы PingPongHandler отражал только эффекты, наложенные гексами.
 */
@Mixin(CastingVM.class)
public abstract class HexCastTrackerMixin {

    @Inject(method = "queueExecuteAndWrapIotas", at = @At("HEAD"))
    private void hexjs$pushHexCaster(List<Iota> iotas, ServerLevel world,
            CallbackInfoReturnable<at.petrak.hexcasting.api.casting.eval.ExecutionClientView> cir) {
        ServerPlayer caster = null;
        try {
            caster = ((CastingVM) (Object) this).getEnv().getCaster();
        } catch (Throwable ignored) {
        }
        PingPongHandler.pushHexCaster(caster == null ? null : caster.getUUID());
    }

    @Inject(method = "queueExecuteAndWrapIotas", at = @At("TAIL"))
    private void hexjs$popHexCaster(List<Iota> iotas, ServerLevel world,
            CallbackInfoReturnable<at.petrak.hexcasting.api.casting.eval.ExecutionClientView> cir) {
        PingPongHandler.popHexCaster();
    }
}

package me.nanorasmus.nanodev.hex_js.mixin;

import me.nanorasmus.nanodev.hex_js.casting.PingPongHandler;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Гамбит Пинг-Понга: перехватывает наложение эффектов на помеченную цель
 * и перенаправляет их на кастера, записавшего метку.
 * Слушаем обе перегрузки: одноаргументная обычно делегирует двуаргументной,
 * но порядок делегирования не гарантирован — повторный вызов безопасен
 * благодаря стражу в PingPongHandler.
 */
@Mixin(LivingEntity.class)
public abstract class PingPongBounceMixin {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void hexjs$pingPongBounceTwoArg(MobEffectInstance inst, Entity source,
            CallbackInfoReturnable<Boolean> cir) {
        Boolean bounced = PingPongHandler.tryBounce((LivingEntity) (Object) this, inst);
        if (bounced != null) {
            cir.setReturnValue(bounced);
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            at = @At("HEAD"), cancellable = true)
    private void hexjs$pingPongBounceOneArg(MobEffectInstance inst,
            CallbackInfoReturnable<Boolean> cir) {
        Boolean bounced = PingPongHandler.tryBounce((LivingEntity) (Object) this, inst);
        if (bounced != null) {
            cir.setReturnValue(bounced);
        }
    }
}

package me.nanorasmus.nanodev.hex_js.mixin;

import me.nanorasmus.nanodev.hex_js.casting.LightningRodHandler;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Громоотвод: перехватывает наложение «Безмолвия», если рядом есть носитель.
 *
 * <p>Слушаем обе перегрузки {@code addEffect} — однааргументная обычно делегирует
 * двуаргументной, но порядок делегирования не гарантирован. Повторный вызов
 * безопасен: {@link LightningRodHandler} под стражем не даёт перехватить эффект,
 * который сам же только что перенёс на носителя.
 */
@Mixin(LivingEntity.class)
public abstract class LightningRodSilenceMixin {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void hexjs$lightningRodTwoArg(MobEffectInstance inst, Entity source,
            CallbackInfoReturnable<Boolean> cir) {
        if (LightningRodHandler.tryInterceptSilence((LivingEntity) (Object) this, inst)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            at = @At("HEAD"), cancellable = true)
    private void hexjs$lightningRodOneArg(MobEffectInstance inst,
            CallbackInfoReturnable<Boolean> cir) {
        if (LightningRodHandler.tryInterceptSilence((LivingEntity) (Object) this, inst)) {
            cir.setReturnValue(true);
        }
    }
}

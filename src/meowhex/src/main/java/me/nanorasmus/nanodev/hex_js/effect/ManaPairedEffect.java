package me.nanorasmus.nanodev.hex_js.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Общий пул активен — маркер спаренного состояния.
 * Висит бесконечно пока пулы общие. Синхронизируется на клиент
 * автоматически (нужен для синего HUD в ManaBarHud).
 * Снятие молоком = разрыв пейринга (чистится в хендлере на тике).
 */
public class ManaPairedEffect extends MobEffect {
    public ManaPairedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3F8FFF); // синий — общий пул
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        return false;
    }
}

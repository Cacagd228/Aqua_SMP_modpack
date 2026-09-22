package me.nanorasmus.nanodev.hex_js.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Мана-пейринг — ожидание подтверждения (5 сек).
 * Висит на обоих кастерах после первого каста.
 * Тиковой логики нет, только маркер + синхронизация на клиент.
 * Снимается молоком (истечение pending чистится в хендлере).
 */
public class ManaPairingEffect extends MobEffect {
    public ManaPairingEffect() {
        super(MobEffectCategory.NEUTRAL, 0xB49BE8); // светло-фиолетовый, ожидание
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

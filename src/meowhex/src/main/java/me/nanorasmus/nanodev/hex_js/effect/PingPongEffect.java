package me.nanorasmus.nanodev.hex_js.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Пинг-Понг — маркер Гамбита Пинг-Понга на цели.
 * Пока висит: любой эффект, наложенный на цель гексами кастера,
 * записавшего метку, перенаправляется на самого кастера
 * (см. PingPongHandler). Снимается молоком.
 */
public class PingPongEffect extends MobEffect {
    public PingPongEffect() {
        super(MobEffectCategory.HARMFUL, 0xE8913A); // оранжевый мяча
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false; // тиковой логики нет, только маркер
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        return false;
    }
}

package me.nanorasmus.nanodev.hex_js.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Привязь Эреба — маркер активной Цепи Эреба на сущности.
 * Пока висит: 30% входящего урона остаётся на сущности,
 * 70% в двойном объёме уходит на привязанного зомби
 * (см. ErebusChainHandler). Снимается молоком — привязка рвётся.
 */
public class ErebusBindEffect extends MobEffect {
    public ErebusBindEffect() {
        super(MobEffectCategory.HARMFUL, 0x2b0a4a); // тёмный фиолет Эреба
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

package me.nanorasmus.nanodev.hex_js.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Морф-хекс — цель превращена в курицу.
 * Вся механика в {@code MorphHexHandler}: входящий урон x0.2, полный локдаун
 * (движение, атаки, блоки, слоты, предметы), масштаб 0.55, рендер курицы на клиенте.
 * Тиковой логики в эффекте нет, только маркер. Снятие молоком/смертью чистится в хендлере.
 */
public class MorphHexEffect extends MobEffect {
    public MorphHexEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFD75F); // куриный жёлтый
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

package me.nanorasmus.nanodev.hex_js.effect;

import at.petrak.hexcasting.api.misc.ManaHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Mana Regeneration — плавно повышает регенерацию маны, пока действует: каждый
 * тик восстанавливает {@code (amplifier + 1)}% максимальной маны в секунду
 * (доля делится на 20 тиков), т.е. уровень I = 1%/с, II = 2%/с и т.д.
 * Работает только с игроками (мана есть только у них); идёт поверх обычного
 * регена и не зависит от паузы регена после расхода. Частицы — фиолетовые
 * (цвет эффекта {@link #getColor()}).
 */
public class ManaRegenEffect extends MobEffect {
    private static final double FRACTION_PER_LEVEL_PER_SECOND = 0.01;
    private static final double TICKS_PER_SECOND = 20.0;

    public ManaRegenEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8561C1); // mana purple
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            double perTick = ManaHelper.maxMana(player)
                    * (amplifier + 1) * FRACTION_PER_LEVEL_PER_SECOND / TICKS_PER_SECOND;
            ManaHelper.setMana(player, ManaHelper.getMana(player) + perTick);
        }
        return true;
    }
}
package com.colonizer.colonycard.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Дебаг-эффект «лицо закрыто».
 *
 * <p>Ведёт себя как балаклава по логике отображения: {@code FaceCover} считает
 * его закрытым лицом, поэтому ник, чат и nameplate ведут себя так же, как с
 * надетой балаклавой. Отличие только в дебаге: скрытое имя показывается
 * красными точками, а не истинным ником.
 */
public class CoveredFaceEffect extends MobEffect {

    public CoveredFaceEffect() {
        super(MobEffectCategory.HARMFUL, 0x2B2B30);
    }
}

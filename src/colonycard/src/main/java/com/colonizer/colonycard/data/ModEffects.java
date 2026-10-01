package com.colonizer.colonycard.data;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.effect.CoveredFaceEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Эффекты мода: {@code colonycard:covered_face} — дебаг-версия балаклавы. */
public final class ModEffects {

    private ModEffects() {
    }

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, ColonyCardMod.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> COVERED_FACE =
            MOB_EFFECTS.register("covered_face", CoveredFaceEffect::new);
}

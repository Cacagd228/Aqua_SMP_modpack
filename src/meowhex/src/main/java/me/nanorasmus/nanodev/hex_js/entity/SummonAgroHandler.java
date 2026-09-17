package me.nanorasmus.nanodev.hex_js.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Ванильные мобы не считают призыв врагом: их цели — игроки/жители/големы,
 * поэтому призванного зомби игнорируют (в ответ бьют только если их ударить).
 * Впрыскиваем каждому враждебному мобу при появлении в мире цели на
 * {@link EntityHadesSummon} и {@link EntityStyxShade}, чтобы мобы агрились
 * на призыв как на обычного врага.
 */
public final class SummonAgroHandler {
    private static final String INJECTED_TAG = "meowhex:summon_agro";

    private SummonAgroHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob instanceof Enemy)) return;
        if (mob instanceof EntityHadesSummon) return;
        try {
            // Тег персистентный (сохраняется в NBT) — не дублируем гол при перезаходе в чанк.
            if (mob.getPersistentData().getBoolean(INJECTED_TAG)) return;
            mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob,
                    EntityHadesSummon.class, 10, true, false, e -> true));
            mob.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(mob,
                    EntityStyxShade.class, 10, true, false, e -> true));
            mob.getPersistentData().putBoolean(INJECTED_TAG, true);
        } catch (Throwable ignored) {
        }
    }
}

package me.nanorasmus.nanodev.hex_js.casting;

import me.nanorasmus.nanodev.hex_js.effect.ErebusBindEffect;
import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import me.nanorasmus.nanodev.hex_js.entity.EntityHadesSummon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Цепь Эреба: привязка сущности к призванному зомби.
 * <ul>
 *   <li>Входящий урон по привязанной сущности делится: 30% остаётся на ней,
 *       70% в двойном объёме уходит на привязанного зомби.</li>
 *   <li>Привязка живёт, пока жив зомби и хватает маны на upkeep
 *       ({@link #UPKEEP_MANA_PER_SECOND} маны/сек с кастера).</li>
 *   <li>Снятие эффекта молоком тоже рвёт привязку (чистится на тике).</li>
 * </ul>
 */
public final class ErebusChainHandler {
    /** Доля урона, остающаяся на привязанной сущности. */
    public static final float SELF_SHARE = 0.3f;
    /** Множитель transferred-доли (70% -> зомби в двойном объёме). */
    public static final float ZOMBIE_MULT = 2.0f;
    /** Upkeep маны в секунду с кастера за каждую привязку. */
    public static final double UPKEEP_MANA_PER_SECOND = 10.0;

    private record Binding(UUID summonId, UUID casterId) {
    }

    private static final ConcurrentHashMap<UUID, Binding> BINDS = new ConcurrentHashMap<>();
    /** Защита от рекурсии: урон, перенаправленный на зомби, не делится снова. */
    private static final ThreadLocal<Boolean> REDIRECTING = ThreadLocal.withInitial(() -> false);

    private static int tickCounter = 0;

    private ErebusChainHandler() {
    }

    /** Создать (или заменить) привязку сущности к зомби. */
    public static void bind(ServerPlayer caster, LivingEntity target, EntityHadesSummon summon) {
        unbind(target);
        target.addEffect(new MobEffectInstance(HexEffects.EREBUS_BIND,
                MobEffectInstance.INFINITE_DURATION, 0, false, true, true));
        BINDS.put(target.getUUID(), new Binding(summon.getUUID(), caster.getUUID()));
    }

    /** Разорвать привязку (карта + эффект). */
    public static void unbind(LivingEntity target) {
        BINDS.remove(target.getUUID());
        try {
            target.removeEffect(HexEffects.EREBUS_BIND);
        } catch (Throwable ignored) {
        }
    }

    public static boolean isBound(LivingEntity target) {
        return BINDS.containsKey(target.getUUID());
    }

    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (REDIRECTING.get()) return;
        LivingEntity victim = event.getEntity();
        Binding b = BINDS.get(victim.getUUID());
        if (b == null) return;
        if (!victim.hasEffect(HexEffects.EREBUS_BIND)) {
            BINDS.remove(victim.getUUID());
            return;
        }
        if (!(victim.level() instanceof ServerLevel)) return;
        EntityHadesSummon summon = findSummon(victim.level().getServer(), b.summonId);
        if (summon == null || !summon.isAlive()) {
            unbind(victim);
            return;
        }
        float orig = event.getNewDamage();
        if (orig <= 0) return;
        REDIRECTING.set(true);
        try {
            event.setNewDamage(orig * SELF_SHARE);
            summon.hurt(event.getSource(), orig * (1.0f - SELF_SHARE) * ZOMBIE_MULT);
        } finally {
            REDIRECTING.set(false);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return; // один прогон в тик
        if ((++tickCounter % 20) != 0) return; // раз в секунду
        var server = level.getServer();
        for (var e : BINDS.entrySet()) {
            UUID boundId = e.getKey();
            Binding b = e.getValue();
            Entity raw = findEntity(server, boundId);
            if (!(raw instanceof LivingEntity bound) || !bound.isAlive()
                    || !bound.hasEffect(HexEffects.EREBUS_BIND)) {
                BINDS.remove(boundId);
                if (raw instanceof LivingEntity le) {
                    try {
                        le.removeEffect(HexEffects.EREBUS_BIND);
                    } catch (Throwable ignored) {
                    }
                }
                continue;
            }
            EntityHadesSummon summon = findSummon(server, b.summonId);
            if (summon == null || !summon.isAlive()) {
                unbind(bound);
                continue;
            }
            ServerPlayer caster = server.getPlayerList().getPlayer(b.casterId);
            if (caster == null) continue; // кастер оффлайн — сущности скоро выгрузятся
            boolean free = caster.isCreative() || caster.isSpectator();
            try {
                free = free || at.petrak.hexcasting.api.misc.ManaHelper.hasInfiniteMana(caster);
            } catch (Throwable ignored) {
            }
            if (!free) {
                double mana = 0;
                try {
                    mana = at.petrak.hexcasting.api.misc.ManaHelper.getMana(caster);
                } catch (Throwable ignored) {
                }
                if (mana < UPKEEP_MANA_PER_SECOND) {
                    unbind(bound);
                } else {
                    try {
                        at.petrak.hexcasting.api.misc.ManaHelper.setMana(caster, mana - UPKEEP_MANA_PER_SECOND);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }

    private static EntityHadesSummon findSummon(net.minecraft.server.MinecraftServer server, UUID id) {
        if (server == null) return null;
        Entity e = findEntity(server, id);
        return e instanceof EntityHadesSummon s ? s : null;
    }

    private static Entity findEntity(net.minecraft.server.MinecraftServer server, UUID id) {
        if (server == null) return null;
        for (ServerLevel sl : server.getAllLevels()) {
            Entity e = sl.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }
}

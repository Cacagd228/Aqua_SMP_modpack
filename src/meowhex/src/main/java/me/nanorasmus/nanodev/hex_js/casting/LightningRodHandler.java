package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.common.lib.HexAttributes;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.addon.HexArtifactsItems;
import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import me.nanorasmus.nanodev.hex_js.helpers.CurioHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Громоотвод: перехват «Безмолвия» из радиуса {@link #RADIUS} на носящего.
 *
 * <p>Срабатывает по событию наложения эффекта (см. {@code LightningRodSilenceMixin}):
 * как только на кого-то легло «Безмолвие», каждый носитель громоотвода в радиусе
 * забирает его себе. Само событие — только повод; забираются <b>все</b> «Безмолвия»
 * в радиусе носителя, а не только только что наложенное.
 *
 * <p>Правила:
 * <ul>
 *   <li>«Безмолвие» с цели снимается целиком и переезжает на носителя
 *       (сроки суммируются, амплификер один — эффект не копится).</li>
 *   <li>За каждое перехваченное «Безмолвие» — {@link #REGEN_PER_STACK} регена
 *       маны на {@link #STACK_TICKS} секунд. Стаки суммируются, окно общее:
 *       каждое перехваченное «Безмолвие» продлевает его до now + 10с.</li>
 *   <li>Собственное «Безмолвие» носителя не трогается и бонуса не даёт.</li>
 *   <li>«Безмолвие» других носителей громоотвода не перехватывается —
 *       иначе два громоотвода бесконечно гоняли бы эффект друг на друге.</li>
 * </ul>
 *
 * <p>Бонус регена — временный модификатор {@code MANA_REGEN} в единицах hexcasting
 * «мана за 6 секунд»: ставится при перехвате, снимается по истечении окна.
 * Счётчик стаков живёт в памяти, {@link #BUFFS}.
 */
public final class LightningRodHandler {
    /** Радиус перехвата вокруг носителя, блоков. */
    public static final double RADIUS = 15.0;
    /** Срок одного стака регена, тиков (10 секунд). */
    public static final int STACK_TICKS = 200;
    /** +10% регена маны за каждое перехваченное «Безмолвие». */
    public static final double REGEN_PER_STACK = 0.10;

    /** Модификатор {@code MANA_REGEN}, даваемый за стаки. */
    public static final ResourceLocation REGEN_ID = HexJS.modLoc("lightning_rod_mana_regen");

    /** Стаки регена и момент (game time), когда они сгорают. */
    private record Stacks(int count, long expiresAt) {
    }

    private static final ConcurrentHashMap<UUID, Stacks> BUFFS = new ConcurrentHashMap<>();

    /** Страж от рекурсии: перенос сам вешает «Безмолвие», а это снова событие. */
    private static final ThreadLocal<Boolean> TRANSFERRING = ThreadLocal.withInitial(() -> false);

    private LightningRodHandler() {
    }

    /**
     * Вызывается из миксина, когда на сущность пытаются наложить эффект.
     * Если рядом есть носитель громоотвода — перехватывает «Безмолвие»
     * и возвращает {@code true}, отменяя обычное наложение на цель.
     */
    public static boolean tryInterceptSilence(LivingEntity target, MobEffectInstance inst) {
        if (target == null || inst == null || TRANSFERRING.get()) {
            return false;
        }
        if (inst.getEffect().value() != HexEffects.SILENCE.get()) {
            return false;
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return false;
        }
        if (wearsLightningRod(target)) {
            return false; // носитель не перехватывает сам себя
        }
        if (!hasWearerInRange(level, target)) {
            return false;
        }
        TRANSFERRING.set(true);
        try {
            // inst передаём дальше: mixin стоит на HEAD, эффекта на цели ещё нет,
            // но именно он — главный перехватываемый. Без этого попадали бы в
            // подсчёт только «лежавшие» Безмолвия, а не только что наложенное.
            pullAll(level, target, inst);
        } finally {
            TRANSFERRING.set(false);
        }
        return true; // на цель эффект не накладываем — он ушёл к носителям
    }

    /** Надето ли громоотвод (любой слот Curios). */
    public static boolean wearsLightningRod(LivingEntity entity) {
        return entity instanceof ServerPlayer player
                && CurioHelper.hasCurio(player, HexArtifactsItems.LIGHTNING_ROD.get());
    }

    /** Есть ли живой носитель громоотвода в радиусе от {@code around}. */
    private static boolean hasWearerInRange(ServerLevel level, LivingEntity around) {
        double rSq = RADIUS * RADIUS;
        for (ServerPlayer p : level.players()) {
            if (p != around && p.isAlive()
                    && p.distanceToSqr(around) <= rSq
                    && wearsLightningRod(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Прогоняет перехват для каждого носителя в радиусе от события.
     * Снятие эффекта с цели сразу исключает её из выдачи следующему носителю,
     * поэтому «Безмолвие» не достаётся двоим сразу.
     */
    private static void pullAll(ServerLevel level, LivingEntity eventTarget, MobEffectInstance incoming) {
        double rSq = RADIUS * RADIUS;
        // Копия: pull() эффекты снимает, но список игроков не трогает — копия для ясности.
        for (ServerPlayer wearer : new ArrayList<>(level.players())) {
            if (!wearer.isAlive() || !wearsLightningRod(wearer)) {
                continue;
            }
            // Носитель ловит только то, до чего дотянулся: событие было рядом с ним.
            if (wearer == eventTarget || wearer.distanceToSqr(eventTarget) > rSq) {
                continue;
            }
            pull(wearer, rSq, eventTarget, incoming);
        }
    }

    /**
     * Забирает всё «Безмолвие» в радиусе носителя и вешает ему реген за стаки.
     *
     * @param eventTarget сущность, на которую пытались наложить эффект
     * @param incoming    сам эффект: на {@code eventTarget} он ещё не применён,
     *                    поэтому учитывается отдельно от уже лежащих
     */
    private static void pull(ServerPlayer wearer, double rSq, LivingEntity eventTarget,
            MobEffectInstance incoming) {
        int stolen = 0;
        int totalDuration = 0;
        // Бесконечное «Безмолвие» (INFINITE_DURATION) не складываем: сумма -1 с
        // положительным сроком дала бы конечное число — отмечаем флагом отдельно.
        boolean infinite = false;
        // Собираем целей заранее: снятие эффекта мутирует их состояние.
        ArrayList<LivingEntity> victims = new ArrayList<>();
        for (LivingEntity e : wearer.level().getEntitiesOfClass(LivingEntity.class,
                wearer.getBoundingBox().inflate(RADIUS), LivingEntity::isAlive)) {
            if (e == wearer) {
                continue; // своё «Безмолвие» носителя не трогаем и не считаем
            }
            if (e.distanceToSqr(wearer) > rSq) {
                continue;
            }
            if (wearsLightningRod(e)) {
                continue; // чужое «Безмолвие» носителя не трогаем
            }
            if (e == eventTarget) {
                // Эффект на цели ещё не наложен (перехват на HEAD) — берём
                // срок из аргумента, чтобы он тоже попал в перехват.
                victims.add(e);
                stolen++;
                infinite |= incoming.getDuration() == MobEffectInstance.INFINITE_DURATION;
                if (!infinite) {
                    totalDuration += incoming.getDuration();
                }
                continue;
            }
            MobEffectInstance cur = e.getEffect(HexEffects.SILENCE);
            if (cur == null) {
                continue;
            }
            victims.add(e);
            stolen++;
            if (cur.getDuration() == MobEffectInstance.INFINITE_DURATION) {
                infinite = true;
            } else if (!infinite) {
                totalDuration += cur.getDuration();
            }
        }
        if (stolen == 0) {
            return;
        }
        for (LivingEntity e : victims) {
            try {
                e.removeEffect(HexEffects.SILENCE);
            } catch (Throwable ignored) {
            }
        }
        // Срок на носителе — сумма украденных; если хоть одно было бесконечным,
        // перенесённое тоже остаётся бесконечным.
        int duration = infinite ? MobEffectInstance.INFINITE_DURATION : totalDuration;
        try {
            // TRANSFERRING уже выставлен: addEffect не должен вызвать перехват заново.
            wearer.addEffect(new MobEffectInstance(HexEffects.SILENCE, duration, 0,
                    false, true, true));
        } catch (Throwable ignored) {
        }
        addStacks(wearer, stolen, wearer.level().getGameTime());
    }

    /** Добавляет стаки регена и продлевает окно до now + STACK_TICKS. */
    private static void addStacks(ServerPlayer player, int added, long now) {
        BUFFS.merge(player.getUUID(), new Stacks(added, now + STACK_TICKS),
                (old, next) -> new Stacks(old.count() + next.count(),
                        Math.max(old.expiresAt(), next.expiresAt())));
        applyRegen(player);
    }

    /** Ставит/обновляет модификатор регена по текущему числу стаков. */
    private static void applyRegen(ServerPlayer player) {
        AttributeInstance attr = regenAttr(player);
        if (attr == null) {
            return;
        }
        Stacks s = BUFFS.get(player.getUUID());
        double bonus = s == null ? 0.0 : s.count() * REGEN_PER_STACK;
        var existing = attr.getModifier(REGEN_ID);
        if (existing != null
                && existing.operation() == AttributeModifier.Operation.ADD_VALUE
                && Math.abs(existing.amount() - bonus) < 1e-9) {
            return; // уже актуален
        }
        attr.removeModifier(REGEN_ID);
        if (bonus > 1e-9) {
            attr.addTransientModifier(new AttributeModifier(
                    REGEN_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static AttributeInstance regenAttr(ServerPlayer player) {
        return player.getAttribute(
                BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_REGEN));
    }

    /** Снимает бонус и чистит запись (смерть). */
    private static void clear(ServerPlayer player) {
        BUFFS.remove(player.getUUID());
        AttributeInstance attr = regenAttr(player);
        if (attr != null) {
            attr.removeModifier(REGEN_ID);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return; // один прогон в тик
        }
        if (BUFFS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (var e : new ArrayList<>(BUFFS.entrySet())) {
            if (e.getValue().expiresAt() > now) {
                continue;
            }
            BUFFS.remove(e.getKey(), e.getValue());
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                continue; // вышел — бонус сгорел вместе с игроком
            }
            AttributeInstance attr = regenAttr(p);
            if (attr != null) {
                attr.removeModifier(REGEN_ID);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BUFFS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }
}

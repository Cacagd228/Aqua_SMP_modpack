package me.nanorasmus.nanodev.hex_js.casting;

import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Гамбит Пинг-Понга: перенаправление эффектов с цели на кастера.
 * <p>
 * Привязка цель → кастер (последний каст побеждает). Отражение срабатывает
 * только для эффектов, наложенных внутри гекс-каста того же кастера:
 * активный каст отслеживается через миксин на CastingVM
 * (см. HexCastTrackerMixin), зелья/маяк/стрелы вне каста не отражаются.
 * Сам маркер Пинг-Понга не отражается. Цепочек нет: перенаправленное
 * наложение выполняется под стражем и идёт напрямую.
 */
public final class PingPongHandler {
    private static final ConcurrentHashMap<UUID, UUID> BINDS = new ConcurrentHashMap<>();

    private static final ThreadLocal<ArrayDeque<UUID>> HEX_CASTERS =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Boolean> BOUNCING =
            ThreadLocal.withInitial(() -> false);
    private static final UUID NO_CASTER = new UUID(0, 0);

    private static int tickCounter = 0;

    private PingPongHandler() {
    }

    public static void bind(UUID target, UUID caster) {
        if (target != null && caster != null) {
            BINDS.put(target, caster);
        }
    }

    public static void pushHexCaster(UUID caster) {
        HEX_CASTERS.get().push(caster == null ? NO_CASTER : caster);
    }

    public static void popHexCaster() {
        var stack = HEX_CASTERS.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    private static UUID currentHexCaster() {
        UUID top = HEX_CASTERS.get().peek();
        return (top == null || top.equals(NO_CASTER)) ? null : top;
    }

    /**
     * @return результат для addEffect, если эффект перехвачен,
     * или null, чтобы наложение шло обычным путём.
     */
    public static Boolean tryBounce(LivingEntity target, MobEffectInstance inst) {
        if (target == null || inst == null || BOUNCING.get()) {
            return null;
        }
        // Свой маркер не отражаем (иначе гамбит отскочил бы сам в кастера).
        if (inst.getEffect().value() == HexEffects.PING_PONG.get()) {
            return null;
        }
        UUID hexCaster = currentHexCaster();
        if (hexCaster == null) {
            return null; // вне гекс-каста: зелья, маяк, стрелы — не трогаем
        }
        UUID bound = BINDS.get(target.getUUID());
        if (bound == null || !bound.equals(hexCaster)) {
            return null;
        }
        var server = target.level().getServer();
        if (server == null) {
            return null;
        }
        ServerPlayer caster = server.getPlayerList().getPlayer(bound);
        if (caster == null || !caster.isAlive()) {
            return null; // кастера нет — остаётся на цели
        }
        BOUNCING.set(true);
        try {
            MobEffectInstance copy = new MobEffectInstance(inst.getEffect(),
                    inst.getDuration(), inst.getAmplifier(),
                    inst.isAmbient(), inst.isVisible(), inst.showIcon());
            return caster.addEffect(copy);
        } finally {
            BOUNCING.set(false);
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
        if (BINDS.isEmpty() || (++tickCounter % 20) != 0) {
            return; // раз в секунду
        }
        var server = level.getServer();
        for (var e : new ArrayList<>(BINDS.entrySet())) {
            LivingEntity target = findLiving(server, e.getKey());
            if (target == null || !target.isAlive() || !target.hasEffect(HexEffects.PING_PONG)) {
                BINDS.remove(e.getKey(), e.getValue());
            }
        }
    }

    private static LivingEntity findLiving(net.minecraft.server.MinecraftServer server, UUID id) {
        for (ServerLevel sl : server.getAllLevels()) {
            var e = sl.getEntity(id);
            if (e instanceof LivingEntity le) {
                return le;
            }
        }
        return null;
    }
}

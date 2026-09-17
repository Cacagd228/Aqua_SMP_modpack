package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Отложенные продолжения Хроноса: образ + продолжение + окружение ждут
 * своего тика, затем исполняются как верхнеуровневый прогон (mishap'ы —
 * обычным путём, со всеми FX). Проверки — на момент срабатывания.
 * Кастера нет в сети/мёртв, либо объект игрока сменился (релог) —
 * ожидание тихо сгорает. Круги без кастера-игрока исполняются как есть.
 */
public final class ChronosHandler {
    /** Мягкий кламп задержки: 100 тиков (5 секунд). */
    public static final int MAX_TICKS = 100;
    /** Цена отсрочки: 1 пыль (10000 меди) за секунду, округление вверх. */
    public static final long FEE_PER_SECOND = 10_000L;

    private record Pending(CastingImage image, SpellContinuation cont, CastingEnvironment env,
                           UUID casterId, ServerPlayer casterRef, long dueTick) {
    }

    private static final ConcurrentLinkedQueue<Pending> QUEUE = new ConcurrentLinkedQueue<>();

    private ChronosHandler() {
    }

    public static void schedule(CastingImage image, SpellContinuation cont, CastingEnvironment env,
            ServerPlayer caster, int delayTicks) {
        long now;
        try {
            now = env.getWorld().getServer().getTickCount();
        } catch (Throwable ignored) {
            return;
        }
        QUEUE.add(new Pending(image, cont, env,
                caster != null ? caster.getUUID() : null, caster, now + Math.max(1, delayTicks)));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return; // один прогон в тик
        }
        if (QUEUE.isEmpty()) {
            return;
        }
        long now = level.getServer().getTickCount();
        var due = new ArrayList<Pending>();
        for (var p : QUEUE) {
            if (p.dueTick() <= now && QUEUE.remove(p)) {
                due.add(p);
            }
        }
        for (var p : due) {
            fire(p);
        }
    }

    private static void fire(Pending p) {
        ServerLevel world;
        try {
            world = p.env().getWorld();
        } catch (Throwable ignored) {
            return;
        }
        if (p.casterId() != null) {
            ServerPlayer cur;
            try {
                cur = world.getServer().getPlayerList().getPlayer(p.casterId());
            } catch (Throwable ignored) {
                return;
            }
            // Нет в сети / мёртв / объект сменился (релог) — сгорает.
            if (cur == null || !cur.isAlive() || cur != p.casterRef()) {
                return;
            }
        }
        CastingVM sub;
        try {
            sub = new CastingVM(p.image(), p.env());
        } catch (Throwable ignored) {
            return;
        }
        // Стартовый спрей в точке кастера (если есть).
        try {
            Vec3 at;
            if (p.casterId() != null) {
                var pl = world.getServer().getPlayerList().getPlayer(p.casterId());
                at = pl != null ? pl.position().add(0, 1, 0) : Vec3.ZERO;
            } else {
                at = Vec3.ZERO;
            }
            new ParticleSpray(at, new Vec3(0, 1, 0), 0.5, Math.PI / 3, 20)
                    .sprayParticles(world, p.env().getPigment());
        } catch (Throwable ignored) {
        }
        SpellContinuation cont = p.cont();
        while (cont instanceof SpellContinuation.NotDone nd) {
            var res = nd.getFrame().evaluate(nd.getNext(), world, sub);
            if (res.getNewData() != null) {
                sub.setImage(res.getNewData());
            }
            try {
                sub.getEnv().postExecution(res);
            } catch (Throwable ignored) {
            }
            cont = res.getContinuation();
            for (var fx : res.getSideEffects()) {
                try {
                    if (fx.performEffect(sub)) {
                        return; // нечем платить / mishap завершил
                    }
                } catch (Throwable ignored) {
                    return;
                }
            }
            if (!res.getResolutionType().getSuccess()) {
                return;
            }
        }
    }
}

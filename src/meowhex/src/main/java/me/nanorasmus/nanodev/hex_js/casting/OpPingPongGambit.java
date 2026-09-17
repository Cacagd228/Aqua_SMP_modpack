package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota;
import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getDouble;
import static at.petrak.hexcasting.api.casting.OperatorUtils.getEntity;

/**
 * Ping-Pong Gambit — вешает на цель метку Пинг-Понга.
 * <p>
 * Стек: [entity, double seconds] — живая цель (можно себя) в радиусе каста
 * и длительность в секундах. Секунды мягко обрезаются в [1, 5]:
 * ниже 1 — mishap, выше 5 — кламп. Стоимость — 500 маны за секунду.
 * Пока висит метка, любой эффект, наложенный на цель гексами кастера,
 * записавшего метку, перенаправляется на самого кастера
 * (см. {@link PingPongHandler}). Зелья, маяк и стрелы вне каста не отражаются.
 * Молоко снимает метку досрочно. Повторный каст перезаписывает привязку.
 */
public class OpPingPongGambit implements SpellAction {

    public static final OpPingPongGambit INSTANCE = new OpPingPongGambit();

    private static final double MIN_SECONDS = 1.0;
    private static final double MAX_SECONDS = 5.0;
    private static final int COST_PER_SECOND = 500_000; // 500 mana

    private OpPingPongGambit() {
    }

    @Override
    public int getArgc() {
        return 2;
    }

    @Override
    public boolean hasCastingSound(CastingEnvironment env) {
        return true;
    }

    @Override
    public boolean awardsCastingStat(CastingEnvironment env) {
        return true;
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation cont) {
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        Entity target;
        double rawSeconds;
        try {
            target = getEntity(args, 0, getArgc());
            rawSeconds = getDouble(args, 1, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        if (!(target instanceof LivingEntity living)) {
            sneakyThrow(new OvidMishap("Требуется живая сущность"));
            return null;
        }

        try {
            env.assertEntityInRange(target);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        if (!(rawSeconds >= MIN_SECONDS) || !Double.isFinite(rawSeconds)) {
            sneakyThrow(MishapInvalidIota.ofType(args.get(1), 0, "double.positive"));
            return null;
        }
        int seconds = (int) Math.min(MAX_SECONDS, rawSeconds);
        int ticks = seconds * 20;

        ServerPlayer caster = null;
        try {
            caster = env.getCaster();
        } catch (Throwable ignored) {
        }
        UUID casterId = caster != null ? caster.getUUID() : null;

        long cost = (long) seconds * COST_PER_SECOND;

        Vec3 eye = living.getEyePosition();
        List<ParticleSpray> particles = List.of(
                ParticleSpray.burst(eye, 1.5, 30),
                ParticleSpray.cloud(eye, 1.0, 20)
        );

        return new SpellAction.Result(new PingPongSpell(living, ticks, casterId), cost, particles, 0);
    }

    /** Rendered stage: вешает маркер и записывает привязку. */
    public static class PingPongSpell implements RenderedSpell {
        private final LivingEntity target;
        private final int ticks;
        private final UUID casterId;

        public PingPongSpell(LivingEntity target, int ticks, UUID casterId) {
            this.target = target;
            this.ticks = ticks;
            this.casterId = casterId;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            MobEffectInstance instance = new MobEffectInstance(HexEffects.PING_PONG, ticks, 0, false, true, true);
            target.addEffect(instance);
            if (casterId != null) {
                PingPongHandler.bind(target.getUUID(), casterId);
            }
        }
    }
}

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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getDouble;
import static at.petrak.hexcasting.api.casting.OperatorUtils.getEntity;

/**
 * Морф-хекс — превращает чужого игрока в курицу.
 * <p>
 * Стек: [entity, double seconds] — цель (только другой игрок, не себя, не мобы)
 * и длительность. Секунды мягко обрезаются в [1, 5]: ниже 1 — mishap, выше 5 — кламп.
 * Стоимость — 2500 маны/сек ({@code seconds * 2_500_000} media).
 * <p>
 * Цель: входящий урон x0.2, запрет движения/магии/слотов/атак/блоков/предметов
 * (см. {@link MorphHexHandler}), визуально — курица (клиент-миксин).
 * Магия режется готовым {@link HexEffects#SILENCE}. Снятие: молоко, смерть.
 * Урон хекс не развеивает. Повторный каст обновляет срок.
 * Сигнатура qaqwawdq (EAST).
 */
public class OpMorphHex implements SpellAction {

    public static final OpMorphHex INSTANCE = new OpMorphHex();

    private static final double MIN_SECONDS = 1.0;
    private static final double MAX_SECONDS = 5.0;
    /** 2500 маны/сек (1 мана = 1000 media). */
    private static final int COST_PER_SECOND = 2_500_000;

    private OpMorphHex() {
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
        return SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        Entity targetRaw;
        double rawSeconds;
        try {
            targetRaw = getEntity(args, 0, getArgc());
            rawSeconds = getDouble(args, 1, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        if (!(targetRaw instanceof Player playerTarget)) {
            sneakyThrow(new OvidMishap("Морф только по игрокам"));
            return null;
        }

        ServerPlayer caster = null;
        try {
            caster = env.getCaster();
        } catch (Throwable ignored) {
        }
        if (caster != null && caster.getUUID().equals(playerTarget.getUUID())) {
            sneakyThrow(new OvidMishap("Нельзя морфить себя"));
            return null;
        }

        try {
            env.assertEntityInRange(targetRaw);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        if (!(rawSeconds >= MIN_SECONDS) || !Double.isFinite(rawSeconds)) {
            sneakyThrow(MishapInvalidIota.ofType(args.get(1), 0, "double.positive"));
            return null;
        }
        int seconds = (int) Math.min(MAX_SECONDS, rawSeconds);
        int ticks = seconds * 20;

        long cost = (long) seconds * COST_PER_SECOND;

        Vec3 eye = playerTarget.getEyePosition();
        List<ParticleSpray> particles = List.of(
                ParticleSpray.burst(eye, 1.5, 30),
                ParticleSpray.cloud(eye, 1.0, 20));

        return new SpellAction.Result(new MorphSpell(playerTarget, ticks), cost, particles, 0);
    }

    /** Rendered stage: маркеры превращения + сайленса, локдаун через хендлер, пуф. */
    public static class MorphSpell implements RenderedSpell {
        private final Player target;
        private final int ticks;

        public MorphSpell(Player target, int ticks) {
            this.target = target;
            this.ticks = ticks;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            target.addEffect(new MobEffectInstance(HexEffects.MORPH_HEX, ticks, 0, false, true, true));
            target.addEffect(new MobEffectInstance(HexEffects.SILENCE, ticks, 0, false, true, true));
            if (target instanceof ServerPlayer sp) {
                MorphHexHandler.morph(sp);
                if (env.getWorld() instanceof ServerLevel sl) {
                    // Дота-пуф превращения.
                    sl.sendParticles(ParticleTypes.POOF,
                            target.getX(), target.getY() + 0.6, target.getZ(),
                            25, 0.4, 0.5, 0.4, 0.05);
                }
            }
        }
    }
}

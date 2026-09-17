package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getEntity;

/**
 * Gerd's Substitution — обращает дар во вред.
 * <p>
 * Стек: [entity] — живая цель (можно себя), должна быть в радиусе каста.
 * С цели снимается один случайный положительный эффект
 * ({@link MobEffectCategory#BENEFICIAL}, включая модовые), а вместо него
 * накладывается случайный ванильный недуг ({@code minecraft}, категория
 * {@link MobEffectCategory#HARMFUL}, без мгновенных — урон/лечение).
 * Новый недуг наследует длительность и усилитель снятого баффа
 * (усилитель мягко обрезается до 0–4, бесконечная длительность — до минуты).
 * Если положительных эффектов на цели нет — mishap.
 * Если цель невосприимчива к выпавшему недугу (например, нежить к яду),
 * жребий тянется заново; если не лёг ни один — бафф возвращается как был.
 * Стоимость — 30 пыли, фиксированная.
 */
public class OpGerdSubstitution implements SpellAction {

    public static final OpGerdSubstitution INSTANCE = new OpGerdSubstitution();

    private static final long COST = 30L * 10000L; // 30 dust
    private static final int MAX_AMPLIFIER = 4;
    private static final int INFINITE_FALLBACK_TICKS = 20 * 60;

    private OpGerdSubstitution() {
    }

    @Override
    public int getArgc() {
        return 1;
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
        try {
            target = getEntity(args, 0, getArgc());
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

        List<MobEffectInstance> beneficial = new ArrayList<>();
        for (MobEffectInstance instance : living.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                beneficial.add(instance);
            }
        }
        if (beneficial.isEmpty()) {
            sneakyThrow(new OvidMishap("Нет положительных эффектов"));
            return null;
        }

        MobEffectInstance stolen = beneficial.get(living.level().random.nextInt(beneficial.size()));
        int duration = stolen.getDuration();
        if (duration == MobEffectInstance.INFINITE_DURATION) {
            duration = INFINITE_FALLBACK_TICKS;
        }
        int amplifier = Math.min(Math.max(stolen.getAmplifier(), 0), MAX_AMPLIFIER);

        Vec3 eye = living.getEyePosition();
        List<ParticleSpray> particles = List.of(
                ParticleSpray.burst(eye, 1.5, 30),
                ParticleSpray.cloud(eye, 1.0, 20)
        );

        return new SpellAction.Result(new SubstitutionSpell(living, stolen, duration, amplifier), COST, particles, 0);
    }

    /** Пул собирается лениво и один раз: ванильные HARMFUL без мгновенных. */
    private static volatile List<Holder<MobEffect>> HARMFUL_POOL = null;

    static List<Holder<MobEffect>> harmfulPool() {
        List<Holder<MobEffect>> pool = HARMFUL_POOL;
        if (pool == null) {
            synchronized (OpGerdSubstitution.class) {
                pool = HARMFUL_POOL;
                if (pool == null) {
                    List<Holder<MobEffect>> built = new ArrayList<>();
                    BuiltInRegistries.MOB_EFFECT.holders().forEach(holder -> {
                        ResourceLocation id = holder.key().location();
                        MobEffect fx = holder.value();
                        if (id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)
                                && fx.getCategory() == MobEffectCategory.HARMFUL
                                && !fx.isInstantenous()) {
                            built.add(holder);
                        }
                    });
                    HARMFUL_POOL = pool = Collections.unmodifiableList(built);
                }
            }
        }
        return pool;
    }

    /** Rendered stage: снимает бафф, тянет жребий, накладывает недуг. */
    public static class SubstitutionSpell implements RenderedSpell {
        private final LivingEntity target;
        private final MobEffectInstance stolen;
        private final int duration;
        private final int amplifier;

        public SubstitutionSpell(LivingEntity target, MobEffectInstance stolen, int duration, int amplifier) {
            this.target = target;
            this.stolen = stolen;
            this.duration = duration;
            this.amplifier = amplifier;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            List<Holder<MobEffect>> pool = new ArrayList<>(harmfulPool());
            if (pool.isEmpty()) {
                return; // не должно случаться: в ванили есть недуги
            }
            target.removeEffect(stolen.getEffect());
            Collections.shuffle(pool, new java.util.Random(target.level().random.nextLong()));
            for (Holder<MobEffect> pick : pool) {
                if (pick.value() == stolen.getEffect().value()) {
                    continue; // не подменяем эффект самим собой
                }
                MobEffectInstance curse = new MobEffectInstance(pick, Math.max(1, duration), amplifier, false, true, true);
                if (target.addEffect(curse)) {
                    return;
                }
            }
            // Ничего не легло (например, сплошные иммунитеты) — возвращаем бафф как был.
            target.addEffect(new MobEffectInstance(stolen.getEffect(),
                    stolen.getDuration(), stolen.getAmplifier(), stolen.isAmbient(), stolen.isVisible(), stolen.showIcon()));
        }
    }
}

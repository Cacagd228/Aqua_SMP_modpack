package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import me.nanorasmus.nanodev.hex_js.entity.EntityHadesSummon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getEntity;

/**
 * Цепь Эреба — связывает сущность с призванным зомби кастера.
 * Стек: [entity, entity] — кого привязать и к какому зомби.
 * Пока действует привязка (на сущности висит эффект "Привязь Эреба"):
 * 30% входящего урона остаётся на сущности, 70% в двойном объёме
 * уходит на привязанного зомби. Upkeep — мана/сек с кастера,
 * привязка живёт пока жив зомби. Сигнатура qaqwawdeq (EAST).
 */
public class OpErebusChain implements SpellAction {

    public static final OpErebusChain INSTANCE = new OpErebusChain();
    private static final long FIXED_COST = 50L * 10000L; // 50 dust

    private OpErebusChain() {
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
        Entity zombieRaw;
        try {
            targetRaw = getEntity(args, 0, getArgc());
            zombieRaw = getEntity(args, 1, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        if (!(targetRaw instanceof LivingEntity)) {
            sneakyThrow(new OvidMishap("Привязать можно только живую сущность"));
        }
        if (!(zombieRaw instanceof EntityHadesSummon)) {
            sneakyThrow(new OvidMishap("Второй аргумент — призванный зомби"));
        }
        if (targetRaw.getUUID().equals(zombieRaw.getUUID())) {
            sneakyThrow(new OvidMishap("Нельзя привязать зомби к самому себе"));
        }

        ServerPlayer caster = null;
        try {
            caster = env.getCaster();
        } catch (Throwable ignored) {
        }
        if (caster == null) {
            sneakyThrow(new OvidMishap("Нет кастера"));
        }
        UUID casterId = caster.getUUID();
        EntityHadesSummon summon = (EntityHadesSummon) zombieRaw;
        if (!casterId.equals(summon.getCasterId())) {
            sneakyThrow(new OvidMishap("Чужой зомби — привязать можно только своего"));
        }

        Vec3 at = targetRaw.position().add(0, targetRaw.getBbHeight() * 0.5, 0);
        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(at, 0.8, 15),
                ParticleSpray.burst(at, 0.8, 20));

        return new SpellAction.Result(
                new Spell(targetRaw.getUUID(), summon.getUUID(), casterId), FIXED_COST, particles, 0);
    }

    public static class Spell implements RenderedSpell {
        private final UUID targetId;
        private final UUID summonId;
        private final UUID casterId;

        public Spell(UUID targetId, UUID summonId, UUID casterId) {
            this.targetId = targetId;
            this.summonId = summonId;
            this.casterId = casterId;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            Entity targetRaw = world.getEntity(targetId);
            Entity summonRaw = world.getEntity(summonId);
            if (!(targetRaw instanceof LivingEntity target) || !(summonRaw instanceof EntityHadesSummon summon)) {
                return;
            }
            if (!summon.isAlive() || !casterId.equals(summon.getCasterId())) {
                return;
            }
            ServerPlayer caster = null;
            try {
                caster = env.getCaster();
            } catch (Throwable ignored) {
            }
            if (caster == null || !caster.getUUID().equals(casterId)) {
                return;
            }
            ErebusChainHandler.bind(caster, target, summon);
        }
    }
}

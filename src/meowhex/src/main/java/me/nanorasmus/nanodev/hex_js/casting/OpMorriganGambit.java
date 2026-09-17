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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getEntity;

/**
 * Гамбит Морриган — натравливает ВСЕХ призванных слуг кастера (зомби Аида
 * и тени Стикса) на одну цель.
 * Стек: [entity] — метка. Приказ держится пока метка жива;
 * новый гамбит переназначает. Бьёт даже самого кастера и своих —
 * запреты обычного ИИ на приказ не действуют. Сигнатура qaqwawdeqw (EAST).
 */
public class OpMorriganGambit implements SpellAction {

    public static final OpMorriganGambit INSTANCE = new OpMorriganGambit();
    private static final long FIXED_COST = 30L * 10000L; // 30 dust

    private OpMorriganGambit() {
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
        Entity mark;
        try {
            mark = getEntity(args, 0, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        ServerPlayer caster = null;
        try {
            caster = env.getCaster();
        } catch (Throwable ignored) {
        }
        if (caster == null) {
            sneakyThrow(new OvidMishap("Нет кастера"));
        }
        var zombies = EntityHadesSummon.getSummonsForCaster(caster.getUUID());
        var shades = me.nanorasmus.nanodev.hex_js.entity.EntityStyxShade.getSummonsForCaster(caster.getUUID());
        if (zombies.isEmpty() && shades.isEmpty()) {
            sneakyThrow(new OvidMishap("Нет призванных слуг"));
        }

        Vec3 at = mark.position().add(0, mark.getBbHeight() * 0.5, 0);
        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(at, 0.8, 15),
                ParticleSpray.burst(at, 0.8, 20));

        return new SpellAction.Result(
                new Spell(mark.getUUID(), caster.getUUID()), FIXED_COST, particles, 0);
    }

    public static class Spell implements RenderedSpell {
        private final UUID markId;
        private final UUID casterId;

        public Spell(UUID markId, UUID casterId) {
            this.markId = markId;
            this.casterId = casterId;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            var server = world.getServer();
            Entity mark = null;
            for (ServerLevel sl : server.getAllLevels()) {
                mark = sl.getEntity(markId);
                if (mark != null) break;
            }
            if (mark == null) return;
            int commanded = 0;
            for (UUID summonId : EntityHadesSummon.getSummonsForCaster(casterId)) {
                Entity raw = null;
                for (ServerLevel sl : server.getAllLevels()) {
                    raw = sl.getEntity(summonId);
                    if (raw != null) break;
                }
                if (raw instanceof EntityHadesSummon summon
                        && summon.isAlive()
                        && casterId.equals(summon.getCasterId())) {
                    summon.setForcedTarget(markId);
                    commanded++;
                }
            }
            for (UUID summonId : me.nanorasmus.nanodev.hex_js.entity.EntityStyxShade.getSummonsForCaster(casterId)) {
                Entity raw = null;
                for (ServerLevel sl : server.getAllLevels()) {
                    raw = sl.getEntity(summonId);
                    if (raw != null) break;
                }
                if (raw instanceof me.nanorasmus.nanodev.hex_js.entity.EntityStyxShade summon
                        && summon.isAlive()
                        && casterId.equals(summon.getCasterId())) {
                    summon.setForcedTarget(markId);
                    commanded++;
                }
            }
            if (commanded == 0) return;
            // Метка известна — слуги сами возьмут её в ForcedTargetGoal на следующем тике.
        }
    }
}

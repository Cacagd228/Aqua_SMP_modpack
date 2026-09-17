package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Hades' Recall — отзыв всего призванного воинства кастера.
 * Стек: [] — развеивает всех зомби Аида и все тени Стикса кастера
 * (до 5 каждых). Стоимость 0. Цепи Эреба на изгнанных зомби рвутся сами
 * штатной валидацией (зомби мёртв — unbind).
 */
public class OpHadesRecall implements SpellAction {

    public static final OpHadesRecall INSTANCE = new OpHadesRecall();

    private OpHadesRecall() {}

    @Override
    public int getArgc() { return 0; }

    @Override
    public boolean hasCastingSound(CastingEnvironment env) { return true; }

    @Override
    public boolean awardsCastingStat(CastingEnvironment env) { return true; }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation cont) {
        return SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @Override
    public Result execute(List<? extends Iota> args, CastingEnvironment env) {
        ServerPlayer caster = null;
        try { caster = env.getCaster(); } catch (Throwable ignored) {}
        if (caster == null) {
            // No caster — nothing to recall, but not mishap
            return new Result(new Spell(null), 0, List.of(), 0);
        }
        Vec3 pos = caster.position();
        List<ParticleSpray> particles = List.of(ParticleSpray.cloud(pos.add(0, 1, 0), 1.0, 10));
        return new Result(new Spell(caster.getUUID()), 0, particles, 0);
    }

    public static class Spell implements RenderedSpell {
        private final UUID casterId;

        public Spell(UUID casterId) { this.casterId = casterId; }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            if (casterId == null) return;
            ServerLevel world = env.getWorld();
            for (UUID id : new java.util.ArrayList<>(
                    me.nanorasmus.nanodev.hex_js.entity.EntityHadesSummon.getSummonsForCaster(casterId))) {
                var ent = world.getEntity(id);
                if (ent != null) ent.discard();
            }
            me.nanorasmus.nanodev.hex_js.entity.EntityHadesSummon.untrackCaster(casterId);
            for (UUID id : new java.util.ArrayList<>(
                    me.nanorasmus.nanodev.hex_js.entity.EntityStyxShade.getSummonsForCaster(casterId))) {
                var ent = world.getEntity(id);
                if (ent != null) ent.discard();
            }
            me.nanorasmus.nanodev.hex_js.entity.EntityStyxShade.untrackCaster(casterId);
        }
    }
}

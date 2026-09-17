package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.BooleanIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Тень Стикса — как Призыв Аида, но векс и в 1.5 раза дороже:
 * призыв 150 пыли, upkeep 30 маны/сек, лимит 5 на кастера.
 * Стек: [vector, vector] — позиция и взгляд. Сигнатура qaqwawdeqd (EAST).
 */
public class OpStyxShade implements SpellAction {

    public static final OpStyxShade INSTANCE = new OpStyxShade();
    private static final long FIXED_COST = 150L * 10000L; // 150 dust

    private OpStyxShade() {}

    @Override
    public int getArgc() { return 2; }

    @Override
    public boolean hasCastingSound(CastingEnvironment env) { return true; }

    @Override
    public boolean awardsCastingStat(CastingEnvironment env) { return true; }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation cont) {
        return SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T { throw (T) t; }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        Vec3 spawnVec;
        Vec3 lookVec;
        try {
            spawnVec = at.petrak.hexcasting.api.casting.OperatorUtils.getVec3(args, 0, getArgc());
            lookVec = at.petrak.hexcasting.api.casting.OperatorUtils.getVec3(args, 1, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        try {
            env.assertVecInRange(spawnVec);
            env.assertVecInWorld(spawnVec);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
        if (!env.isVecInWorld(spawnVec)) {
            sneakyThrow(new OvidMishap("Блок вне мира"));
        }

        ServerLevel world = env.getWorld();
        ServerPlayer caster = null;
        try { caster = env.getCaster(); } catch (Throwable ignored) {}
        UUID casterId = caster != null ? caster.getUUID() : null;

        if (casterId != null && me.nanorasmus.nanodev.hex_js.entity.EntityStyxShade.countForCaster(casterId) >= 5) {
            sneakyThrow(new OvidMishap("Максимум 5 теней"));
        }

        long cost = FIXED_COST;
        BlockPos pos = BlockPos.containing(spawnVec);
        Vec3 spawnPos = spawnVec.add(0, 0.5, 0);
        try { env.assertVecInRange(spawnPos); } catch (Throwable t) { sneakyThrow(t); }

        Vec3 normLook = lookVec.normalize();
        if (normLook.lengthSqr() < 1e-6) normLook = new Vec3(0, 0, 1);

        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(spawnPos, 1.0, 20),
                ParticleSpray.burst(spawnPos, 1.0, 30)
        );

        return new SpellAction.Result(new Spell(spawnPos, normLook, casterId), cost, particles, 0);
    }

    public static class Spell implements RenderedSpell {
        private final Vec3 spawnPos;
        private final Vec3 look;
        private final UUID casterId;

        public Spell(Vec3 spawnPos, Vec3 look, UUID casterId) {
            this.spawnPos = spawnPos;
            this.look = look;
            this.casterId = casterId;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            var type = me.nanorasmus.nanodev.hex_js.entity.HexEntities.STYX_SHADE.get();
            var summon = new me.nanorasmus.nanodev.hex_js.entity.EntityStyxShade(type, world);
            summon.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            if (look != null && look.lengthSqr() > 1e-6) {
                summon.setLookVec(look);
                double yaw = Math.toDegrees(Math.atan2(-look.x, look.z));
                double pitch = Math.toDegrees(Math.atan2(-look.y, Math.sqrt(look.x*look.x + look.z*look.z)));
                summon.setYRot((float) yaw);
                summon.setXRot((float) pitch);
                summon.yHeadRot = (float) yaw;
                summon.yBodyRot = (float) yaw;
            }
            if (casterId != null) summon.setCaster(casterId);
            summon.setPersistenceRequired();
            world.addFreshEntity(summon);
            if (casterId != null) {
                me.nanorasmus.nanodev.hex_js.entity.EntityStyxShade.trackCaster(casterId, summon.getUUID());
            }
        }
    }
}

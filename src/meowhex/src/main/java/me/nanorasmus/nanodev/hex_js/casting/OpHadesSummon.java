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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class OpHadesSummon implements SpellAction {

    public static final OpHadesSummon INSTANCE = new OpHadesSummon();
    private static final long FIXED_COST = 100L * 10000L; // 100 dust

    private OpHadesSummon() {}

    @Override
    public int getArgc() { return 3; }

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
        boolean small;
        try {
            spawnVec = at.petrak.hexcasting.api.casting.OperatorUtils.getVec3(args, 0, getArgc());
            lookVec = at.petrak.hexcasting.api.casting.OperatorUtils.getVec3(args, 1, getArgc());
            small = at.petrak.hexcasting.api.casting.OperatorUtils.getBool(args, 2, getArgc());
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

        if (casterId != null && me.nanorasmus.nanodev.hex_js.entity.EntityHadesSummon.countForCaster(casterId) >= 5) {
            sneakyThrow(new OvidMishap("Максимум 5 призывов"));
        }

        long cost = FIXED_COST;
        BlockPos pos = BlockPos.containing(spawnVec);
        Vec3 spawnPos = spawnVec.add(0, 0.5, 0);
        try { env.assertVecInRange(spawnPos); } catch (Throwable t) { sneakyThrow(t); }

        Vec3 normLook = lookVec.normalize();
        if (normLook.lengthSqr() < 1e-6) normLook = new Vec3(0, 0, 1);

        boolean isDesert = false;
        try {
            isDesert = world.getBiome(pos).is(net.minecraft.world.level.biome.Biomes.DESERT);
        } catch (Throwable ignored) {}

        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(spawnPos, 1.0, 20),
                ParticleSpray.burst(spawnPos, 1.0, 30)
        );

        return new SpellAction.Result(new Spell(spawnPos, normLook, casterId, small, isDesert), cost, particles, 0);
    }

    public static class Spell implements RenderedSpell {
        private final Vec3 spawnPos;
        private final Vec3 look;
        private final UUID casterId;
        private final boolean small;
        private final boolean isDesert;

        public Spell(Vec3 spawnPos, Vec3 look, UUID casterId, boolean small, boolean isDesert) {
            this.spawnPos = spawnPos;
            this.look = look;
            this.casterId = casterId;
            this.small = small;
            this.isDesert = isDesert;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            var type = me.nanorasmus.nanodev.hex_js.entity.HexEntities.HADES_SUMMON.get();
            var summon = new me.nanorasmus.nanodev.hex_js.entity.EntityHadesSummon(type, world);
            summon.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            summon.setSmall(small);
            summon.setHusk(isDesert);
            summon.setHealth(small ? 10.0f : 20.0f);
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
                me.nanorasmus.nanodev.hex_js.entity.EntityHadesSummon.trackCaster(casterId, summon.getUUID());
            }
        }
    }
}

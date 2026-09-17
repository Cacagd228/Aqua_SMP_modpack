package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Hermes' Deception — создаёт пустышку, копирующую внешность.
 * Стек: [Entity, Vec3, Vec3] где Entity — LivingEntity/Mob для копирования (модель/текстура/размер/хитбокс/свечение/экипировка/поза/имя), Vec3 — где спавнить, Vec3 — взгляд.
 * Пустышка: NoAI, с гравитацией+физикой, hitbox как у оригинала, не толкает, но толкаема, 1 HP, 5 минут, можно убить атакой.
 * Стоимость зависит от размера моба, максимум 1 копия на кастера (старая удаляется).
 */
public class OpHermesDeception implements SpellAction {

    public static final OpHermesDeception INSTANCE = new OpHermesDeception();

    private static final long FIXED_COST = 100L * 10000L; // 100 dust fixed

    private OpHermesDeception() {}

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
    public Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T { throw (T) t; }

    @Override
    public Result execute(List<? extends Iota> args, CastingEnvironment env) {
        Entity ent;
        Vec3 vec;
        Vec3 look;
        try {
            ent = at.petrak.hexcasting.api.casting.OperatorUtils.getEntity(args, 0, getArgc());
            vec = at.petrak.hexcasting.api.casting.OperatorUtils.getVec3(args, 1, getArgc());
            look = at.petrak.hexcasting.api.casting.OperatorUtils.getVec3(args, 2, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        if (!(ent instanceof LivingEntity living)) {
            sneakyThrow(new OvidMishap("Требуется LivingEntity/Mob"));
            return null;
        }
        // Only Living/Mob allowed
        if (!(living instanceof Mob) && !(living instanceof LivingEntity)) {
            sneakyThrow(new OvidMishap("Требуется LivingEntity/Mob"));
        }

        try {
            env.assertEntityInRange(ent);
            env.assertVecInRange(vec);
            env.assertVecInWorld(vec);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
        if (!env.isVecInWorld(vec)) {
            sneakyThrow(new OvidMishap("Блок вне мира"));
        }

        ServerLevel world = env.getWorld();
        ServerPlayer caster = null;
        try {
            caster = env.getCaster();
        } catch (Throwable ignored) {}
        UUID casterId = caster != null ? caster.getUUID() : null;

        if (casterId != null && me.nanorasmus.nanodev.hex_js.entity.EntityDeception.countForCaster(casterId) >= 5) {
            sneakyThrow(new OvidMishap("Максимум 5 обманок"));
        }

        // Fixed cost 100 dust, no size dependence
        long cost = FIXED_COST;

        BlockPos pos = BlockPos.containing(vec);
        Vec3 spawnPos = vec.add(0, 0.5, 0);
        try { env.assertVecInRange(spawnPos); } catch (Throwable t) { sneakyThrow(t); }

        // Normalize look
        Vec3 normLook = look.normalize();
        if (normLook.lengthSqr() < 1e-6) normLook = new Vec3(0, 0, 1);

        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(spawnPos, 1.0, 20),
                ParticleSpray.burst(spawnPos, 1.0, 30),
                ParticleSpray.cloud(ent.position().add(0, ent.getBbHeight()/2, 0), 1.0, 15)
        );

        return new Result(new Spell(living, spawnPos, normLook, casterId), cost, particles, 0);
    }

    public static class Spell implements RenderedSpell {
        private final LivingEntity target;
        private final Vec3 spawnPos;
        private final Vec3 look;
        private final UUID casterId;

        public Spell(LivingEntity target, Vec3 spawnPos, Vec3 look, UUID casterId) {
            this.target = target;
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

            // Create deception
            var type = me.nanorasmus.nanodev.hex_js.entity.HexEntities.DECEPTION.get();
            var deception = new me.nanorasmus.nanodev.hex_js.entity.EntityDeception(type, world);
            deception.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            // Copy appearance
            deception.setTargetFrom(target);
            // Apply look vector
            if (look != null && look.lengthSqr() > 1e-6) {
                double yaw = Math.toDegrees(Math.atan2(-look.x, look.z));
                double pitch = Math.toDegrees(Math.atan2(-look.y, Math.sqrt(look.x*look.x + look.z*look.z)));
                deception.setYRot((float) yaw);
                deception.setXRot((float) pitch);
                deception.yHeadRot = (float) yaw;
                deception.yBodyRot = (float) yaw;
                deception.setLookFromVec(look);
            }
            if (casterId != null) deception.setCaster(casterId);
            // Ensure 1 HP
            deception.setHealth(1.0f);
            world.addFreshEntity(deception);
            // Track
            if (casterId != null) {
                me.nanorasmus.nanodev.hex_js.entity.EntityDeception.trackCaster(casterId, deception.getUUID());
            }
        }
    }
}

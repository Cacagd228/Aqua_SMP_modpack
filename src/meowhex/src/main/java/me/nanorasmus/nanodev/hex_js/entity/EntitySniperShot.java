package me.nanorasmus.nanodev.hex_js.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The Deadeye round: a fast homing projectile fired by {@code OpDeadeye}. It flies
 * straight through any blocks (no physics / no collision) toward its target and, on
 * arrival, strips 40% of the target's max HP as magic damage. The visual is a plain
 * vanilla arrow model with a tracer trail.
 */
public class EntitySniperShot extends Arrow {

    /** Blocks travelled per tick. */
    private static final double SPEED = 3.0;
    /** Distance (to the target's eye) at which the round "arrives" and deals damage. */
    private static final double ARRIVE_DIST = 1.0;
    /** Hard timeout in ticks so a lost round never lives forever. */
    private static final int MAX_AGE = 220;
    /** Fraction of max health stripped on a successful hit. */
    private static final float DMG_FRACTION = 0.4f;

    private static final int TRACER_COLOR = 0xFF_FFAA00;

    private UUID targetId;
    private LivingEntity targetCache;
    private UUID ownerId;
    private int color = TRACER_COLOR;

    public EntitySniperShot(EntityType<? extends Arrow> type, Level level) {
        super(type, level);
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
        this.setNoGravity(true);
        this.setNoPhysics(true);
    }

    public EntitySniperShot(Level level, double x, double y, double z, LivingEntity target, LivingEntity owner, int color) {
        this((EntityType<? extends Arrow>) HexEntities.SNIPER_SHOT.get(), level);
        this.setPos(x, y, z);
        this.setDeltaMovement(0.0, 0.0, 0.0);
        if (target != null) {
            this.targetId = target.getUUID();
        }
        if (owner != null) {
            this.ownerId = owner.getUUID();
        }
        this.color = color;
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) {
            // Keep the local copy gliding along the last velocity so the renderer
            // looks smooth between server position packets.
            this.baseTick();
            Vec3 d = this.getDeltaMovement();
            if (d.lengthSqr() > 1e-9) {
                this.move(MoverType.SELF, d);
            }
            return;
        }
        this.baseTick();
        this.tickServer();
    }

    private void tickServer() {
        ServerLevel level = (ServerLevel) this.level();

        if (this.tickCount > MAX_AGE) {
            this.fizzle();
            return;
        }

        LivingEntity target = this.targetCache;
        if (target == null || !target.isAlive() || target.level() != level || !target.isAddedToLevel()) {
            target = level.getEntity(this.targetId) instanceof LivingEntity le ? le : null;
            this.targetCache = target;
            if (target == null || !target.isAlive() || target.level() != level) {
                this.fizzle();
                return;
            }
        }

        Vec3 aim = target.getEyePosition();
        Vec3 pos = this.position();
        double dist = aim.distanceTo(pos);

        if (dist <= ARRIVE_DIST) {
            this.impact(target);
            return;
        }

        Vec3 dir = aim.subtract(pos).normalize();
        double stepLen = Math.min(SPEED, dist - ARRIVE_DIST);
        if (stepLen < 0.05) {
            stepLen = 0.05;
        }
        Vec3 step = dir.scale(stepLen);
        this.setDeltaMovement(step);
        this.faceTowards(step);
        this.move(MoverType.SELF, step);

        if (this.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.02);
        }
    }

    /** Rotates the model to point along the travel direction. */
    private void faceTowards(Vec3 d) {
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) Math.toDegrees(Math.atan2(d.x, d.z));
        float pitch = (float) Math.toDegrees(Math.atan2(d.y, horiz));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private void impact(LivingEntity target) {
        if (target.isAlive()) {
            target.hurt(this.damageSources().magic(), target.getMaxHealth() * DMG_FRACTION);
        }
        ServerLevel level = (ServerLevel) this.level();
        Vec3 p = target.getEyePosition();
        level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 40, 0.4, 0.4, 0.4, 0.2);
        level.playSound(null, p.x, p.y, p.z, SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.2f, 0.8f);
        this.discard();
    }

    private void fizzle() {
        ServerLevel level = (ServerLevel) this.level();
        level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 8, 0.1, 0.1, 0.1, 0.05);
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.targetId != null) {
            tag.putUUID("Target", this.targetId);
        }
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        tag.putInt("ShotColor", this.color);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Target")) {
            this.targetId = tag.getUUID("Target");
        }
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }
        this.color = tag.getInt("ShotColor");
    }
}

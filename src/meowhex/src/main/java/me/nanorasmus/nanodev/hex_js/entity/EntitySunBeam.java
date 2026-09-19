package me.nanorasmus.nanodev.hex_js.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Sun Strike beam: a purely visual vertical pillar of light, ~1.25 s.
 * Damage is dealt instantly by the spell itself; this entity only renders
 * the beam (see SunBeamRenderer) and throws a few sparks.
 */
public class EntitySunBeam extends Entity {
    /** Lifetime in ticks. */
    public static final int MAX_AGE = 25;
    /** Beam height in blocks. */
    public static final float HEIGHT = 32.0f;

    public EntitySunBeam(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public EntitySunBeam(Level level, double x, double y, double z) {
        this(HexEntities.SUN_BEAM.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > MAX_AGE) {
            this.discard();
            return;
        }
        if (this.level().isClientSide) {
            return;
        }
        // Rising sparks along the column.
        double x = this.getX() + (this.random.nextDouble() - 0.5) * 1.6;
        double y = this.getY() + this.random.nextDouble() * HEIGHT * 0.6;
        double z = this.getZ() + (this.random.nextDouble() - 0.5) * 1.6;
        ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                x, y, z, 2, 0.05, 0.6, 0.05, 0.02);
    }
}

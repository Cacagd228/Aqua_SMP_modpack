package me.nanorasmus.nanodev.hex_js.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Apollo's Arrow — стрела +10% к луку, трейсер HEX 1/3т.
 */
public class EntityApolloArrow extends Arrow {

    private int pigmentColor = 0xFF_FF00FF; // default hex color magenta
    private boolean useHexParticle = true;

    public EntityApolloArrow(EntityType<? extends Arrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.ALLOWED;
    }

    public EntityApolloArrow(Level level, double x, double y, double z, ItemStack pickupItem, int color) {
        super(HexEntities.APOLLO_ARROW.get(), level);
        this.setPos(x, y, z);
        this.setPickupItemStack(pickupItem.copy());
        this.pigmentColor = color;
        this.pickup = Pickup.ALLOWED;
    }

    public void setPigmentColor(int color) {
        this.pigmentColor = color;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount % 3 == 0 && !this.inGround) {
            try {
                ServerLevel sl = (ServerLevel) this.level();
                ParticleOptions particle;
                try {
                    particle = new at.petrak.hexcasting.common.particles.ConjureParticleOptions(pigmentColor);
                } catch (Throwable t) {
                    particle = net.minecraft.core.particles.ParticleTypes.CRIT;
                }
                sl.sendParticles(particle, this.getX(), this.getY() + 0.1, this.getZ(), 1, 0, 0, 0, 0);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }
}

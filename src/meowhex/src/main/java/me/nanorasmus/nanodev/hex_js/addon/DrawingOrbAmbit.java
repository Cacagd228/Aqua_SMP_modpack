package me.nanorasmus.nanodev.hex_js.addon;

import at.petrak.hexcasting.api.addldata.ADIotaHolder;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironmentComponent;
import at.petrak.hexcasting.api.casting.iota.EntityIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironmentComponent.Key;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemDrawingOrb;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Hextended Drawing Orb mechanism: while the caster holds a {@link ItemDrawingOrb}
 * containing an {@link EntityIota}, their casting ambit is extended to that entity's
 * position — enabling casts at far-away entities.
 */
public class DrawingOrbAmbit implements CastingEnvironmentComponent.IsVecInRange {
    private final CastingEnvironment env;

    public static class DrawingOrbKey implements Key<IsVecInRange> {
    }

    public DrawingOrbAmbit(CastingEnvironment env) {
        this.env = env;
    }

    @Override
    public Key<IsVecInRange> getKey() {
        return new DrawingOrbKey();
    }

    @Override
    public boolean onIsVecInRange(Vec3 vec, boolean current) {
        LivingEntity caster = env.getCaster();
        if (caster == null) {
            return current;
        }

        ItemStack orb = null;
        // a drawing orb must be held in either hand
        if (caster.getOffhandItem().getItem() instanceof ItemDrawingOrb offhandOrb) {
            orb = caster.getOffhandItem();
        }
        if (caster.getMainHandItem().getItem() instanceof ItemDrawingOrb mainhandOrb) {
            orb = caster.getMainHandItem();
        }
        if (orb == null) {
            return current;
        }

        // an iota must be stored in that drawing orb
        ServerLevel level = env.getWorld();
        ADIotaHolder holder = IXplatAbstractions.INSTANCE.findDataHolder(orb);
        if (holder == null) {
            return current;
        }
        Iota datum = holder.readIota(level);
        if (datum == null) {
            return current;
        }

        // that iota must be something we can grant ambit on: an entity
        if (!(datum instanceof EntityIota entityIota)) {
            return current;
        }
        Vec3 target = entityIota.getEntity().position();

        return current || vec.equals(target);
    }
}
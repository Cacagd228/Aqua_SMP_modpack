package me.nanorasmus.nanodev.hex_js.addon;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironmentComponent;
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironmentComponent.Key;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemChargedDiadem;
import me.nanorasmus.nanodev.hex_js.helpers.CurioHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Charged diadem mechanism: while the caster wears an {@link ItemChargedDiadem}
 * (Curios slot), their casting ambit radius is doubled (32 -> 64 blocks).
 */
public class DiademAmbit implements CastingEnvironmentComponent.IsVecInRange {
    /** Doubled ambit radius while the diadem is worn. */
    public static final double DIADEM_AMBIT_RADIUS = PlayerBasedCastEnv.AMBIT_RADIUS * 2.0;

    private final CastingEnvironment env;

    public static class DiademKey implements Key<IsVecInRange> {
    }

    public DiademAmbit(CastingEnvironment env) {
        this.env = env;
    }

    @Override
    public Key<IsVecInRange> getKey() {
        return new DiademKey();
    }

    @Override
    public boolean onIsVecInRange(Vec3 vec, boolean current) {
        if (current) {
            return true;
        }
        if (!(env.getCaster() instanceof ServerPlayer caster)) {
            return false;
        }
        if (!CurioHelper.hasCurio(caster, HextendedItems.CHARGED_AMETHYST_DIADEM.get())) {
            return false;
        }
        return vec.distanceToSqr(caster.position()) <= DIADEM_AMBIT_RADIUS * DIADEM_AMBIT_RADIUS;
    }
}

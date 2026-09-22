package dev.hexsable.casting;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironmentComponent;
import dev.hexsable.HexSable;
import dev.hexsable.HexSableConfig;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.phys.Vec3;

/**
 * Позиции блоков внутри структур Sable живут в далёких "плот"-координатах, из-за чего Hex считает их
 * «слишком далёкими». Это расширение проецирует такую точку в мир и проверяет дальность уже по ней.
 * Так Break Block / Place Block / Ignite и т.д. работают по блокам структуры (координаты — через Structure Projection).
 */
public final class SableRangeComponent implements CastingEnvironmentComponent.IsVecInRange {
    public static final CastingEnvironmentComponent.Key KEY = new CastingEnvironmentComponent.Key() {};

    private static final ThreadLocal<Boolean> REENTRANT = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final CastingEnvironment env;

    private SableRangeComponent(CastingEnvironment env) {
        this.env = env;
    }

    public static void attach(CastingEnvironment env) {
        boolean enabled = true;
        try {
            enabled = HexSableConfig.BLOCK_SPELLS_ON_STRUCTURES.get();
        } catch (IllegalStateException ignored) {
            // конфиг ещё не загружен — оставляем включённым
        }
        if (enabled) {
            env.addExtension(new SableRangeComponent(env));
        }
    }

    @Override
    public CastingEnvironmentComponent.Key getKey() {
        return KEY;
    }

    @Override
    public boolean onIsVecInRange(Vec3 vec, boolean current) {
        if (current || REENTRANT.get()) {
            return current;
        }
        try {
            Vec3 projected = Sable.HELPER.projectOutOfSubLevel(env.getWorld(), vec);
            if (projected == null || projected.equals(vec)) {
                return false; // точка не в плоте структуры
            }
            REENTRANT.set(Boolean.TRUE);
            try {
                return env.isVecInRange(projected);
            } finally {
                REENTRANT.set(Boolean.FALSE);
            }
        } catch (RuntimeException e) {
            HexSable.LOGGER.debug("Sable range projection failed", e);
            return false;
        }
    }
}

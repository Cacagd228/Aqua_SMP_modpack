package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.List;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getEntity;

/**
 * Nurse's Purification (порт из Hexal {@code hexal:health}).
 * <p>
 * Стек: [Entity] — забирает живую сущность со стека и возвращает,
 * сколько здоровья у неё осталось (текущее HP числом).
 * Стоимость 0. Сигнатура: aqwawqa (NORTH_WEST) — как в оригинале.
 */
public class OpNursesPurification implements ConstMediaAction {

    public static final OpNursesPurification INSTANCE = new OpNursesPurification();
    public static final HexPattern PATTERN = HexPattern.fromAngles("aqwawqa", HexDir.NORTH_WEST);

    private OpNursesPurification() {
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public int getArgc() {
        return 1;
    }

    @Override
    public long getMediaCost() {
        return 0L;
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation cont) {
        return ConstMediaAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public ConstMediaAction.CostMediaActionResult executeWithOpCount(List<? extends Iota> args, CastingEnvironment env) {
        return ConstMediaAction.DefaultImpls.executeWithOpCount(this, args, env);
    }

    @Override
    public List<Iota> execute(List<? extends Iota> args, CastingEnvironment env) {
        Entity ent;
        try {
            ent = getEntity(args, 0, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        if (!(ent instanceof LivingEntity living) || ent instanceof ArmorStand) {
            sneakyThrow(new OvidMishap("Требуется живая сущность (не стойка для брони)"));
            return null;
        }

        try {
            env.assertEntityInRange(ent);
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        return List.of(new DoubleIota(living.getHealth()));
    }
}

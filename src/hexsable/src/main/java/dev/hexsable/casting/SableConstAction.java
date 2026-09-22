package dev.hexsable.casting;

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;

import java.util.List;

/**
 * Hex написан на Kotlin без -Xjvm-default, поэтому "методы по умолчанию" интерфейса лежат в DefaultImpls,
 * и Java-реализация обязана делегировать в них вручную. Делаем это один раз здесь.
 */
public abstract class SableConstAction implements ConstMediaAction {
    @Override
    public long getMediaCost() {
        return ConstMediaAction.DefaultImpls.getMediaCost(this);
    }

    @Override
    public ConstMediaAction.CostMediaActionResult executeWithOpCount(List<? extends Iota> args, CastingEnvironment env) {
        return ConstMediaAction.DefaultImpls.executeWithOpCount(this, args, env);
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation continuation) {
        return ConstMediaAction.DefaultImpls.operate(this, env, image, continuation);
    }
}

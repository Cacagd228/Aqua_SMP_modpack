package dev.hexsable.casting;

import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/** См. {@link SableConstAction}: делегирование в Kotlin DefaultImpls. */
public abstract class SableSpellAction implements SpellAction {
    @Override
    public boolean hasCastingSound(CastingEnvironment env) {
        return SpellAction.DefaultImpls.hasCastingSound(this, env);
    }

    @Override
    public boolean awardsCastingStat(CastingEnvironment env) {
        return SpellAction.DefaultImpls.awardsCastingStat(this, env);
    }

    /** Наши заклинания реализуют executeWithUserdata; голый execute — на всякий случай с пустым userData. */
    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        return executeWithUserdata(args, env, new CompoundTag());
    }

    @Override
    public abstract SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userData);

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation continuation) {
        return SpellAction.DefaultImpls.operate(this, env, image, continuation);
    }
}

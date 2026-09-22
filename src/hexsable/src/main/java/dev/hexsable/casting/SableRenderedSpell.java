package dev.hexsable.casting;

import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;

public abstract class SableRenderedSpell implements RenderedSpell {
    @Override
    public CastingImage cast(CastingEnvironment env, CastingImage image) {
        return RenderedSpell.DefaultImpls.cast(this, env, image);
    }
}

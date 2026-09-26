package at.petrak.hexcasting.api.casting.mishaps.circle

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.pigment.FrozenPigment
import net.minecraft.network.chat.Component
import net.minecraft.world.item.DyeColor

/**
 * Circle tried to cast but the Mana Vessel doesn't hold enough media.
 * Amounts are pre-formatted mana strings (player-mana units).
 * Handled exactly like any other circle mishap: message on the impetus,
 * mishap particles, execution halts (see CircleCastEnv.postExecution).
 */
class MishapCircleVesselMedia(
    val needMana: String,
    val haveMana: String,
) : Mishap() {
    override fun accentColor(ctx: CastingEnvironment, errorCtx: Context): FrozenPigment =
        dyeColor(DyeColor.RED)

    override fun execute(env: CastingEnvironment, errorCtx: Context, stack: MutableList<Iota>) {
    }

    override fun errorMessage(ctx: CastingEnvironment, errorCtx: Context): Component =
        error("circle.not_enough_media", needMana, haveMana)
}

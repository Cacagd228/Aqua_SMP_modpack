package at.petrak.hexcasting.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota

/**
 * Render any iota as the text a player would see for it. This is what makes a
 * string iota able to carry a number, a name, or a whole spell's worth of
 * meaning out to the world as text.
 */
object OpIotaString : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> =
        args[0].display().string.asActionResult
}

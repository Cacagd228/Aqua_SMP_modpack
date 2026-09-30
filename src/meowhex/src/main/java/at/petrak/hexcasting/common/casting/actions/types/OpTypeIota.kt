package at.petrak.hexcasting.common.casting.actions.types

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota

/**
 * The iota type of another iota: "what kind of thing is this". The type of a
 * type is a type, so this is its own answer.
 */
object OpTypeIota : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> =
        args[0].type.asActionResult
}

package at.petrak.hexcasting.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getString
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.StringIota

/**
 * Split a string on a separator, pushing each piece back in order. The pieces
 * come out in the order they appear, so the first piece ends up deepest.
 */
object OpSplitString : ConstMediaAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val toSplit = args.getString(0, argc)
        val splitOn = args.getString(1, argc)
        return toSplit.split(splitOn).map { StringIota(it) }.asActionResult
    }
}

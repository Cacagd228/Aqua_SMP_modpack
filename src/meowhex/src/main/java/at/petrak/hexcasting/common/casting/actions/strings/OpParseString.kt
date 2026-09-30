package at.petrak.hexcasting.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getString
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.NullIota

/**
 * Read a string as a number. Unparseable text yields null rather than
 * mishapping, so a spell can branch on whether the parse worked.
 */
object OpParseString : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val s = args.getString(0, argc)
        return s.toDoubleOrNull()?.asActionResult ?: listOf(NullIota())
    }
}

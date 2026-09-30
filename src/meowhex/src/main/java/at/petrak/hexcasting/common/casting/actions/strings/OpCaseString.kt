package at.petrak.hexcasting.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBoolOrNull
import at.petrak.hexcasting.api.casting.getString
import at.petrak.hexcasting.api.casting.iota.Iota
import java.util.Locale

/**
 * Change the case of a string. A true forces upper, false forces lower, and
 * null toggles each letter independently -- which is how you invert a case
 * convention you did not write.
 */
object OpCaseString : ConstMediaAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val string = args.getString(0, argc)
        val upper = args.getBoolOrNull(1, argc)
        val out = when (upper) {
            true -> string.uppercase(Locale.ROOT)
            false -> string.lowercase(Locale.ROOT)
            // Kotlin's Char.isLowerCase is Unicode-aware, so this inverts the
            // convention rather than just flipping ASCII bits.
            null -> string.map {
                if (it.isLowerCase()) it.uppercaseChar() else it.lowercaseChar()
            }.joinToString("")
        }
        return out.asActionResult
    }
}

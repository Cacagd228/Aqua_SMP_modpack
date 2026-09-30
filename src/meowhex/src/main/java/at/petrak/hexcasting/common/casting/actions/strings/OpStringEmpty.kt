package at.petrak.hexcasting.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota

/**
 * The empty string. Note this is a string, not null: the two are different
 * values, and a spell can tell them apart.
 */
object OpStringEmpty : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> =
        "".asActionResult
}

/** A single space. */
object OpStringSpace : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> =
        " ".asActionResult
}

/** A comma, for use as a split separator. */
object OpStringComma : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> =
        ",".asActionResult
}

/** A newline, for writing multi-line text to a sign. */
object OpStringNewline : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> =
        "\n".asActionResult
}

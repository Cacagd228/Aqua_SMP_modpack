package at.petrak.hexcasting.common.casting.arithmetic.operator.string

import at.petrak.hexcasting.api.casting.arithmetic.operator.OperatorBasic
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaPredicate
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.StringIota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.common.casting.arithmetic.operator.nextString
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.DOUBLE
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.STRING
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Delete either the first occurrence of a substring, or the character at a
 * given index. The two spellings share a rune so a spell can pick either.
 */
object OperatorStringRemove : OperatorBasic(
    2,
    IotaMultiPredicate.pair(
        IotaPredicate.ofType(STRING), IotaPredicate.or(IotaPredicate.ofType(STRING), IotaPredicate.ofType(DOUBLE))
    )
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator().withIndex()
        val removeFrom = it.nextString(arity)
        val (idx, x) = it.next()

        if (x is StringIota) {
            val toRemove = x.string
            // Deleting the empty string is a no-op; short-circuit rather than
            // let the replace path decide what that means.
            if (toRemove.isEmpty()) return listOf(StringIota(removeFrom))
            return listOf(StringIota(removeFrom.replaceFirst(toRemove, "")))
        }

        val double = (x as DoubleIota).double
        val rounded = double.roundToInt()
        // Upstream accepts 0..length inclusive, which makes the final character
        // unremovable and throws outright on an empty string. A rune that can
        // crash a spell on a valid-looking input is worse than one that mishaps.
        if (abs(double - rounded) > DoubleIota.TOLERANCE || rounded !in 0 until removeFrom.length) {
            throw MishapInvalidIota.of(x, arity - 1 - idx, "int.positive.less.equal", removeFrom.length)
        }
        return listOf(StringIota(removeFrom.substring(0, rounded) + removeFrom.substring(rounded + 1)))
    }
}

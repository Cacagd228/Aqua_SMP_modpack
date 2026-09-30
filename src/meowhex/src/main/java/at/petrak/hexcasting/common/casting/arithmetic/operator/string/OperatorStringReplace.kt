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
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Overwrite either the first occurrence of a substring, or a span of characters
 * starting at a given index. An index-form write is clipped to the end of the
 * string, so writing past the end is allowed and simply truncates.
 */
object OperatorStringReplace : OperatorBasic(
    3,
    IotaMultiPredicate.triple(
        IotaPredicate.ofType(STRING),
        IotaPredicate.or(IotaPredicate.ofType(STRING), IotaPredicate.ofType(DOUBLE)),
        IotaPredicate.ofType(STRING)
    )
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator().withIndex()
        val replaceIn = it.nextString(arity)
        val (idx, x) = it.next()
        val replaceWith = it.nextString(arity)

        if (x is StringIota) {
            return listOf(StringIota(replaceIn.replaceFirst(x.string, replaceWith)))
        }

        val double = (x as DoubleIota).double
        val rounded = double.roundToInt()
        // Here the inclusive upper bound is deliberate: index == length means
        // "append", which is half the reason to reach for this spelling.
        if (abs(double - rounded) > DoubleIota.TOLERANCE || rounded !in 0..replaceIn.length) {
            throw MishapInvalidIota.of(x, arity - 1 - idx, "int.positive.less.equal", replaceIn.length)
        }
        val n = min(replaceIn.length - rounded, replaceWith.length)
        return listOf(
            StringIota(
                replaceIn.substring(0, rounded) +
                    replaceWith.substring(0, n) +
                    replaceIn.substring(rounded + n)
            )
        )
    }
}

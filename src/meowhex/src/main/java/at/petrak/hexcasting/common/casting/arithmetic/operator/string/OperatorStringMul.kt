package at.petrak.hexcasting.common.casting.arithmetic.operator.string

import at.petrak.hexcasting.api.casting.arithmetic.operator.OperatorBasic
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaPredicate
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.StringIota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.DOUBLE
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.STRING
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Repeat a string a whole number of times. Either operand order is accepted, so
 * a spell can push the count before or after the text.
 */
object OperatorStringMul : OperatorBasic(
    2,
    IotaMultiPredicate.either(
        IotaMultiPredicate.pair(IotaPredicate.ofType(STRING), IotaPredicate.ofType(DOUBLE)),
        IotaMultiPredicate.pair(IotaPredicate.ofType(DOUBLE), IotaPredicate.ofType(STRING))
    )
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator().withIndex()
        val (i0, first) = it.next()
        val (i1, second) = it.next()

        // Mishaps point at whichever iota is actually the number, so the player
        // is not left hunting for a bad count that is not where we said it is.
        val (str, num, numIdx) = if (first is DoubleIota) {
            Triple(second as StringIota, first, i0)
        } else {
            Triple(first as StringIota, second as DoubleIota, i1)
        }

        val double = (num as DoubleIota).double
        val rounded = double.roundToInt()
        if (abs(double - rounded) > DoubleIota.TOLERANCE || rounded < 0) {
            throw MishapInvalidIota.of(num, arity - 1 - numIdx, "int.positive.less.equal", Int.MAX_VALUE)
        }
        return listOf(StringIota((str as StringIota).string.repeat(rounded)))
    }
}

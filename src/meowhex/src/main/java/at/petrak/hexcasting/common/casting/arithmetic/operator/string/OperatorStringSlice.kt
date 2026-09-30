package at.petrak.hexcasting.common.casting.arithmetic.operator.string

import at.petrak.hexcasting.api.casting.arithmetic.operator.OperatorBasic
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaPredicate
import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.StringIota
import at.petrak.hexcasting.common.casting.arithmetic.operator.nextPositiveIntUnderInclusive
import at.petrak.hexcasting.common.casting.arithmetic.operator.nextString
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.DOUBLE
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.STRING
import kotlin.math.max
import kotlin.math.min

/**
 * Substring between two inclusive-of-endpoint bounds. Reversed bounds are
 * normalized, so `(slice "abcd" 3 1)` is the same as `(slice "abcd" 1 3)`.
 */
object OperatorStringSlice : OperatorBasic(
    3,
    IotaMultiPredicate.triple(
        IotaPredicate.ofType(STRING), IotaPredicate.ofType(DOUBLE), IotaPredicate.ofType(DOUBLE)
    )
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator().withIndex()
        val str = it.nextString(arity)
        val index0 = it.nextPositiveIntUnderInclusive(str.length, arity)
        val index1 = it.nextPositiveIntUnderInclusive(str.length, arity)

        if (index0 == index1) {
            return listOf(StringIota(""))
        }
        return listOf(StringIota(str.substring(min(index0, index1), max(index0, index1))))
    }
}

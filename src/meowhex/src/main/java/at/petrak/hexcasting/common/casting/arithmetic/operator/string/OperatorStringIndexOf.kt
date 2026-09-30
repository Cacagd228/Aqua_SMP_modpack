package at.petrak.hexcasting.common.casting.arithmetic.operator.string

import at.petrak.hexcasting.api.casting.arithmetic.operator.Operator.Companion.downcast
import at.petrak.hexcasting.api.casting.arithmetic.operator.OperatorBasic
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaPredicate
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.STRING

/**
 * Offset of the first occurrence of a substring, or -1 when absent. An empty
 * needle matches at 0, per `String.indexOf`.
 */
object OperatorStringIndexOf : OperatorBasic(
    2, IotaMultiPredicate.pair(IotaPredicate.ofType(STRING), IotaPredicate.ofType(STRING))
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator()
        val toSearch = downcast(it.next(), STRING).string
        val searchFor = downcast(it.next(), STRING).string
        return listOf(DoubleIota(toSearch.indexOf(searchFor).toDouble()))
    }
}

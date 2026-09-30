package at.petrak.hexcasting.common.casting.arithmetic.operator.string

import at.petrak.hexcasting.api.casting.arithmetic.operator.Operator.Companion.downcast
import at.petrak.hexcasting.api.casting.arithmetic.operator.OperatorBasic
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaPredicate
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.iota.StringIota
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.DOUBLE
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.STRING
import kotlin.math.roundToInt

/**
 * Index into a string, yielding a one-character string. An out-of-bounds read
 * yields null rather than mishapping, matching how `OperatorIndex` treats a list.
 */
object OperatorStringIndex : OperatorBasic(
    2, IotaMultiPredicate.pair(IotaPredicate.ofType(STRING), IotaPredicate.ofType(DOUBLE))
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator()
        val str = downcast(it.next(), STRING).string
        val index = downcast(it.next(), DOUBLE).double
        val ch = str.getOrNull(index.roundToInt()) ?: return listOf(NullIota())
        return listOf(StringIota(ch.toString()))
    }
}

package at.petrak.hexcasting.common.casting.arithmetic

import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.*
import at.petrak.hexcasting.api.casting.arithmetic.engine.InvalidOperatorException
import at.petrak.hexcasting.api.casting.arithmetic.operator.Operator
import at.petrak.hexcasting.api.casting.arithmetic.operator.Operator.Companion.downcast
import at.petrak.hexcasting.api.casting.arithmetic.operator.OperatorBinary
import at.petrak.hexcasting.api.casting.arithmetic.operator.OperatorUnary
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate.all
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate.pair
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaPredicate
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.StringIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.casting.arithmetic.operator.string.OperatorStringIndex
import at.petrak.hexcasting.common.casting.arithmetic.operator.string.OperatorStringIndexOf
import at.petrak.hexcasting.common.casting.arithmetic.operator.string.OperatorStringMul
import at.petrak.hexcasting.common.casting.arithmetic.operator.string.OperatorStringRemove
import at.petrak.hexcasting.common.casting.arithmetic.operator.string.OperatorStringReplace
import at.petrak.hexcasting.common.casting.arithmetic.operator.string.OperatorStringSlice
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.STRING
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.UnaryOperator

/**
 * Teaches the stock arithmetic runes about strings, so a spell gets the same
 * vocabulary lists already have. Nothing here adds a pattern: `add` on two
 * strings concatenates, `len` on a string counts characters, and so on.
 * Ported from the MoreIotas addon (MIT, Talia-12).
 */
object StringArithmetic : Arithmetic {
    private val OPS = listOf(
        ADD,
        MUL,
        ABS,
        INDEX,
        SLICE,
        REV,
        INDEX_OF,
        REMOVE,
        REPLACE,
        UNIQUE
    )

    override fun arithName() = "string_ops"

    override fun opTypes(): Iterable<HexPattern> = OPS

    override fun getOperator(pattern: HexPattern): Operator {
        return when (pattern) {
            ADD -> make2 { s0, s1 -> s0 + s1 }
            MUL -> OperatorStringMul
            ABS -> make1ToDouble { it.length.toDouble() }
            INDEX -> OperatorStringIndex
            SLICE -> OperatorStringSlice
            REV -> make1 { it.reversed() }
            INDEX_OF -> OperatorStringIndexOf
            REMOVE -> OperatorStringRemove
            REPLACE -> OperatorStringReplace
            UNIQUE -> make1 { s -> s.toList().distinct().joinToString("") }
            else -> throw InvalidOperatorException("$pattern is not a valid operator in Arithmetic $this.")
        }
    }

    private fun make1(op: UnaryOperator<String>): OperatorUnary =
        OperatorUnary(all(IotaPredicate.ofType(STRING))) { iota: Iota ->
            StringIota(op.apply(downcast(iota, STRING).string))
        }

    private fun make1ToDouble(op: Function<String, Double>): OperatorUnary =
        OperatorUnary(all(IotaPredicate.ofType(STRING))) { iota: Iota ->
            DoubleIota(op.apply(downcast(iota, STRING).string))
        }

    private fun make2(op: BinaryOperator<String>): OperatorBinary =
        OperatorBinary(pair(IotaPredicate.ofType(STRING), IotaPredicate.ofType(STRING))) { i: Iota, j: Iota ->
            StringIota(op.apply(downcast(i, STRING).string, downcast(j, STRING).string))
        }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.smt

import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.expr.BinaryExpr.Operator.*
import com.github.javaparser.ast.expr.UnaryExpr.Operator.*
import com.github.javaparser.resolution.types.ResolvedArrayType
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedPrimitiveType.*
import com.github.javaparser.resolution.types.ResolvedType
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.model.SmtType
import java.math.BigInteger

/**
 * @author Alexander Weigl
 * @version 1 (01.07.22)
 */
open class BitVectorArithmeticTranslator(val smtLog: SmtQuery) : ArithmeticTranslator {
    var cnt: Int = 0

    override fun binary(operator: BinaryExpr.Operator, left: SExpr, right: SExpr): SExpr {
        // Java binary numeric promotion (JLS 5.6.2) / unary promotion for shifts
        // (JLS 5.6.1): byte/short/char operands are widened to int (long wins)
        // *before* the operation, so `bv8 + bv32` and `bvadd` on BV8 — which would
        // wrap where Java cannot — are impossible. Equality/boolean/array operands
        // are not bit-vectors and pass through unchanged.
        val (l, r) = promote(operator, left, right)
        return when (operator) {
            IMPLICATION -> term.impl(l, r)
            SUBTYPE -> error("")
            RANGE -> error("")
            SUB_LOCK -> error("")
            SUB_LOCKE -> error("")
            RIMPLICATION -> term.impl(r, l)
            EQUIVALENCE -> term.equiv(l, r)
            ANTIVALENCE -> term.not(term.equiv(l, r))
            OR -> term.or(l, r)
            AND -> term.and(l, r)
            BINARY_OR -> term.bor(l, r)
            BINARY_AND -> term.band(l, r)
            XOR -> term.xor(l, r)
            EQUALS -> term.equality(l, r)
            NOT_EQUALS -> term.not(term.equality(l, r))
            LESS -> term.lessThan(l, r)
            GREATER -> term.greaterThan(l, r)
            LESS_EQUALS -> term.lessOrEquals(l, r, true)
            GREATER_EQUALS -> term.greaterOrEquals(l, r, true)
            LEFT_SHIFT -> term.shiftLeft(l, r)
            SIGNED_RIGHT_SHIFT -> term.shiftRight(l, r, true)
            UNSIGNED_RIGHT_SHIFT -> term.shiftRight(l, r, true)
            BinaryExpr.Operator.PLUS -> term.add(l, r)
            BinaryExpr.Operator.MINUS -> term.subtract(l, r)
            MULTIPLY -> term.multiply(l, r)
            DIVIDE -> term.divide(l, r, true)
            REMAINDER -> term.modulo(l, r, true)
        }
    }

    override fun promote(operator: BinaryExpr.Operator, left: SExpr, right: SExpr): Pair<SExpr, SExpr> {
        val lw = (left.smtType as? SmtType.BitVec)?.width ?: return left to right
        val rw = (right.smtType as? SmtType.BitVec)?.width ?: return left to right
        // Shifts (JLS 15.19): only the *left* operand is unary-promoted (int or
        // long); the distance is widened to the same width so the bit-vector shift
        // stays well-sorted (Z3's bvshl/bvashr/bvshr require equal sorts). This is
        // exactly SMT-LIB's masking of the shift amount.
        if (operator in SHIFT_OPS) {
            val w = if (lw >= 64) 64 else 32
            val promoted = widenTo(left, w)
            return promoted to widenTo(right, w)
        }
        // binary numeric promotion: long wins over int/narrow; otherwise int.
        val w = if (lw >= 64 || rw >= 64) 64 else 32
        return widenTo(left, w) to widenTo(right, w)
    }

    override fun makeChar(n: CharLiteralExpr): SExpr {
        // `asChar()` evaluates escapes properly — `n.value[0]` would yield the
        // backslash of a '\uFFFF' literal.
        val v = term.makeBitvector(16, n.asChar().code.toLong())
        // carry the char-ness: `char` values are unsigned, so their widening to
        // int must zero-extend (`\uFFFF` is 65535, not -1).
        v.javaType = ResolvedPrimitiveType.CHAR
        return v
    }

    override fun unary(operator: UnaryExpr.Operator, accept: SExpr): SExpr {
        val operand = when (operator) {
            // JLS 15.15.1/15.15.2/15.15.4: unary plus/minus and the pre-increment/
            // decrement in expression position promote byte/short/char to int.
            UnaryExpr.Operator.MINUS, PREFIX_INCREMENT, PREFIX_DECREMENT -> widenTo(accept, 32)
            else -> accept
        }
        return when (operator) {
            UnaryExpr.Operator.PLUS, POSTFIX_INCREMENT, POSTFIX_DECREMENT -> operand
            UnaryExpr.Operator.MINUS -> term.negate(operand)
            PREFIX_INCREMENT -> term.add(operand, term.makeBitvector(widthOf(operand, 32), 1))
            PREFIX_DECREMENT -> term.subtract(operand, term.makeBitvector(widthOf(operand, 32), 1))
            LOGICAL_COMPLEMENT -> term.not(operand)
            BITWISE_COMPLEMENT -> term.not(operand)
        }
    }

    /** Widens [x] to [w] bits (no-op when already wide enough): `char` zero-extends
     *  because it is unsigned; byte/short and everything else sign-extend
     *  (JLS 5.1.2 / 5.6.2). */
    private fun widenTo(x: SExpr, w: Int): SExpr {
        val width = (x.smtType as? SmtType.BitVec)?.width ?: return x
        if (width >= w) return x
        val isChar = (x.javaType as? ResolvedPrimitiveType) === ResolvedPrimitiveType.CHAR
        return if (isChar) term.zeroExtend(x, w) else term.signExtend(x, w)
    }

    private fun widthOf(x: SExpr, fallback: Int): Int =
        (x.smtType as? SmtType.BitVec)?.width ?: fallback

    override fun makeLong(n: LongLiteralExpr) = term.makeBitvector(64, n.value.toBigInteger())

    override fun makeInt(n: IntegerLiteralExpr) = term.makeBitvector(32, n.asNumber().toLong())

    override fun makeInt(i: BigInteger) = term.makeBitvector(32, i)

    override fun makeIntVar(): SExpr {
        val name = "__RAND_" + (++cnt)
        smtLog.declareConst(name, SmtType.BV32)
        return term.symbol(name)
    }

    override fun getVariable(jmlBoundVariable: VariableDeclarator): SExpr {
        val rType = jmlBoundVariable.type.resolve()
        return term.list(null, SmtType.BOOL, jmlBoundVariable.nameAsString, term.type(getType(rType)))
    }

    override fun makeBoolean(value: Boolean) = term.makeBoolean(value)

    override fun getType(asPrimitive: ResolvedType): SmtType {
        if (asPrimitive.isPrimitive) {
            val rType: ResolvedPrimitiveType = asPrimitive.asPrimitive()
            return getPrimitiveType(rType)
        }

        if (asPrimitive.isArray) {
            val aType: ResolvedArrayType = asPrimitive.asArrayType()
            val cType: SmtType = getType(aType.componentType)
            val intType: SmtType = getType(INT)
            return SmtType.Array(intType, cType)
        }

        if (asPrimitive.isReferenceType) {
            return SmtType.JAVA_OBJECT
        }

        throw RuntimeException("Unsupported type")
    }

    override fun arrayLength(obj: SExpr): SExpr = term.list(INT, SmtType.BV32, term.symbol("bv\$length"), obj)

    override fun makeInt(i: Long): SExpr = makeInt(BigInteger.valueOf(i))

    override fun makeVar(rtype: ResolvedType): SExpr {
        TODO("Not yet implemented")
    }

    open fun getPrimitiveType(rType: ResolvedPrimitiveType) =
        when (rType) {
            BOOLEAN -> SmtType.BOOL
            BYTE -> SmtType.BV8
            SHORT -> SmtType.BV16
            CHAR -> SmtType.BV16
            INT -> SmtType.BV32
            LONG -> SmtType.BV64
            FLOAT -> SmtType.FP32
            DOUBLE -> SmtType.FP64
        }

    companion object {
        private val term: SmtTermFactory = SmtTermFactory

        /** Shift operators use the *left* operand's promoted width only (JLS 15.19). */
        private val SHIFT_OPS: Set<BinaryExpr.Operator> =
            setOf(LEFT_SHIFT, SIGNED_RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT)
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.jml.expr.JmlLabelExpr

/**
 * Abstract values of the boolean lattice used for the simple abstract interpretation.
 */
enum class BoolAbsValue {
    VALID,
    INVALID,
    UNKNOWN;

    fun negate(): BoolAbsValue = when (this) {
        VALID -> INVALID
        INVALID -> VALID
        UNKNOWN -> UNKNOWN
    }

    companion object {
        fun of(b: Boolean): BoolAbsValue = if (b) VALID else INVALID
    }
}

/**
 * A simple abstract interpreter for boolean JML expressions.
 *
 * Everything that cannot be decided soundly evaluates to [BoolAbsValue.UNKNOWN].
 */
class FormulaEvaluator {
    tailrec fun eval(expr: Expression): BoolAbsValue = when (expr) {
        is BooleanLiteralExpr -> BoolAbsValue.of(expr.value)

        is EnclosedExpr -> eval(expr.inner)

        is CastExpr -> {
            // a cast to a boolean-ish type does not change the truth value
            val t = expr.type.asString()
            if (t.equals("boolean", ignoreCase = true) || t == "java.lang.Boolean") {
                eval(expr.expression)
            } else {
                BoolAbsValue.UNKNOWN
            }
        }

        is JmlLabelExpr -> eval(expr.expression)

        is UnaryExpr -> evalUnary(expr)

        is BinaryExpr -> evalBinary(expr)

        is ConditionalExpr -> evalConditional(expr)

        // `null instanceof T` is always false
        is InstanceOfExpr ->
            if (expr.expression.isNullLiteralExpr) BoolAbsValue.INVALID else BoolAbsValue.UNKNOWN

        else -> BoolAbsValue.UNKNOWN
    }

    private fun evalUnary(expr: UnaryExpr): BoolAbsValue =
        when (expr.operator) {
            UnaryExpr.Operator.LOGICAL_COMPLEMENT -> eval(expr.expression).negate()
            else -> BoolAbsValue.UNKNOWN
        }

    private fun evalConditional(expr: ConditionalExpr): BoolAbsValue {
        val c = eval(expr.condition)
        val t = eval(expr.thenExpr)
        val e = eval(expr.elseExpr)

        return when (c) {
            BoolAbsValue.VALID -> t
            BoolAbsValue.INVALID -> e
            BoolAbsValue.UNKNOWN -> {
                if (t == e) t else BoolAbsValue.UNKNOWN
            }
        }
    }

    private fun evalBinary(expr: BinaryExpr): BoolAbsValue {
        val (left, right) = expr
        return when (expr.operator) {
            BinaryExpr.Operator.AND -> {
                val l = eval(left)
                val r = eval(right)
                if (l == BoolAbsValue.INVALID || r == BoolAbsValue.INVALID) {
                    BoolAbsValue.INVALID
                } else if (l == BoolAbsValue.VALID && r == BoolAbsValue.VALID) {
                    BoolAbsValue.VALID
                } // x && !x  is always false
                else if (isComplement(left, right) || isComplement(right, left)) {
                    BoolAbsValue.INVALID
                } else {
                    BoolAbsValue.UNKNOWN
                }
            }

            BinaryExpr.Operator.OR -> {
                val l = eval(left)
                val r = eval(right)
                if (l == BoolAbsValue.VALID || r == BoolAbsValue.VALID) {
                    BoolAbsValue.VALID
                } else if (l == BoolAbsValue.INVALID && r == BoolAbsValue.INVALID) {
                    BoolAbsValue.INVALID
                } // x || !x  is always true
                else if (isComplement(left, right) || isComplement(right, left)) {
                    BoolAbsValue.VALID
                } else {
                    BoolAbsValue.UNKNOWN
                }
            }

            BinaryExpr.Operator.XOR -> {
                val l = eval(left)
                val r = eval(right)
                if (l == BoolAbsValue.UNKNOWN || r == BoolAbsValue.UNKNOWN) {
                    BoolAbsValue.UNKNOWN
                } else {
                    BoolAbsValue.of(l != r)
                }
            }

            BinaryExpr.Operator.EQUALS, BinaryExpr.Operator.NOT_EQUALS -> {
                val cmp = compareLiterals(left, right)
                    ?: return if (expr.operator == BinaryExpr.Operator.EQUALS &&
                        isSyntacticallyEqual(left, right)
                    ) {
                        BoolAbsValue.VALID
                    } else {
                        BoolAbsValue.UNKNOWN
                    }
                BoolAbsValue.of(
                    if (expr.operator == BinaryExpr.Operator.EQUALS) cmp == 0 else cmp != 0
                )
            }

            BinaryExpr.Operator.LESS, BinaryExpr.Operator.GREATER,
            BinaryExpr.Operator.LESS_EQUALS, BinaryExpr.Operator.GREATER_EQUALS -> {
                val cmp = compareLiterals(left, right) ?: return BoolAbsValue.UNKNOWN
                val result = when (expr.operator) {
                    BinaryExpr.Operator.LESS -> cmp < 0
                    BinaryExpr.Operator.GREATER -> cmp > 0
                    BinaryExpr.Operator.LESS_EQUALS -> cmp <= 0
                    else -> cmp >= 0
                }
                BoolAbsValue.of(result)
            }

            else -> BoolAbsValue.UNKNOWN
        }
    }

    /** Returns true iff `b` is the syntactic negation `!(a)`. */
    private fun isComplement(a: Expression, b: Expression): Boolean =
        b is UnaryExpr &&
            b.operator == UnaryExpr.Operator.LOGICAL_COMPLEMENT &&
            isSyntacticallyEqual(a, b.expression)

    /** Syntactic equality of names, used for `x && !x` / `x || !x`. */
    private fun isSyntacticallyEqual(a: Expression, b: Expression): Boolean =
        a is NameExpr && b is NameExpr &&
            a.nameAsString == b.nameAsString

    /**
     * Compares two primitive literals. Returns a negative/zero/positive integer,
     * or `null` if either side is not a comparable literal.
     */
    private fun compareLiterals(a: Expression, b: Expression): Int? {
        val left = literalValue(a) ?: return null
        val right = literalValue(b) ?: return null
        return if (left is String && right is String) {
            left.compareTo(right)
        } else if (left !is String && right !is String) {
            java.lang.Double.compare((left as Number).toDouble(), (right as Number).toDouble())
        } else {
            null
        }
    }

    private fun literalValue(expr: Expression): Any? = when (expr) {
        is IntegerLiteralExpr -> expr.asInt()
        is LongLiteralExpr -> expr.asLong()
        is DoubleLiteralExpr -> expr.asDouble()
        is CharLiteralExpr -> expr.asChar().code
        is StringLiteralExpr -> expr.asString()
        else -> null
    }
}

operator fun BinaryExpr.component1() = left()
operator fun BinaryExpr.component2() = right()
operator fun BinaryExpr.component3() = operator()

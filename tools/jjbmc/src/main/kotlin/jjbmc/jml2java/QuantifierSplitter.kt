/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.jml2java

import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.jml.expr.JmlMultiCompareExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr

/**
 * Supports two formats:
 *
 *
 *  * Bounded quantifier with three expressions:
 * `(\forall int i; lower; upper; val)`*
 *
 *  * Or range format with two expressions:
 * `(\forall int i; lower <= i && i < upper; val)`*
 *
 *  * Or range format with two expressions:
 * `(\forall int i; lower <= i < upper; val)`*
 *
 *
 */
object QuantifierSplitter {
    fun getVariable(n: JmlQuantifiedExpr): VariableDeclarator {
        check(n.variables.size == 1) { "Unsupported number of variables." }
        return n.variables.first()
    }

    private fun getUpperBound(e: BinaryExpr, variable: NameExpr?): Expression? {
        if (e.operator.equals(BinaryExpr.Operator.AND)) {
            var leftCandidate: Expression? = null
            if (e.left is BinaryExpr) {
                leftCandidate = QuantifierSplitter.getUpperBound(e.left, variable)
            }
            var rightCandidate: Expression? = null
            if (e.right is BinaryExpr) {
                rightCandidate = QuantifierSplitter.getUpperBound(e.right, variable)
            }
            if (rightCandidate == null && leftCandidate == null) {
                return null
            }
            check(!(rightCandidate != null && leftCandidate != null)) { "Ubiquitous lower bound found in: " + e }
            if (rightCandidate != null) {
                return rightCandidate
            }
            return leftCandidate
        }

        if (e.operator.equals(BinaryExpr.Operator.LESS) && e.left.equals(variable)) {
            return e.right
        }

        if (e.operator.equals(BinaryExpr.Operator.LESS_EQUALS) &&
            e.left.equals(variable)
        ) {
            return BinaryExpr(e.right, IntegerLiteralExpr("1"), BinaryExpr.Operator.PLUS)
        }

        if (e.operator.equals(BinaryExpr.Operator.GREATER) && e.right.equals(variable)) {
            return BinaryExpr(e.left, IntegerLiteralExpr("1"), BinaryExpr.Operator.PLUS)
        }

        if (e.operator.equals(BinaryExpr.Operator.GREATER_EQUALS) &&
            e.right.equals(variable)
        ) {
            return e.left
        }

        return null
    }

    private fun getUpperBound(expr: JmlMultiCompareExpr, variable: NameExpr?): Expression? {
        check(
            !(expr.expressions.size !== 3 || expr.operators.size !== 2)
        ) { "Unable to find lower bound in: " + expr }
        val firstCandidate: Expression? = getUpperBound(
            BinaryExpr(
                expr.expressions[0],
                expr.expressions[1],
                expr.operators[0]
            ),
            variable
        )
        val secondCandidate: Expression? = getUpperBound(
            BinaryExpr(
                expr.expressions[1],
                expr.expressions[2],
                expr.operators[1]
            ),
            variable
        )
        if (firstCandidate == null && secondCandidate == null) {
            return null
        }
        check(!(firstCandidate != null && secondCandidate != null)) { "Ubiquitous lower bound found in: " + expr }
        if (firstCandidate != null) {
            return firstCandidate
        }
        return secondCandidate
    }

    private fun getLowerBound(e: BinaryExpr, variable: NameExpr?): Expression? {
        if (e.operator.equals(BinaryExpr.Operator.AND)) {
            var leftCandidate: Expression? = null
            if (e.left is BinaryExpr) {
                leftCandidate = QuantifierSplitter.getLowerBound(e.left, variable)
            }
            var rightCandidate: Expression? = null
            if (e.right is BinaryExpr) {
                rightCandidate = QuantifierSplitter.getLowerBound(e.right, variable)
            }
            if (rightCandidate == null && leftCandidate == null) {
                return null
            }
            check(!(rightCandidate != null && leftCandidate != null)) { "Ubiquitous lower bound found in: " + e }
            if (rightCandidate != null) {
                return rightCandidate
            }
            return leftCandidate
        }

        if (e.operator.equals(BinaryExpr.Operator.LESS) && e.right.equals(variable)) {
            return BinaryExpr(e.left, IntegerLiteralExpr("1"), BinaryExpr.Operator.PLUS)
        }
        if (e.operator.equals(BinaryExpr.Operator.LESS_EQUALS) &&
            e.right.equals(variable)
        ) {
            return e.left
        }
        if (e.operator.equals(BinaryExpr.Operator.GREATER) && e.left.equals(variable)) {
            return BinaryExpr(e.right, IntegerLiteralExpr("1"), BinaryExpr.Operator.PLUS)
        }
        if (e.operator.equals(BinaryExpr.Operator.GREATER_EQUALS) &&
            e.left.equals(variable)
        ) {
            return e.right
        }
        return null
    }

    private fun getLowerBound(expr: JmlMultiCompareExpr, variable: NameExpr?): Expression? {
        check(
            !(expr.expressions.size !== 3 || expr.operators.size !== 2)
        ) { "Unable to find lower bound in: " + expr }
        val firstCandidate: Expression? = getLowerBound(
            BinaryExpr(
                expr.expressions[0],
                expr.expressions[1],
                expr.operators[0]
            ),
            variable
        )
        val secondCandidate: Expression? = getLowerBound(
            BinaryExpr(
                expr.expressions[1],
                expr.expressions[2],
                expr.operators[1]
            ),
            variable
        )
        if (firstCandidate == null && secondCandidate == null) {
            return null
        }
        check(!(firstCandidate != null && secondCandidate != null)) { "Ubiquitous lower bound found in: " + expr }
        if (firstCandidate != null) {
            return firstCandidate
        }
        return secondCandidate
    }

    fun getLowerBound(n: JmlQuantifiedExpr): Expression? {
        if (n.expressions.size == 3) { // bounded format
            return n.expressions[0]
        }
        val variable: NameExpr? = getVariable(n).nameAsExpression

        if (n.expressions[0] is BinaryExpr) {
            val res: Expression? = QuantifierSplitter.getLowerBound(n.expressions[0], variable)
            if (res != null) {
                return res
            }
        }
        if (n.expressions[0] is JmlMultiCompareExpr) {
            val res: Expression? = QuantifierSplitter.getLowerBound(n.expressions[0], variable)
            if (res != null) {
                return res
            }
        }
        throw IllegalStateException("Mis-formed binder guard in: " + n)
    }

    fun getUpperBound(n: JmlQuantifiedExpr): Expression? {
        if (n.expressions.size == 3) { // bounded format
            return n.expressions[0]
        }
        val variable: NameExpr? = getVariable(n).nameAsExpression

        val e = n.expressions[0]
        if (e is BinaryExpr) {
            val res: Expression? = getUpperBound(e, variable)
            if (res != null) {
                return res
            }
        }
        if (e is JmlMultiCompareExpr) {
            val res: Expression? = getUpperBound(e, variable)
            if (res != null) {
                return res
            }
        }
        throw IllegalStateException("Mis-formed binder guard in: " + n)
    }
}

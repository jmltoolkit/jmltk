/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.jml2java

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr
import com.github.javaparser.ast.visitor.ModifierVisitor
import com.github.javaparser.ast.visitor.Visitable
import jjbmc.UnsupportedException

class NormalizeBinaryExpressions : ModifierVisitor<Any>() {
    private fun swapOperator(expr: BinaryExpr, newOp: BinaryExpr.Operator): Expression {
        val left: Expression = expr.left.accept(this, Any()) as Expression
        val right: Expression = expr.right.accept(this, Any()) as Expression
        val newExpr: Expression = BinaryExpr(left, right, newOp)
        newExpr.setParentNode(expr.parentNode.get())
        left.setParentNode(newExpr)
        right.setParentNode(newExpr)
        return newExpr
    }

    private fun addNot(expr: Expression): Expression {
        val oldParentNode: Node = expr.parentNode.get()
        val inner: Expression? = expr.accept(this, Any()) as Expression?
        val newExpr: Expression = UnaryExpr(EnclosedExpr(inner), UnaryExpr.Operator.LOGICAL_COMPLEMENT)
        newExpr.setParentNode(oldParentNode)
        return newExpr
    }

    private fun getDualQuantifier(quantifier: JmlQuantifiedExpr.JmlBinder): JmlQuantifiedExpr.JmlDefaultBinder {
        if (quantifier == JmlQuantifiedExpr.JmlDefaultBinder.FORALL) {
            return JmlQuantifiedExpr.JmlDefaultBinder.EXISTS
        }
        if (quantifier == JmlQuantifiedExpr.JmlDefaultBinder.EXISTS) {
            return JmlQuantifiedExpr.JmlDefaultBinder.FORALL
        }
        throw UnsupportedException("Quantifier $quantifier not supported.")
    }

    override fun visit(n: BinaryExpr, arg: Any): Visitable {
        if (n.operator.equals(BinaryExpr.Operator.ANTIVALENCE)) {
            return swapOperator(n, BinaryExpr.Operator.NOT_EQUALS)
        }
        return super.visit(n, arg)
    }

    override fun visit(n: UnaryExpr, arg: Any): Visitable {
        if (n.operator.equals(UnaryExpr.Operator.LOGICAL_COMPLEMENT)) {
            if (n.childNodes[0] is JmlQuantifiedExpr) {
                val quantifiedExpression: JmlQuantifiedExpr =
                    n.childNodes[0] as JmlQuantifiedExpr
                var inner = quantifiedExpression.expressions.last()
                inner = addNot(inner)
                val expressions: NodeList<Expression?> = NodeList()
                expressions.addAll(
                    quantifiedExpression
                        .expressions
                        .subList(0, quantifiedExpression.expressions.size - 1)
                )
                expressions.add(inner)
                val newQuantifiedExpr = JmlQuantifiedExpr(
                    quantifiedExpression.tokenRange.get(),
                    getDualQuantifier(quantifiedExpression.binder),
                    quantifiedExpression.variables,
                    expressions
                )
                newQuantifiedExpr.setParentNode(n.parentNode.get())
                return super.visit(newQuantifiedExpr, arg)
            }
        }
        return super.visit(n, arg)
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules.locset

import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.ThisExpr
import com.github.javaparser.ast.jml.expr.JmlLabelExpr
import com.github.javaparser.ast.jml.expr.JmlSetComprehensionExpr

/**
 * The abstract interpreter for locset (store-ref) expressions.
 *
 * Transfer functions map each syntactic form of a store-ref expression to an abstract
 * value in [AbsLoc]. The interpretation is sound and terminating: every transfer
 * function is monotone and the domain is finite, so no widening is required.
 *
 * An environment maps the names of ghost/spec variables of locset type to their
 * abstract values; unknown names are abstracted as [AbsLoc.SINGLE_LOCATION], because
 * a simple store-ref name denotes exactly one location.
 *
 * @param env abstract bindings for locset-valued variables in scope.
 * @author Alexander Weigl
 * @version 1 (21.09.26)
 */
class LocsetEvaluator(private val env: Map<String, AbsLoc> = emptyMap()) {

    /**
     * The abstract value of a frame clause expression list
     * (`assignable e1, ..., en` is the union `e1 ∪ ... ∪ en`).
     * An empty list means the clause is absent, i.e. `\everything` (JML default).
     */
    fun frame(exprs: List<Expression>): AbsLoc {
        if (exprs.isEmpty()) return AbsLoc.EVERYTHING
        return exprs.map { eval(it) }.reduce { a, b -> a union b }
    }

    /** The transfer function for a single store-ref expression. */
    fun eval(expr: Expression): AbsLoc = when (expr) {
        // locset literals
        is NameExpr -> evalName(expr)

        // `a[i]`: one array cell; `a[*]` (wildcard index) or `a[0..n]` (range):
        // all elements of a (possibly empty) array
        is ArrayAccessExpr -> when {
            expr.index.toString() == "*" -> AbsLoc.ALL_ARRAY_ELEMENTS
            isRangeIndex(expr.index) -> AbsLoc.ALL_ARRAY_ELEMENTS
            else -> AbsLoc.SINGLE_LOCATION
        }

        // `o.f`: one field; `o.*`: all fields of a (possibly null) object
        is FieldAccessExpr -> when (expr.nameAsString) {
            "*" -> AbsLoc.ALL_OBJECT_FIELDS
            else -> AbsLoc.SINGLE_LOCATION
        }

        is ThisExpr -> AbsLoc.SINGLE_LOCATION

        // control-flow merge: `cond ? e1 : e2` may evaluate either branch
        is ConditionalExpr -> eval(expr.thenExpr) union eval(expr.elseExpr)

        is EnclosedExpr -> eval(expr.inner)

        // JML set comprehension `new elems [; such that ...]`: a finite subset
        is JmlSetComprehensionExpr -> AbsLoc.ALL_ARRAY_ELEMENTS

        // labeled JML expression, e.g. `\old(e)`, `\pre(e)`: interpret the inner
        is JmlLabelExpr -> eval(expr.expression)

        // set operations written as binary operators, if the grammar allows them
        is BinaryExpr -> evalBinary(expr)

        // anything else (method calls, \result arithmetic, ...) is unknown
        else -> AbsLoc.TOP
    }

    private fun evalName(expr: NameExpr): AbsLoc = when (expr.nameAsString) {
        "\\nothing", "\\strictly_nothing" -> AbsLoc.NOTHING
        "\\everything" -> AbsLoc.EVERYTHING
        "this" -> AbsLoc.SINGLE_LOCATION
        else -> env[expr.nameAsString] ?: AbsLoc.SINGLE_LOCATION
    }

    private fun evalBinary(expr: BinaryExpr): AbsLoc = when (expr.operator) {
        // set union / intersection / difference, if used on locsets
        BinaryExpr.Operator.PLUS, BinaryExpr.Operator.OR ->
            eval(expr.left) union eval(expr.right)

        BinaryExpr.Operator.MULTIPLY, BinaryExpr.Operator.AND ->
            eval(expr.left) intersect eval(expr.right)

        BinaryExpr.Operator.MINUS ->
            eval(expr.left) minus eval(expr.right)

        else -> AbsLoc.TOP
    }

    /** Recognizes JML range indexes such as `0..`, `0..n`, `..n`. */
    private fun isRangeIndex(index: Expression): Boolean {
        val s = index.toString()
        return s.startsWith("..") || s.endsWith("..") || s.contains("..")
    }
}

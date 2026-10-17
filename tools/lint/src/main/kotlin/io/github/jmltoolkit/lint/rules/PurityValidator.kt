/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jmlparser.lint.rules

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.jml.body.JmlClassExprDeclaration
import com.github.javaparser.ast.jml.clauses.JmlSimpleExprClause
import com.github.javaparser.ast.jml.stmt.JmlExpressionStmt
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRuleVisitor
import kotlin.jvm.optionals.getOrNull

/**
 * ## Rule: JML expressions must be pure
 *
 * Specification expressions in JML (JML Reference Manual, chapter on purity)
 * are mathematical predicates: they are evaluated by tools in arbitrary
 * contexts, at arbitrary program points, and possibly multiple times. They
 * must therefore be free of side effects. An expression with a side effect
 * makes the specification unexecutable and its meaning undefined; every JML
 * tool rejects it.
 *
 * ### Checked cases
 *
 * The rule walks the expression of every simple expression clause (e.g.
 * `requires`, `ensures`, `loop_invariant`), every class invariant, and every
 * JML expression statement (e.g. `assert`, `assume`, `set`), and reports:
 *
 *  - **Assignments** &mdash; an assignment (`=`, `+=`, ...) always modifies
 *    state and is never pure.
 *
 *  - **Increment/decrement operators** &mdash; `++` and `--` (prefix and
 *    postfix) modify the operand; they are never pure.
 *
 *  - **Method calls** &mdash; a call is only permitted if the callee is
 *    declared with the JML modifiers `pure` or `strictly_pure`. A call to any
 *    other method *might* modify state, so it is reported even if the called
 *    method happens to be side-effect free in its implementation &mdash; only
 *    the `pure` declaration is a guarantee for clients of the specification.
 *
 * ### Known limitations
 *
 *  - Only the **first** violation within an expression is reported; the
 *    traversal stops at the offending subexpression.
 *  - Assignments are reported without a specific hint; only the general
 *    message is given.
 *  - A method call that cannot be resolved aborts the check of the enclosing
 *    expression with an internal error.
 *  - Purity is not checked for other constructs, e.g. object creation, field
 *    accesses on the left-hand side of store-ref expressions, or
 *    constructor calls.
 *
 * ### Examples
 *
 * Good &mdash; pure expression, call of a `pure` method:
 * ```java
 * //@ requires n >= 0;
 * //@ ensures \result == Math.abs(n);
 * /*@ pure */
 * public int abs(int n) { ... }
 * ```
 *
 * Bad &mdash; assignment inside a clause:
 * ```java
 * //@ ensures (count = count + 1) > 0;  // error: assignments are not pure
 * public int next() { ... }
 * ```
 *
 * Bad &mdash; increment inside an assertion:
 * ```java
 * //@ assert i++ < n;  // error: increment operators are not pure
 * ```
 *
 * Bad &mdash; call of a method without a `pure` declaration:
 * ```java
 * //@ ensures list.size() > 0;  // error: 'size' might not be pure
 * public int first(List list) { ... }
 * ```
 *
 * @author Alexander Weigl
 * @version 1 (12/29/21)
 */
class PurityValidator : LintRuleVisitor() {
    override val visitor: VoidVisitorAdapter<LintProblemReporter>
        get() = object : VoidVisitorAdapter<LintProblemReporter>() {
            override fun visit(n: JmlSimpleExprClause, arg: LintProblemReporter) {
                val r = PurityVisitor()
                n.expression.accept(r, null)
                if (r.reason != null) {
                    arg.error(r.reason!!, "", "", "Expression in JML clause must be pure." + r.text)
                }
            }

            override fun visit(n: JmlClassExprDeclaration, arg: LintProblemReporter) {
                val r = PurityVisitor()
                n.invariant.accept(r, null)
                if (r.reason != null) {
                    arg.error(r.reason!!, "", "", "Expression in JML invariant clause must be pure." + r.text)
                }
            }

            override fun visit(n: JmlExpressionStmt, arg: LintProblemReporter) {
                val r = PurityVisitor()
                n.expression.accept(r, null)
                if (r.reason != null) {
                    arg.error(r.reason!!, "", "", "Expression in JML statements must be pure." + r.text)
                }
            }
        }

    private class PurityVisitor : VoidVisitorAdapter<Void?>() {
        var reason: Node? = null
        var text: String? = null

        override fun visit(n: AssignExpr, arg: Void?) {
            reason = n
        }

        override fun visit(n: UnaryExpr, arg: Void?) {
            when (n.operator) {
                UnaryExpr.Operator.POSTFIX_DECREMENT, UnaryExpr.Operator.POSTFIX_INCREMENT -> {
                    reason = n
                    text = "Postfix de-/increment operator found."
                }

                UnaryExpr.Operator.PREFIX_INCREMENT, UnaryExpr.Operator.PREFIX_DECREMENT -> {
                    reason = n
                    text = "Prefix de-/increment operator found"
                }

                else -> n.expression.accept(this, arg)
            }
        }

        override fun visit(n: MethodCallExpr, arg: Void?) {
            val r = n.resolve().toAst().getOrNull()
            val mods = r as? NodeWithModifiers<*>

            if (mods?.hasModifier(Modifier.DefaultKeyword.JML_PURE) == true ||
                mods?.hasModifier(Modifier.DefaultKeyword.JML_STRICTLY_PURE) == true
            ) {
                super.visit(n, arg)
            } else {
                reason = n
                text = METHOD_NOT_PURE
            }
        }
    }

    companion object {
        const val METHOD_NOT_PURE: String = "JML expressions should be pure and this method might not be pure"
        const val ASSIGNMENT_NOT_PURE: String = "JML expressions should be pure and assignments are not pure"
    }
}

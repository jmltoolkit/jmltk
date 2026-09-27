/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import com.github.javaparser.ast.jml.clauses.JmlSimpleExprClause
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRuleVisitor

/**
 * ## Rule: `\result` may only be used in the postcondition of a non-void method
 *
 * `\result` is the JML pseudo-variable that denotes the value returned by a
 * method call (JML Reference Manual, "Result" in the chapter on spec
 * expressions). It only has a meaning **after** the method has terminated
 * normally, and only if the method actually returns a value. Consequently,
 * JML permits `\result` only in the postcondition clauses of a method with a
 * non-`void` return type; every other use is a specification error, and most
 * JML tools reject it.
 *
 * ### Checked cases
 *
 * Every occurrence of the name `\result` is inspected. The enclosing clause is
 * a valid postcondition if, and only if, its clause kind is one of
 *
 *  - `ensures`, `ensures_free`, `ensures_redundantly` &mdash; the lightweight
 *    postcondition clauses, and
 *  - `post`, `post_redundantly` &mdash; the heavyweight-specification
 *    counterparts (e.g. inside a `normal_behavior` block).
 *
 * Two problems are reported:
 *
 *  - **`\result` in a non-postcondition clause** &mdash; e.g. in `requires`,
 *    `signals`, `loop_invariant`, `assert` or `assume`. There is no return
 *    value in these contexts: the method has not terminated (preconditions,
 *    loop invariants, assertions) or has terminated abruptly (`signals`).
 *
 *  - **`\result` for a method or constructor without a return value** &mdash;
 *    i.e. a method declared `void`, or a constructor. Nothing is returned, so
 *    `\result` cannot denote anything.
 *
 * ### Known limitations
 *
 *  - The context is tracked per *clause kind*, not semantically. Occurrences of
 *    `\result` in JML statements **inside the method body** (e.g. `assert` or
 *    block contracts) are always treated as non-postconditions and may be
 *    reported even where a tool would accept them.
 *  - Only simple name occurrences of `\result` are recognized, as produced by
 *    the parser.
 *
 * ### Examples
 *
 * Lightweight specification, good:
 * ```java
 * //@ requires n >= 0;
 * //@ ensures \result >= 0;
 * public int abs(int n) { ... }
 * ```
 *
 * Heavyweight specification, good:
 * ```java
 * /*@ public normal_behavior
 *   @   requires n >= 0;
 *   @   ensures \result == n;
 *   @ also public exceptional_behavior
 *   @   signals (IllegalArgumentException) true;
 *   @
 * public int abs(int n) { ... }
 * */
 * ```
 * (Note: the `signals` clause must not mention `\result`; the exception object
 * is referred to by its own identifier.)
 *
 * Bad &mdash; `\result` in a precondition (no return value exists yet):
 * ```java
 * //@ requires \result > 0;  // error: use of \result in a non-postcondition clause
 * //@ ensures \result > 0;
 * public int get() { ... }
 * ```
 *
 * Bad &mdash; `\result` in a `signals` clause (the method terminated abruptly,
 * no value was returned):
 * ```java
 * //@ signals (Exception) \result == null;  // error: use of \result in a non-postcondition clause
 * public int get() { ... }
 * ```
 *
 * Bad &mdash; `\result` for a `void` method or a constructor:
 * ```java
 * //@ ensures \result == null;  // error: method does not return anything
 * public void print() { ... }
 * ```
 *
 * @author Alexander Weigl
 * @version 1 (14.10.22)
 */
class ResultVarCheck : LintRuleVisitor() {
    override val visitor: VoidVisitorAdapter<LintProblemReporter>
        get() = object : VoidVisitorAdapter<LintProblemReporter>() {
            private var inMethodWithNonVoidReturnType = false
            private var inPostCondition = false

            override fun visit(n: MethodDeclaration, arg: LintProblemReporter) {
                inMethodWithNonVoidReturnType = !n.type.isVoidType
                n.contracts.forEach { v -> v.accept(this, arg) }
                inMethodWithNonVoidReturnType = false
                n.body.ifPresent { l -> l.accept(this, arg) }
            }

            override fun visit(n: JmlSimpleExprClause, arg: LintProblemReporter?) {
                inPostCondition =
                    n.kind === JmlClauseKind.ENSURES || n.kind === JmlClauseKind.ENSURES_FREE || n.kind === JmlClauseKind.ENSURES_REDUNDANTLY || n.kind === JmlClauseKind.POST || n.kind === JmlClauseKind.POST_REDUNDANTLY
                super.visit(n, arg)
                inPostCondition = false
            }

            override fun visit(n: NameExpr, arg: LintProblemReporter) {
                if (n.nameAsString.equals("\\result")) {
                    if (!inPostCondition) arg.error(n, "", "", "Use of \\result in non-post-conditional clause.")
                    if (!inMethodWithNonVoidReturnType) arg.error(n, "", "", NO_METHOD_RESULT)
                }
            }
        }

    companion object {
        const val NO_METHOD_RESULT: String =
            "Cannot use \\result here, as this method / constructor does not return anything"
    }
}

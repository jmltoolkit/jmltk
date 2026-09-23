/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jmlparser.lint.rules

import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import com.github.javaparser.ast.jml.clauses.JmlContract
import com.github.javaparser.ast.jml.clauses.JmlSignalsOnlyClause
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRuleVisitor
import kotlin.jvm.optionals.getOrNull

/**
 * ## Rule: at most one `signals_only` clause per contract
 *
 * A `signals_only` clause (JML Reference Manual, chapter on exception
 * specifications) restricts the set of exception types that a method may
 * throw when its contract is satisfied. Multiple `signals_only` clauses in
 * the same contract are at best redundant and at worst confusing: it is
 * unclear whether their exception sets are to be intersected or united, and
 * JML tools differ in what they accept. A single clause with the union of
 * the intended exception types expresses the intent unambiguously.
 *
 * ### Checked cases
 *
 *  - **More than one `signals_only` clause** (WARN): the second and every
 *    further `signals_only` clause of a contract is reported, including
 *    clauses in nested behavior cases of heavyweight specifications.
 *
 * ### Status and intended checks
 *
 * Only the `signals_only` check is currently implemented. Further
 * context-sensitive checks are planned but not implemented yet; some of them
 * are already covered by dedicated rules:
 *
 *  - the use of `\result` outside of postcondition clauses is checked by the
 *    result-variable rule,
 *  - the use of `\old(...)` outside of `ensures`/`signals` clauses, `assert`
 *    and `assume` statements, and loop invariants is *not* checked yet,
 *  - redundant `\not_specified` clauses next to an explicit specification of
 *    the same clause kind are *not* checked yet.
 *
 * ### Known limitations
 *
 *  - The clause counter spans the whole contract tree, including the
 *    individual behavior cases of a heavyweight specification; a
 *    `signals_only` clause in each of two `also` cases is reported even
 *    though each case has only one.
 *
 * ### Examples
 *
 * Good &mdash; a single `signals_only` clause with the union of exceptions:
 * ```java
 * //@ signals_only IllegalArgumentException, IllegalStateException;
 * public void check() { ... }
 * ```
 *
 * Bad &mdash; multiple `signals_only` clauses:
 * ```java
 * //@ signals_only IllegalArgumentException;
 * //@ signals_only IllegalStateException;  // warning: use a single signals_only clause
 * public void check() { ... }
 * ```
 *
 * @author Alexander Weigl
 * @version 1 (12/29/21)
 */
class ContextSensitiveForbiddenFunctionsValidator : LintRuleVisitor() {

    override val visitor: VoidVisitorAdapter<LintProblemReporter>
        = object : VoidVisitorAdapter<LintProblemReporter>() {
        private var signalsOnlyCounter = 0

        override fun visit(n: JmlSignalsOnlyClause, arg: LintProblemReporter) {
            signalsOnlyCounter++
            if (signalsOnlyCounter == 2) { // warn only once
                arg.warn(n, "", "", MULTIPLE_SIGNALS_ONLY)
            }
        }

        override fun visit(n: JmlContract, arg: LintProblemReporter) {
            if(n.parentNode.getOrNull() !is JmlContract) { // do not reset on nested contract
                signalsOnlyCounter = 0
            }
            super.visit(n, arg)
        }
    }


    companion object {
        const val MULTIPLE_SIGNALS_ONLY: String = "Use a single signals_only clause to avoid confusion"
        const val NOT_SPECIFIED_REDUNDANT: String =
            "This clause containing \\not_specified is redundant because you already specified it"
        const val BACKSLASH_RESULT_NOT_ALLOWED: String = "You can only use \\result in an ensures clause"
        const val OLD_EXPR_NOT_ALLOWED: String =
            "You can only use an \\old() expressions in ensures and signals clauses, assert and assume statements, and in loop invariants"
    }
}

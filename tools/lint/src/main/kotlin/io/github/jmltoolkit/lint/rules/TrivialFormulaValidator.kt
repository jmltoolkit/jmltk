/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import com.github.javaparser.ast.jml.clauses.JmlClauseKind.*
import com.github.javaparser.ast.jml.clauses.JmlSignalsClause
import com.github.javaparser.ast.jml.clauses.JmlSimpleExprClause
import com.github.javaparser.ast.jml.stmt.JmlExpressionStmt
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRuleVisitor

/**
 * ## Rule: detect trivially true or trivially false specification formulas
 *
 * A specification clause whose formula always evaluates to the same truth
 * value carries no information: a *trivially true* clause is vacuous (it
 * permits every behavior), and a *trivially false* clause can never be
 * satisfied (for preconditions and assertions this makes the program
 * immediately incorrect, for postconditions the method unverifiable). Both
 * usually indicate a typo or a leftover placeholder rather than intent, so
 * this rule reports them.
 *
 * ### Checked cases
 *
 * The formulas of the following clauses and statements are analyzed:
 *
 *  - preconditions: `requires`, `requires_free`, `requires_redundantly`,
 *    `pre`, `pre_redundantly`,
 *  - postconditions: `ensures`, `ensures_free`, `ensures_redundantly`,
 *    `post`, `post_redundantly`,
 *  - exception postconditions: `signals`,
 *  - JML statements: `assert`, `assert_redundantly`, `assume`,
 *    `assume_redundantly`.
 *
 * Reported problems:
 *
 *  - **Trivially true** (HINT): the clause is vacuous; consider removing it or
 *    strengthening the formula.
 *  - **Trivially false** (WARN): the clause can never be satisfied.
 *
 * ### How it works
 *
 * Each formula is evaluated by a simple abstract interpretation over the
 * three-valued boolean lattice `{ VALID, INVALID, UNKNOWN }`; anything that
 * cannot be decided soundly evaluates to `UNKNOWN` and is not reported.
 * Decidable cases include:
 *
 *  - boolean and comparison operators over literal operands
 *    (`true`, `1 < 2`, `"a" == "a"`),
 *  - the boolean connectives `!`, `&&`, `||`, `^` and the conditional
 *    operator, with short-circuiting on decided operands,
 *  - syntactic patterns such as `x && !x` (always false) and `x || !x`
 *    (always true) for the same simple name `x`,
 *  - `null instanceof T` (always false).
 *
 * ### Known limitations
 *
 *  - No values of variables, fields or method results are tracked; the
 *    analysis is purely local to the formula.
 *  - Syntactic equality is by simple name only, so `a.b && !a.b` or
 *    `x && !y` with `x == y` are not recognized.
 *  - JML-specific operators (quantifiers, `\old`, `\result`, `\reach`, ...)
 *    evaluate to `UNKNOWN` and are never reported.
 *
 * ### Examples
 *
 * Good &mdash; informative formulas:
 * ```java
 * //@ requires n >= 0;
 * //@ ensures \result == n;
 * public int id(int n) { ... }
 * ```
 *
 * Bad &mdash; trivially true (vacuous) clauses:
 * ```java
 * //@ requires true;        // hint: vacuous
 * //@ requires 1 < 2;       // hint: vacuous
 * //@ ensures b || !b;      // hint: vacuous
 * ```
 *
 * Bad &mdash; trivially false clauses:
 * ```java
 * //@ requires 1 > 2;            // warning: can never be satisfied
 * //@ assert b && !b;            // warning: can never be satisfied
 * ```
 *
 * @author Alexander Weigl
 * @version 1 (22.09.26)
 */
class TrivialFormulaValidator : LintRuleVisitor() {
    private val evaluator = FormulaEvaluator()

    override val visitor: VoidVisitorAdapter<LintProblemReporter>
        get() = object : VoidVisitorAdapter<LintProblemReporter>() {
            override fun visit(n: JmlSimpleExprClause, arg: LintProblemReporter) {
                if (n.kind in CHECKED_KINDS) {
                    checkFormula(n.expression, n.kind.jmlSymbol(), arg)
                }
                super.visit(n, arg)
            }

            override fun visit(n: JmlSignalsClause, arg: LintProblemReporter) {
                checkFormula(n.expression, "signals", arg)
                super.visit(n, arg)
            }

            override fun visit(n: JmlExpressionStmt, arg: LintProblemReporter) {
                if (n.kind == JmlExpressionStmt.JmlStmtKind.ASSERT ||
                    n.kind == JmlExpressionStmt.JmlStmtKind.ASSERT_REDUNDANTLY ||
                    n.kind == JmlExpressionStmt.JmlStmtKind.ASSUME ||
                    n.kind == JmlExpressionStmt.JmlStmtKind.ASSUME_REDUNDANTLY
                ) {
                    checkFormula(n.expression, n.kind.jmlSymbol(), arg)
                }
                super.visit(n, arg)
            }
        }

    private fun checkFormula(expr: Expression, clause: String, arg: LintProblemReporter) {
        when (evaluator.eval(expr)) {
            BoolAbsValue.VALID ->
                arg.hint(
                    expr, "", ID,
                    "The formula of the '%s' clause is trivially true; the clause is vacuous.", clause
                )

            BoolAbsValue.INVALID ->
                arg.warn(
                    expr, "", ID,
                    "The formula of the '%s' clause is trivially false; the clause can never be satisfied.", clause
                )

            BoolAbsValue.UNKNOWN -> { /* nothing known, no report */ }
        }
    }

    companion object {
        const val ID = "trivial-formula"

        /** Clause kinds whose formula is a boolean predicate we reason about. */
        val CHECKED_KINDS: Set<JmlClauseKind> = setOf(
            REQUIRES, REQUIRES_FREE, REQUIRES_REDUNDANTLY,
            ENSURES, ENSURES_FREE, ENSURES_REDUNDANTLY,
            PRE, PRE_REDUNDANTLY, POST, POST_REDUNDANTLY
        )
    }
}

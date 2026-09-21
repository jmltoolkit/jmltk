/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import com.github.javaparser.ast.jml.clauses.JmlMultiExprClause
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRule
import io.github.jmltoolkit.lint.LintRuleVisitor
import io.github.jmltoolkit.lint.rules.locset.AbsLoc
import io.github.jmltoolkit.lint.rules.locset.LocsetEvaluator

/**
 * Runs the locset abstract interpretation ([LocsetEvaluator]) on frame clauses
 * (`assignable`, `accessible`, `modifiable`, `modifies`) and reports the classified
 * results:
 *
 *  - the frame is equivalent to `\\everything` (WARN): the clause is vacuous, it has
 *    the same meaning as omitting the clause,
 *  - the frame is provably empty (HINT): the method has no visible effect on the
 *    state space considered; state-preservation reasoning applies,
 *  - the frame is contradictory (ERROR): the locset can not denote any set.
 *
 * @author Alexander Weigl
 * @version 1 (21.09.26)
 */
class AssignableLocsetValidator : LintRuleVisitor() {
    override fun visit(n: JmlMultiExprClause, arg: LintProblemReporter) {
        if (n.kind !in FRAME_KINDS) return
        val value = LocsetEvaluator().frame(n.expression.toList())
        when {
            value.isBottom -> arg.error(
                n, CATEGORY, CONTRADICTORY.id,
                "The locset of this ${n.kind} clause is contradictory: it is provably " +
                        "empty, yet contains \\\\everything.",
            )

            value.containsEverything -> arg.warn(
                n, CATEGORY, VACUOUS.id,
                "The locset of this ${n.kind} clause contains \\\\everything. The clause is " +
                        "vacuous: it has the same meaning as omitting the clause.",
            )

            value.alwaysEmpty -> arg.hint(
                n, CATEGORY, ALWAYS_EMPTY.id,
                "The locset of this ${n.kind} clause is provably empty (\\\\nothing): the " +
                        "method has no effect visible in this frame.",
            )
        }
        super.visit(n, arg)
    }

    companion object {
        const val CATEGORY = "locset"

        private val FRAME_KINDS = setOf(
            JmlClauseKind.ASSIGNABLE,
            JmlClauseKind.ASSIGNABLE_REDUNDANTLY,
            JmlClauseKind.MODIFIABLE,
            JmlClauseKind.MODIFIABLE_REDUNDANTLY,
            JmlClauseKind.MODIFIES,
            JmlClauseKind.MODIFIES_REDUNDANTLY,
            JmlClauseKind.ACCESSIBLE,
        )

        val ALWAYS_EMPTY: LintProblemMeta = LintProblemMeta(
            "JML-LOCSET-1",
            "Frame clause is provably empty",
            LintRule.HINT,
        )

        val VACUOUS: LintProblemMeta = LintProblemMeta(
            "JML-LOCSET-2",
            "Frame clause is equivalent to \\everything",
            LintRule.WARN,
        )

        val CONTRADICTORY: LintProblemMeta = LintProblemMeta(
            "JML-LOCSET-3",
            "Frame clause is contradictory",
            LintRule.ERROR,
        )
    }
}

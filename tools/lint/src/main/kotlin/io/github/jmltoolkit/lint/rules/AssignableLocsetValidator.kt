/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import com.github.javaparser.ast.jml.clauses.JmlMultiExprClause
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRule
import io.github.jmltoolkit.lint.LintRuleVisitor
import io.github.jmltoolkit.lint.rules.locset.AbsLoc
import io.github.jmltoolkit.lint.rules.locset.LocsetEvaluator

/**
 * ## Rule: classify frame clauses by their abstract locset
 *
 * A frame clause (`assignable`, `accessible`, `modifiable`, `modifies`)
 * denotes the set of memory locations a method may touch. The extreme cases
 * of that set carry little or contradictory information and usually indicate
 * a mistake: a frame containing `\everything` permits the method to modify
 * anything (the clause is vacuous &mdash; the same as writing no clause at
 * all), and a frame that is simultaneously empty and universal cannot denote
 * any set. This rule computes an abstraction of the locset and reports the
 * classified extreme cases.
 *
 * ### Checked cases
 *
 * Frame clauses of kind `assignable`, `assignable_redundantly`,
 * `modifiable`, `modifiable_redundantly`, `modifies`, `modifies_redundantly`
 * and `accessible` are evaluated:
 *
 *  - **Vacuous frame** (`JML-LOCSET-2`, WARN): the locset provably contains
 *    `\everything`; the clause has the same meaning as omitting it.
 *
 *  - **Provably empty frame** (`JML-LOCSET-1`, HINT): the locset is provably
 *    `\nothing`; the method has no effect visible in this frame. Idiomatic
 *    for pure methods (`assignable \nothing`), so only a hint.
 *
 *  - **Contradictory frame** (`JML-LOCSET-3`, ERROR): the locset is provably
 *    empty yet contains `\everything`; it can not denote any set.
 *
 * ### How it works
 *
 * The expressions of the clause are evaluated by an abstract interpretation
 * over the product domain *emptiness* (definitely empty / unknown /
 * definitely non-empty) &times; *universality* (definitely contains
 * `\everything` / unknown / definitely not). The comma-separated expression
 * list is joined by set union; an absent expression list defaults to
 * `\everything` (the JML default frame). Recognized forms include the locset
 * literals `\nothing`/`\strictly_nothing`/`\everything`, single locations
 * (`this`, `o.f`, `a[i]`), the bulk patterns `a[*]`, `a[0..n]` and `o.*`,
 * set comprehensions, `\old(...)` labels, the conditional operator (union of
 * the branches), and set union/intersection/difference written as binary
 * operators. Anything else (method calls, arithmetic, ...) evaluates to the
 * unknown value and is never reported.
 *
 * ### Known limitations
 *
 *  - Unknown names are abstracted as a single location; locset-valued ghost
 *    variables are not tracked (no environment is supplied by this rule).
 *  - The contradictory case is currently unreachable: no transfer function
 *    derives the contradictory abstract value, so `JML-LOCSET-3` is
 *    defensive, reserved for future locset operators.
 *  - An intentionally broad frame (`assignable \everything`) is reported even
 *    where the breadth is intended.
 *
 * ### Examples
 *
 * Good &mdash; a concrete, informative frame:
 * ```java
 * //@ assignable count, cache[*];
 * public void increment() { ... }
 * ```
 *
 * Hint &mdash; provably empty frame (idiomatic for pure methods):
 * ```java
 * //@ assignable \nothing;  // hint: no effect visible in this frame
 * /*@ pure */
 * public int get() { ... }
 * ```
 *
 * Bad &mdash; vacuous frame:
 * ```java
 * //@ assignable \everything;   // warning: same meaning as omitting the clause
 * //@ assignable x, \everything;  // warning: contains \everything
 * public void touch() { ... }
 * ```
 *
 * @author Alexander Weigl
 * @version 1 (21.09.26)
 */
class AssignableLocsetValidator : LintRuleVisitor() {
    override val visitor: VoidVisitorAdapter<LintProblemReporter> = Visitor
    private object Visitor :  VoidVisitorAdapter<LintProblemReporter>() {
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
            HINT,
        )

        val VACUOUS: LintProblemMeta = LintProblemMeta(
            "JML-LOCSET-2",
            "Frame clause is equivalent to \\everything",
            WARN,
        )

        val CONTRADICTORY: LintProblemMeta = LintProblemMeta(
            "JML-LOCSET-3",
            "Frame clause is contradictory",
            ERROR,
        )
    }
}

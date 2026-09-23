/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.jml.clauses.JmlSignalsClause
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRule
import io.github.jmltoolkit.lint.LintRuleVisitor

/**
 * Checks the use of the JML keyword `\exception`, which refers to the exception
 * object thrown by a method (JML Reference Manual, clause on `\exception` in
 * the chapter on JML expressions).
 *
 * `\exception` is only bound within a `signals` clause. This rule reports two
 * misuses:
 *
 *  - **Outside a signals clause** (ERROR): `\exception` used in any other
 *    context, e.g. in a `requires` or `ensures` clause, an invariant, or an
 *    `assert`/`assume` statement. There is no exception object in these
 *    contexts, so the reference is meaningless.
 *
 *  - **Shadowed by an explicit identifier** (WARN): `\exception` used inside a
 *    `signals` clause that declares its own identifier, e.g.
 *    `signals (MyException e) ...`. In such a clause the exception object is
 *    bound to the declared identifier (`e`), not to `\exception`; the reference
 *    is at best confusing and should use the declared identifier instead.
 *
 * The implicitly given identifier of a `signals` clause without an own
 * identifier is itself `\exception`; it is not a name expression and is not
 * inspected by this rule.
 *
 * ## Examples
 *
 * Good &mdash; `\exception` in a `signals` clause without an own identifier:
 * ```java
 * //@ signals (IllegalArgumentException) \exception.getMessage() != null;
 * public void check() { ... }
 * ```
 *
 * Bad &mdash; `\exception` outside a `signals` clause:
 * ```java
 * //@ ensures \exception.getMessage() != null;  // error: \exception is only bound in signals clauses
 * public void check() { ... }
 * ```
 *
 * Bad &mdash; `\exception` in a `signals` clause with an own identifier:
 * ```java
 * //@ signals (IllegalArgumentException e) \exception.getMessage() != null;  // warning: use 'e' instead
 * public void check() { ... }
 * ```
 *
 * @author Alexander Weigl
 * @version 1 (23.09.26)
 */
class ExceptionVarCheck : LintRuleVisitor() {
    override val visitor: VoidVisitorAdapter<LintProblemReporter>
        get() = object : VoidVisitorAdapter<LintProblemReporter>() {
            override fun visit(n: NameExpr, arg: LintProblemReporter) {

                if (n.nameAsString != "\\exception") return

                val signals = n.findAncestor(JmlSignalsClause::class.java)
                if (signals.isEmpty) {
                    arg.error(
                        n, CATEGORY, OUTSIDE_SIGNALS.id,
                        "Use of \\exception outside of a signals clause. " +
                            "The exception object is only bound within signals clauses.",
                    )
                    return
                }

                // The enclosing signals clause binds the exception object to its declared
                // identifier; only the implicit identifier is `\exception` itself.
                val parameterName = signals.get().parameter.nameAsString
                if (parameterName != "\\exception") {
                    arg.warn(
                        n, CATEGORY, SHADOWED_BY_IDENTIFIER.id,
                        "This signals clause binds the exception object to the identifier '%s'; " +
                            "use that identifier instead of \\exception.",
                        parameterName,
                    )
                }
            }
        }


    companion object {
        const val CATEGORY = "signals"

        /** \exception used outside of a signals clause, where it is not bound. */
        val OUTSIDE_SIGNALS: LintProblemMeta = LintProblemMeta(
            "JML-EXCEPTION-1",
            "\\exception is only bound within signals clauses",
            LintRule.ERROR,
        )

        /** \exception used in a signals clause that declares an own identifier. */
        val SHADOWED_BY_IDENTIFIER: LintProblemMeta = LintProblemMeta(
            "JML-EXCEPTION-2",
            "The signals clause declares an own identifier for the exception object",
            LintRule.WARN,
        )
    }
}

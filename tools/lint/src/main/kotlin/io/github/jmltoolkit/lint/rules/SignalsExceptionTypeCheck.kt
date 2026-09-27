/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.jml.clauses.JmlSignalsClause
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.jml.JmlUtility
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRule
import io.github.jmltoolkit.lint.LintRuleVisitor

/**
 * ## Rule: the parameter of a `signals` clause must be an exception class
 *
 * A `signals` clause (JML Reference Manual, "Information Associated with
 * Exceptions in Java and JML") declares an exceptional postcondition for the
 * exception object thrown by the method, e.g.
 * `signals (MyException e) e.getMessage() != null`. Its parameter type must be
 * a subtype of `java.lang.Exception`; a clause whose parameter type is not an
 * exception class is a specification error.
 *
 * ### Checked cases
 *
 *  - **Parameter type is not an exception class** (ERROR): the declared type of
 *    the `signals` parameter is not a subtype of `java.lang.Exception`, e.g.
 *    `signals (String s) ...`.
 *
 *  - **Parameter type could not be resolved** (ERROR): the declared type cannot
 *    be resolved at all (e.g. a missing import). The rule reports this instead
 *    of aborting the whole lint run.
 *
 * ### Examples
 *
 * Good:
 * ```java
 * //@ signals (IllegalArgumentException) true;
 * //@ signals (Exception e) e.getMessage() != null;
 * public void check() { ... }
 * ```
 *
 * Bad &mdash; parameter type is not an exception class:
 * ```java
 * //@ signals (String s) s.isEmpty();  // error: this is not an exception class
 * public void check() { ... }
 * ```
 *
 * Bad &mdash; parameter type cannot be resolved:
 * ```java
 * //@ signals (MyException) true;  // error: could not be resolved, missing import?
 * public void check() { ... }
 * ```
 *
 * @author Alexander Weigl
 * @version 1 (23.09.26)
 */
class SignalsExceptionTypeCheck : LintRuleVisitor() {
    override val visitor: VoidVisitorAdapter<LintProblemReporter>
        get() = object : VoidVisitorAdapter<LintProblemReporter>() {
            override fun visit(n: JmlSignalsClause, arg: LintProblemReporter) {
                val exceptionType = JmlUtility.resolveException(n)
                val rtype = try {
                    n.parameter.type.resolve()
                } catch (e: Exception) {
                    arg.error(
                        n, CATEGORY, CLASS_REFERENCE_NOT_FOUND.id,
                        "The exception type of this signals clause could not be resolved, " +
                            "did you forget to import it?",
                    )
                    super.visit(n, arg)
                    return
                }
                if (!exceptionType.isAssignableBy(rtype)) {
                    arg.report(NOT_AN_EXCEPTION_CLASS.create(n))
                }
                super.visit(n, arg)
            }
        }
    companion object {
        const val CATEGORY = "signals"

        /** The parameter of a signals clause is not a subtype of java.lang.Exception. */
        val NOT_AN_EXCEPTION_CLASS: LintProblemMeta =
            LintProblemMeta("JML-SIGNALS-1", "This is not an exception class", LintRule.ERROR)

        /** The exception type of a signals clause could not be resolved. */
        val CLASS_REFERENCE_NOT_FOUND: LintProblemMeta =
            LintProblemMeta(
                "JML-SIGNALS-2",
                "This class could not be resolved, did you forget to import it?",
                LintRule.ERROR,
            )
    }
}

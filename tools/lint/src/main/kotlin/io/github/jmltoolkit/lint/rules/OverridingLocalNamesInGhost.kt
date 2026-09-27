/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.jml.stmt.JmlGhostStmt
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRule
import io.github.jmltoolkit.lint.LintRuleVisitor

/**
 * ## Rule: ghost variables must not reuse names of Java locals
 *
 * JML ghost variables (JML Reference Manual, chapter on ghost and model
 * variables) are specification-only variables that live in the same scopes as
 * the Java program. They follow Java's scoping and "no duplicate names"
 * rules: declaring a ghost variable with the name of an existing Java local
 * variable or parameter of the same method makes the specification ambiguous
 * &mdash; every reference to that name in a JML expression could mean either
 * declaration, and tools disagree on which one wins. Such a shadowing
 * declaration is therefore an error.
 *
 * ### Checked cases
 *
 * For every variable declared inside a `//@ ghost ...` statement, the name is
 * resolved in the scope of the enclosing Java method (via symbol resolution,
 * with the name temporarily attached to the ghost statement). If a Java
 * declaration &mdash; a local variable or a parameter of the same method
 * &mdash; is found under that name, an error is reported.
 *
 * The reverse direction &mdash; a Java local variable reusing the name of an
 * earlier ghost variable of the same method &mdash; is *not* checked here but
 * by the duplicate-names rule for JML declarations.
 *
 * ### Known limitations
 *
 *  - Resolution happens at the position of the ghost statement; names of Java
 *    locals declared *after* the ghost statement in the same block are
 *    invisible to Java at that point and are not detected as duplicates.
 *  - Fields are not considered here; a ghost variable shadowing a field of
 *    the enclosing type is a different (weaker) concern and not reported.
 *  - If the enclosing method cannot be resolved, the check silently skips the
 *    declaration.
 *
 * ### Examples
 *
 * Good &mdash; ghost variables with fresh names:
 * ```java
 * public int sum(int[] a) {
 *     //@ ghost int total = 0;
 *     //@ ghost int seen = 0;
 *     ...
 * }
 * ```
 *
 * Bad &mdash; ghost variable reusing a parameter name:
 * ```java
 * public int sum(int[] a) {
 *     //@ ghost int a = 0;  // error: 'a' already declared in Java
 *     ...
 * }
 * ```
 *
 * Bad &mdash; ghost variable reusing a local variable name:
 * ```java
 * public int sum(int[] a) {
 *     int result = 0;
 *     //@ ghost int result = 0;  // error: 'result' already declared in Java
 *     ...
 * }
 * ```
 *
 * @author Alexander Weigl
 * @version 1 (14.10.22)
 */
class OverridingLocalNamesInGhost : LintRuleVisitor() {
    override val visitor: VoidVisitorAdapter<LintProblemReporter>
        get() = object : VoidVisitorAdapter<LintProblemReporter>() {
            override fun visit(n: VariableDeclarationExpr, arg: LintProblemReporter) {
                val ghost = n.findAncestor(JmlGhostStmt::class.java)
                if (ghost.isPresent) {
                    val enclosingGhost = ghost.get()
                    for (variable in n.variables) {
                        val name = variable.nameAsExpression
                        // The name expression is a detached node; temporarily attach it to the
                        // ghost statement so that symbol resolution happens in the scope of the
                        // enclosing Java method. Detach it again even if resolution fails.
                        name.setParentNode(enclosingGhost)
                        val value = try {
                            enclosingGhost.symbolResolver
                                .resolveDeclaration(name, ResolvedValueDeclaration::class.java)
                        } finally {
                            name.setParentNode(null)
                        }
                        if (value != null) {
                            arg.error(
                                variable, CATEGORY, DUPLICATE_NAME.id,
                                "Variable %s already declared in Java.", variable.nameAsString,
                            )
                        }
                    }
                }
                super.visit(n, arg)
            }
        }

    companion object {
        const val CATEGORY = "names"

        /** A ghost variable reuses the name of a Java local variable or parameter. */
        val DUPLICATE_NAME: LintProblemMeta = LintProblemMeta(
            "JML-GHOST-1",
            "A ghost variable reuses the name of a Java local variable",
            LintRule.ERROR,
        )
    }
}

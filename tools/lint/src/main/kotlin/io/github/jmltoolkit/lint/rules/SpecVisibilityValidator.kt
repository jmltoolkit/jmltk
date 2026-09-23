/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.AccessSpecifier
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.jml.clauses.JmlContract
import com.github.javaparser.ast.nodeTypes.NodeWithTokenRange
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.declarations.HasAccessSpecifier
import com.github.javaparser.resolution.declarations.ResolvedDeclaration
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRule
import io.github.jmltoolkit.lint.LintRuleVisitor

/**
 * Validates the visibility of names referenced in JML specifications
 * (JML Reference Manual, §2.5 Privacy Modifiers, "Annotation-context visibility").
 *
 * An expression in a context of a given privacy level may only refer to names at that
 * level or less visible, and only where Java visibility rules also permit it.
 * For example, a public invariant may not mention a private field.
 *
 * The privacy level of a specification is taken from the privacy modifier of its
 * contract (`spec_public`, `spec_protected`, `spec_package`, `spec_private` or the
 * equivalent Java access modifier). A contract without a privacy modifier inherits
 * the level of the member it annotates (method, constructor or type); the default
 * is `spec_public`.
 *
 * The visibility of a referenced name is its Java access level, widened by JML privacy
 * modifiers on its declaration (e.g. a `private` field declared `spec_public` may be
 * referenced by public specifications). Parameters and local variables are part of the
 * specification context and are always permitted.
 *
 * Limitation: type references (e.g. casts to a private nested class) are not inspected,
 * only value and method references.
 *
 *
 * @author Alexander Weigl
 * @version 1 (21.09.26)
 */
class SpecVisibilityValidator : LintRuleVisitor() {
    private fun reportProblem(n: Node, meta: LintProblemMeta, arg: LintProblemReporter) {
        @Suppress("UNCHECKED_CAST")
        val node = n as NodeWithTokenRange<Node?>
        arg.report(meta.create(node))
    }

    override val visitor: VoidVisitorAdapter<LintProblemReporter>
        get() = object : VoidVisitorAdapter<LintProblemReporter>() {
            override fun visit(n: JmlContract, arg: LintProblemReporter) {
                val level = contractPrivacyLevel(n)
                checkClauses(n, level, arg)
                super.visit(n, arg)
            }
        }

    /**
     * Checks all clauses of the given contract: every name referenced in a clause
     * expression must be at least as visible as the contract's privacy level.
     */
    private fun checkClauses(contract: JmlContract, level: Int, arg: LintProblemReporter) {
        for (clause in contract.clauses) {
            for (expr in clause.findAll(Expression::class.java)) {
                if (!isReference(expr)) continue
                // Skip the scope of a field access or method call: the outermost
                // reference is checked as a whole (e.g. `a.b.c` is checked as one
                // field access, not also as the name `a`).
                if (isScopeOfBiggerReference(expr)) continue
                checkReference(expr, level, arg)
            }
        }
    }

    private fun isReference(expr: Expression): Boolean =
        expr is NameExpr || expr is FieldAccessExpr || expr is MethodCallExpr

    private fun isScopeOfBiggerReference(expr: Expression): Boolean {
        val parent = expr.parentNode.orElse(null) ?: return false
        val scope: Expression = when (parent) {
            is FieldAccessExpr -> parent.scope as Expression
            is MethodCallExpr -> parent.scope as Expression
            else -> return false
        }
        return scope == expr
    }

    private fun checkReference(expr: Expression, level: Int, arg: LintProblemReporter) {
        val name = when (expr) {
            is NameExpr -> expr.nameAsString
            is FieldAccessExpr -> expr.nameAsString
            is MethodCallExpr -> expr.nameAsString
            else -> return
        }
        val resolved = resolve(expr) ?: return
        val visibility = effectiveVisibility(resolved) ?: return
        if (visibility < level) {
            arg.error(
                expr, CATEGORY, SPEC_LESS_VISIBLE.id,
                "The name '$name' (${levelName(visibility)}) is less visible than the " +
                    "${levelName(level)} specification it is used in. An expression in a context " +
                    "of a given privacy level may only refer to names at that level or more visible.",
            )
        }
    }

    private fun resolve(expr: Expression): ResolvedDeclaration? = try {
        when (expr) {
            is MethodCallExpr ->
                expr.symbolResolver.resolveDeclaration(expr, ResolvedMethodDeclaration::class.java)

            else ->
                expr.symbolResolver.resolveDeclaration(expr, ResolvedValueDeclaration::class.java)
        }
    } catch (_: Exception) {
        null
    }

    /**
     * The privacy level of the given contract: the explicit privacy modifier of the
     * contract, or the level of the annotated member, or `spec_public` (the default).
     */
    private fun contractPrivacyLevel(n: JmlContract): Int {
        modifierLevel(n.modifiers)?.let { return it }
        val callable = n.findAncestor(CallableDeclaration::class.java)
        if (callable.isPresent) {
            // default: the privacy of the annotated member (its Java access level)
            return modifierLevel(callable.get().modifiers) ?: LEVEL_PACKAGE
        }
        val type = n.findAncestor(TypeDeclaration::class.java)
        if (type.isPresent) {
            return modifierLevel(type.get().modifiers) ?: LEVEL_PACKAGE
        }
        return LEVEL_PUBLIC
    }

    /**
     * The effective visibility of a resolved declaration, as seen by a client of the
     * specification: the Java access level, widened by JML privacy modifiers on the
     * declaration (`spec_public` etc.). Returns null if the visibility cannot be
     * determined (e.g. parameters or local variables, which are always permitted).
     */
    private fun effectiveVisibility(d: ResolvedDeclaration): Int? {
        if (d !is HasAccessSpecifier) return null
        var level = level(d.accessSpecifier())
        when (d) {
            is JavaParserFieldDeclaration ->
                modifierLevel(d.wrappedNode.modifiers)?.let { level = maxOf(level, it) }

            is JavaParserMethodDeclaration ->
                modifierLevel(d.wrappedNode.modifiers)?.let { level = maxOf(level, it) }
        }
        return level
    }

    private fun level(a: AccessSpecifier): Int = when (a) {
        AccessSpecifier.PRIVATE -> LEVEL_PRIVATE
        AccessSpecifier.NONE -> LEVEL_PACKAGE
        AccessSpecifier.PROTECTED -> LEVEL_PROTECTED
        AccessSpecifier.PUBLIC -> LEVEL_PUBLIC
    }

    /**
     * The most visible level given by the JML privacy or Java access modifiers,
     * or null if no such modifier is present.
     */
    private fun modifierLevel(modifiers: Iterable<Modifier>): Int? {
        var found: Int? = null
        for (modifier in modifiers) {
            val l = when (modifier.keyword) {
                Modifier.DefaultKeyword.PUBLIC,
                Modifier.DefaultKeyword.JML_SPEC_PUBLIC -> LEVEL_PUBLIC

                Modifier.DefaultKeyword.PROTECTED,
                Modifier.DefaultKeyword.JML_SPEC_PROTECTED -> LEVEL_PROTECTED

                Modifier.DefaultKeyword.JML_PACKAGE,
                Modifier.DefaultKeyword.JML_SPEC_PACKAGE -> LEVEL_PACKAGE

                Modifier.DefaultKeyword.PRIVATE,
                Modifier.DefaultKeyword.JML_SPEC_PRIVATE -> LEVEL_PRIVATE

                else -> null
            }
            if (l != null && (found == null || l > found)) found = l
        }
        return found
    }

    private fun levelName(level: Int): String = when (level) {
        LEVEL_PRIVATE -> "spec_private"
        LEVEL_PACKAGE -> "spec_package"
        LEVEL_PROTECTED -> "spec_protected"
        else -> "spec_public"
    }

    companion object {
        const val CATEGORY = "visibility"

        private const val LEVEL_PRIVATE = 0
        private const val LEVEL_PACKAGE = 1
        private const val LEVEL_PROTECTED = 2
        private const val LEVEL_PUBLIC = 3

        /** A specification references a name that is less visible than itself. */
        val SPEC_LESS_VISIBLE: LintProblemMeta = LintProblemMeta(
            "JML-VISIBILITY-1",
            "A specification may only refer to names at its own privacy level or more visible.",
            LintRule.ERROR,
        )
    }
}

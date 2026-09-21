/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.EnumDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.RecordDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.jml.body.JmlFieldDeclaration
import com.github.javaparser.ast.jml.body.JmlMethodDeclaration
import com.github.javaparser.ast.jml.stmt.JmlGhostStmt
import com.github.javaparser.ast.nodeTypes.NodeWithTokenRange
import io.github.jmltoolkit.lint.LintProblem
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRule
import io.github.jmltoolkit.lint.LintRuleVisitor

/**
 * Validates that JML declarations do not use the same names as corresponding Java
 * declarations in the same scope (JML Reference Manual: "No duplicate names" —
 * if Java would reject it as a duplicate declaration, JML may not declare it either).
 *
 * Checked scopes and declarations:
 *  - class-level: ghost and model fields (e.g. `//@ ghost int count;`) may not share
 *    a name with a Java field of the same type, and model methods may not have the
 *    same signature as a Java method of the same type,
 *  - method bodies: a Java local variable may not reuse the name of a ghost variable
 *    of the same method (the reverse direction, a ghost variable shadowing a Java
 *    local, is checked by [OverridingLocalNamesInGhost]).
 *
 * Method signatures are compared by name and parameter count.
 *
 * @author Alexander Weigl
 * @version 1 (21.09.26)
 */
class JmlDuplicateNamesValidator : LintRuleVisitor() {
    private fun report(n: Node, meta: LintProblemMeta, name: String, arg: LintProblemReporter) {
        @Suppress("UNCHECKED_CAST")
        val node = n as NodeWithTokenRange<Node?>
        arg.report(
            LintProblem(
                meta.level,
                "The name '$name' is already used by a Java declaration in the same scope. " +
                        "JML declarations may not use the same names as corresponding Java " +
                        "declarations: if Java would reject it as a duplicate, JML may not " +
                        "declare it either.",
                node.tokenRange.orElse(null),
                meta.id,
            )
        )
    }

    override fun visit(n: ClassOrInterfaceDeclaration, arg: LintProblemReporter) {
        checkType(n, arg)
        super.visit(n, arg)
    }

    override fun visit(n: EnumDeclaration, arg: LintProblemReporter) {
        checkType(n, arg)
        super.visit(n, arg)
    }

    override fun visit(n: RecordDeclaration, arg: LintProblemReporter) {
        checkType(n, arg)
        super.visit(n, arg)
    }

    /**
     * Checks the members of a type declaration for name clashes between JML
     * declarations (ghost/model fields and model methods) and Java declarations.
     */
    private fun checkType(n: TypeDeclaration<*>, arg: LintProblemReporter) {
        val javaFields = HashMap<String, FieldDeclaration>()
        val jmlFields = HashMap<String, FieldDeclaration>()
        val javaMethods = HashMap<String, MethodDeclaration>()
        val jmlMethods = HashMap<String, MethodDeclaration>()

        for (member in n.members) {
            when (member) {
                is JmlFieldDeclaration -> collectFields(member.decl, jmlFields)

                is JmlMethodDeclaration -> collectMethods(member.methodDeclaration, jmlMethods)

                is FieldDeclaration ->
                    collectFields(member, if (isJmlDeclaration(member)) jmlFields else javaFields)

                is MethodDeclaration ->
                    collectMethods(member, if (isJmlDeclaration(member)) jmlMethods else javaMethods)
            }
        }

        for ((name, decl) in jmlFields) {
            if (javaFields.containsKey(name)) {
                report(decl, DUPLICATE_MEMBER, name, arg)
            }
        }
        for ((_, decl) in jmlMethods) {
            if (javaMethods.containsKey(signature(decl))) {
                report(decl, DUPLICATE_MEMBER, decl.nameAsString, arg)
            }
        }
    }

    private fun collectFields(decl: FieldDeclaration, target: HashMap<String, FieldDeclaration>) {
        for (v in decl.variables) {
            target.putIfAbsent(v.nameAsString, decl)
        }
    }

    private fun collectMethods(decl: MethodDeclaration, target: HashMap<String, MethodDeclaration>) {
        target.putIfAbsent(signature(decl), decl)
    }

    private fun signature(m: MethodDeclaration): String = "${m.nameAsString}/${m.parameters.size}"

    /**
     * Whether the given declaration is a JML declaration, i.e. carries the `ghost`
     * or `model` modifier.
     */
    private fun isJmlDeclaration(decl: FieldDeclaration): Boolean = decl.modifiers.any {
        it.keyword == Modifier.DefaultKeyword.JML_GHOST || it.keyword == Modifier.DefaultKeyword.JML_MODEL
    }

    private fun isJmlDeclaration(decl: MethodDeclaration): Boolean = decl.modifiers.any {
        it.keyword == Modifier.DefaultKeyword.JML_GHOST || it.keyword == Modifier.DefaultKeyword.JML_MODEL
    }

    /**
     * Checks method bodies: a Java local variable may not reuse the name of a ghost
     * variable declared in the same method.
     */
    override fun visit(n: MethodDeclaration, arg: LintProblemReporter) {
        val body = n.body.orElse(null)
        if (body != null) {
            val ghostNames = HashSet<String>()
            for (ghost in body.findAll(JmlGhostStmt::class.java)) {
                for (v in ghost.findAll(VariableDeclarationExpr::class.java)) {
                    v.variables.forEach { ghostNames.add(it.nameAsString) }
                }
            }
            if (ghostNames.isNotEmpty()) {
                for (local in body.findAll(VariableDeclarationExpr::class.java)) {
                    if (local.findAncestor(JmlGhostStmt::class.java).isPresent) continue
                    for (v in local.variables) {
                        if (v.nameAsString in ghostNames) {
                            report(v, DUPLICATE_LOCAL, v.nameAsString, arg)
                        }
                    }
                }
            }
        }
        super.visit(n, arg)
    }

    companion object {
        /** A JML declaration duplicates a Java declaration in the same scope. */
        val DUPLICATE_MEMBER: LintProblemMeta = LintProblemMeta(
            "JML-DUP-1",
            "JML declaration duplicates a Java declaration in the same scope",
            LintRule.ERROR,
        )

        /** A Java local variable duplicates a ghost variable. */
        val DUPLICATE_LOCAL: LintProblemMeta = LintProblemMeta(
            "JML-DUP-2",
            "Java local variable duplicates a ghost variable",
            LintRule.ERROR,
        )
    }
}

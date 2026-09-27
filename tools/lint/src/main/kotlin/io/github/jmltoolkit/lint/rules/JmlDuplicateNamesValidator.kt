/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.jml.body.JmlFieldDeclaration
import com.github.javaparser.ast.jml.body.JmlMethodDeclaration
import com.github.javaparser.ast.jml.stmt.JmlGhostStmt
import com.github.javaparser.ast.nodeTypes.NodeWithTokenRange
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import io.github.jmltoolkit.lint.LintProblem
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRule
import io.github.jmltoolkit.lint.LintRuleVisitor

/**
 * ## Rule: JML declarations must not duplicate Java declarations in the same
 * scope
 *
 * JML declarations (ghost and model fields, model methods) live in the same
 * namespaces as the Java program: they share the member namespace of their
 * type and the local namespace of their method (JML Reference Manual, "No
 * duplicate names"). If Java would reject a declaration as a duplicate, JML
 * may not declare it either &mdash; a duplicate would make every reference to
 * the name ambiguous between the Java and the JML declaration, and tools
 * disagree on which one wins. Such duplicates are reported as errors.
 *
 * ### Checked cases
 *
 *  - **Members** (`JML-DUP-1`, ERROR): within a class, interface, enum or
 *    record declaration, a ghost or model field (e.g. `//@ ghost int count;`)
 *    may not share its name with a Java field, and a model method may not
 *    have the same signature as a Java method of the same type.
 *
 *  - **Locals** (`JML-DUP-2`, ERROR): within a method body, a Java local
 *    variable may not reuse the name of a ghost variable declared in the same
 *    method. (The reverse direction &mdash; a ghost variable reusing the name
 *    of a Java local &mdash; is checked by the ghost-names rule.)
 *
 * ### How it works
 *
 * Members are partitioned by their `ghost`/`model` modifiers into JML and
 * Java declarations; fields are compared by name, methods by the signature
 * `name/parameter-count`. In method bodies, all ghost declarations are
 * collected first, then every non-ghost local declaration is checked against
 * that set.
 *
 * ### Known limitations
 *
 *  - Method signatures are compared by name and parameter **count** only;
 *    parameter types are ignored, so two *overloads* with the same number of
 *    parameters (e.g. `m(int)` and `m(String)`) are falsely reported as
 *    duplicates.
 *  - Block scoping is ignored in method bodies: a Java local in a sibling
 *    block of the ghost declaration is reported even though Java scoping
 *    would keep the names apart.
 *  - Duplicates *within* JML (e.g. two model fields with the same name) and
 *  duplicates within Java are not checked; the latter are the Java
 *  compiler's job.
 *
 * ### Examples
 *
 * Good &mdash; fresh names for ghost and model declarations:
 * ```java
 * public class Counter {
 *     private int count;
 *     //@ ghost int ghostCount;
 *     //@ model int modelCount;
 *     //@ model int remaining(int total);
 * }
 * ```
 *
 * Bad &mdash; JML members duplicating Java members:
 * ```java
 * public class Counter {
 *     private int count;
 *     //@ ghost int count;          // error: JML-DUP-1, name already used by Java field
 *     //@ model int total(int c);   // error: JML-DUP-1, same signature as Java method
 *     public int total(int c) { ... }
 * }
 * ```
 *
 * Bad &mdash; Java local duplicating a ghost variable:
 * ```java
 * public int sum(int[] a) {
 *     //@ ghost int total = 0;
 *     ...
 *     int total = 0;  // error: JML-DUP-2, name already used by ghost variable
 *     ...
 * }
 * ```
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

    override val visitor: VoidVisitorAdapter<LintProblemReporter>
        get() = object : VoidVisitorAdapter<LintProblemReporter>() {
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

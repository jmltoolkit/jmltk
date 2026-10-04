/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.jml.body.JmlBodyClauseKind
import com.github.javaparser.ast.jml.body.JmlClassExprDeclaration
import com.github.javaparser.ast.jml.expr.JmlLabelExpr

/**
 * The `\invariant_for(..)` code transformation: the statically-visible class
 * invariants of a type are gathered, and folded into a single large expression
 * in which every invariant clause is wrapped in a `\lblpos` label carrying the
 * clause's identity.
 *
 *   `(\lblpos inv_1 e1 && \lblpos inv_2 e2 && ...)`
 *
 * The labels keep the individual clauses addressable after the fold; the engine
 * later disappears them while translating (the label of a failed conjunct will
 * identify the clause that was violated).
 *
 * Statically-visible invariants are those declared on the type itself and on its
 * superclasses / superinterfaces; the concrete type's clauses come first, the
 * ancestors' afterwards (declaration order within each type). Only the plain
 * `invariant` clause kind is collected — `invariant_redundantly` duplicates a
 * proof obligation and `invariant_free` is not checked, so both are ignored.
 */
object InvariantFold {

    /** One invariant clause as declared on an owner type. */
    data class Invariant(val label: String, val expression: Expression)

    /**
     * Collects the statically-visible invariants of [type]. Clones the clause
     * expressions so the synthesized fold never steals AST nodes from the source
     * compilation unit.
     */
    @JvmStatic
    fun invariantsOf(type: TypeDeclaration<*>): List<Invariant> {
        val out = ArrayList<Invariant>()
        collect(type, out, LinkedHashSet())
        return out
    }

    private fun collect(
        type: TypeDeclaration<*>,
        out: MutableList<Invariant>,
        seen: MutableSet<String>,
    ) {
        val cls = type as? ClassOrInterfaceDeclaration ?: return
        val id = cls.nameAsString + "@" + cls.range
        if (!seen.add(id)) return
        var n = 1
        for (m in cls.members) {
            if (m !is JmlClassExprDeclaration || m.kind != JmlBodyClauseKind.INVARIANT) continue
            val label = m.name.orElse(null)?.asString() ?: "inv_${cls.nameAsString}_$n"
            n++
            out.add(Invariant(label, m.invariant.clone()))
        }
        // statically-visible ancestors: superclass and interfaces that live in the
        // same compilation unit (JML members of a class that was re-parsed without
        // JML processing would be lost, so resolution goes through the AST).
        val cu = cls.findAncestor(CompilationUnit::class.java).orElse(null) ?: return
        for (t in cls.extendedTypes + cls.implementedTypes) {
            val qn = try {
                t.resolve().asReferenceType().typeDeclaration.orElse(null)?.qualifiedName
            } catch (e: Exception) {
                null
            } ?: continue
            val anc = cu.findAll(ClassOrInterfaceDeclaration::class.java).firstOrNull {
                try {
                    it.resolve().qualifiedName == qn
                } catch (e: Exception) {
                    false
                }
            } ?: continue
            collect(anc, out, seen)
        }
    }

    /**
     * Folds [invariants] into one large conjunction of `\lblpos`-labeled clauses.
     * An empty list folds to `true`.
     */
    @JvmStatic
    fun fold(invariants: List<Invariant>): Expression {
        if (invariants.isEmpty()) return BooleanLiteralExpr(true)
        var acc: Expression? = null
        for (inv in invariants) {
            val labeled = JmlLabelExpr(JmlLabelExpr.Kind.POSITIVE, SimpleName(inv.label), inv.expression)
            acc = if (acc == null) labeled else BinaryExpr(acc, labeled, BinaryExpr.Operator.AND)
        }
        return acc!!
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import com.github.javaparser.ast.jml.clauses.JmlMultiExprClause
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRuleVisitor
import kotlin.jvm.optionals.getOrNull

/**
 * ## Rule: store-ref expressions in an `assignable` clause must denote
 * assignable locations
 *
 * An `assignable` clause defines the frame of a method: the set of locations
 * that the method (and everything it calls) is permitted to assign
 * (JML Reference Manual, chapter on assignable clauses and datagroups). Every
 * location listed there must be assignable according to Java's rules &mdash;
 * a location that Java would never allow to be assigned makes the frame
 * unsatisfiable and the specification meaningless.
 *
 * ### Checked cases
 *
 * For each expression of an `assignable` (or `assignable_redundantly`)
 * clause:
 *
 *  - **`this`** &mdash; the reference itself can not be reassigned; only its
 *    fields may be listed.
 *
 *  - **Enum constants** &mdash; enum members are final and can never be
 *    assigned.
 *
 *  - **Final fields** &mdash; a `final` field can be assigned exactly once, in
 *    a constructor or initializer, never in the scope of a contract for
 *    arbitrary method executions.
 *
 *  - **Array access to a non-array** &mdash; an indexed store-ref like `a[i]`
 *    is only meaningful if the named expression has an array type.
 *
 *  - **Other expressions** &mdash; any expression of an unsupported form is
 *    reported; an `assignable` clause expects store-ref expressions (names,
 *    field accesses, array accesses, `\nothing`, `\everything`, `.*` and
 *    `[*]` patterns), not arbitrary value expressions.
 *
 * ### Known limitations
 *
 *  - Only `assignable` and `assignable_redundantly` clauses are inspected;
 *    the related frame clauses `modifiable`, `modifies` and `accessible` are
 *    not checked by this rule.
 *  - Only simple names are resolved against declarations; qualified field
 *    accesses (e.g. `obj.field`) currently fall into the "other expressions"
 *    branch.
 *  - A name that cannot be resolved aborts the check of the enclosing clause.
 *
 * ### Examples
 *
 * Good:
 * ```java
 * //@ assignable count, cache[*];
 * public void increment() { ... }
 * ```
 *
 * Bad &mdash; locations that can never be assigned:
 * ```java
 * //@ assignable this;          // error: this reference is not re-assignable
 * //@ assignable RED, GREEN;    // error: enum constants are not re-assignable
 * //@ assignable MAX_SIZE;      // error: this variable is final
 * public void touch() { ... }
 * ```
 *
 * Bad &mdash; array access to a non-array:
 * ```java
 * //@ assignable size[i];       // error: 'size' is not an array
 * public void touch(int i) { ... }
 * ```
 *
 * @author Alexander Weigl
 * @version 1 (12/29/21)
 */
class AssignableValidator : LintRuleVisitor() {
    override val visitor: VoidVisitorAdapter<LintProblemReporter>
        get() = object : VoidVisitorAdapter<LintProblemReporter>() {
            override fun visit(n: JmlMultiExprClause, arg: LintProblemReporter) {
                if (n.kind === JmlClauseKind.ASSIGNABLE ||
                    n.kind === JmlClauseKind.ASSIGNABLE_REDUNDANTLY
                ) {
                    checkFinalFieldsInAssignableClause(n, arg)
                }
            }
        }

    private fun checkFinalFieldsInAssignableClause(n: JmlMultiExprClause, arg: LintProblemReporter) {
        for (e in n.expression) {
            if (e.isNameExpr) {
                if (e.asNameExpr().nameAsString.equals("this")) {
                    arg.error(e, "", "", "This reference is not re-assignable!")
                    continue
                }
                val value = e.asNameExpr().resolve()
                if (value.isEnumConstant) {
                    arg.error(e, "", "", "Enum constants are not re-assignable!")
                } else if (value.isField) {
                    val ast = value.asField().toAst().getOrNull()
                    if (ast is FieldDeclaration && ast.isFinal) {
                        arg.error(e, "", "", "This variable is final, so cannot be assigned")
                    }
                }
            } else if (e.isArrayAccessExpr) {
                // TODO weigl check for array-ness of name expr
                val rtype = e.asArrayAccessExpr().name.calculateResolvedType()
                if (!rtype.isArray) {
                    arg.error(e, "", "", "Array access to non-array. Calculated type is %s", rtype.describe())
                }
            } else {
                arg.error(e, "", "", "Strange expression type found: %s", e.metaModel.typeName)
            }
        }
    }
}

// ---------- 1. Abstract domain ----------

// information order: MAYBE < DEF_EMPTY ,  DEF_NONEMPTY
enum class Emptiness {
    NOTHING,
    MAYBE,
    EVERYTHING
}

/** Control-flow join: a fact survives only if it holds on all paths. */
infix fun Emptiness.intersect(that: Emptiness): Emptiness =
    if (this == Emptiness.MAYBE || that == Emptiness.MAYBE) {
        return Emptiness.MAYBE
    } else if (this == Emptiness.NOTHING && that == Emptiness.NOTHING) {
        return Emptiness.NOTHING
    } else if (this == Emptiness.EVERYTHING && that == Emptiness.EVERYTHING) {
        return Emptiness.EVERYTHING
    } else {
        error("unreachable: invalid Emptiness combination")
    }

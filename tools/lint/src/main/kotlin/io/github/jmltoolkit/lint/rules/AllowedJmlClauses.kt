/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jmlparser.lint.rules

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.jml.NodeWithContracts
import com.github.javaparser.ast.jml.clauses.ContractType
import com.github.javaparser.ast.jml.clauses.JmlClause
import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import com.github.javaparser.ast.jml.clauses.JmlClauseKind.*
import com.github.javaparser.ast.jml.clauses.JmlContract
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRuleVisitor
import java.util.*

/**
 * Checks that only JML clauses appropriate to the kind of contract are used.
 *
 * JML distinguishes several kinds of contracts, each of which admits a specific
 * set of clauses (JML Reference Manual, §3 "Expressions" and §9 "Semantics of
 * Java Methods", clause grammar). Using a clause in a context where it has no
 * meaning is either a specification error or a hint that the wrong contract
 * keyword was used, and it is rejected by most JML tools. This rule reports
 * each misplaced clause as a warning.
 *
 * ## Checked cases
 *
 * The kind of contract is determined from the annotated node (the nearest
 * ancestor carrying contracts) and, for loops, from the contract type:
 *
 *  - **Method contracts** (annotation on a method or constructor declaration):
 *    behavioral clauses such as `requires`, `ensures`, `signals`,
 *    `assignable`, `measured_by`, `diverges`, `duration`, `capturable`,
 *    `callable`, ... Loop-specific clauses like `loop_invariant` or
 *    `maintaining` are not allowed here.
 *
 *  - **Loop invariants** (`loop_invariant` / `decreases` annotation on a
 *    `for`, `foreach`, `while` or `do` statement, of contract type `LOOP_INV`):
 *    only `loop_invariant` (and its `_redundantly` / `_free` variants),
 *    `maintaining`, `decreases`/`decreasing`, and the frame clauses
 *    `modifies`, `modifiable`, `assignable`, `accessible`. In particular, no
 *    `requires`/`ensures` clauses are permitted.
 *
 *  - **Loop contracts** (other annotation on a loop statement, e.g. a
 *    surrounding `requires`/`ensures` block): the union of method clauses and
 *    loop clauses, including heavy-weight clauses (`Old`, `ForAll`,
 *    `working_space`, ...).
 *
 *  - **Block contracts** (annotation on a statement block): effectively the
 *    same set as loop contracts.
 *
 * Contracts attached to other nodes are not checked by this rule.
 *
 * ## Examples
 *
 * Good &mdash; a loop invariant on a `while` loop:
 * ```java
 * //@ loop_invariant 0 <= i && i <= n;
 * //@ decreases n - i;
 * //@ assignable \nothing;
 * while (i < n) { ... }
 * ```
 *
 * Good &mdash; a method contract with `requires`/`ensures`:
 * ```java
 * //@ requires n >= 0;
 * //@ ensures \result >= 0;
 * public int count(int n) { ... }
 * ```
 *
 * Bad &mdash; `requires`/`ensures` inside a `loop_invariant` contract:
 * ```java
 * //@ loop_invariant 0 <= i && i <= n;
 * //@ requires n >= 0;        // error: REQUIRES clause not allowed in a loop_invariant contract
 * //@ ensures \result == n;   // error: ENSURES clause not allowed in a loop_invariant contract
 * while (i < n) { ... }
 * ```
 *
 * Bad &mdash; `loop_invariant` inside a method contract:
 * ```java
 * //@ requires n >= 0;
 * //@ loop_invariant i <= n;  // error: LOOP_INVARIANT clause not allowed in a method contract
 * public int count(int n) { ... }
 * ```
 *
 * @author Alexander Weigl
 * @version 1 (13.10.22)
 */
class AllowedJmlClauses : LintRuleVisitor() {
    private val LOOP_INVARIANT_CLAUSES: EnumSet<JmlClauseKind> = EnumSet.of(
        DECREASES,
        MODIFIES,
        MODIFIABLE,
        ASSIGNABLE,
        ACCESSIBLE,
        MAINTAINING,
        MAINTAINING_REDUNDANTLY,
        DECREASING,
        DECREASES_REDUNDANTLY,
        LOOP_INVARIANT,
        LOOP_INVARIANT_FREE,
        LOOP_INVARIANT_REDUNDANTLY
    )

    private val LOOP_CONTRACT_CLAUSES: EnumSet<JmlClauseKind> = EnumSet.of(
        ENSURES,
        ENSURES_FREE,
        ENSURES_REDUNDANTLY,
        REQUIRES,
        REQUIRES_FREE,
        REQUIRES_REDUNDANTLY,
        DECREASES,
        MODIFIES,
        MODIFIABLE,
        ASSIGNABLE,
        ACCESSIBLE,
        PRE,
        POST,
        PRE_REDUNDANTLY,
        POST_REDUNDANTLY,
        MAINTAINING,
        MAINTAINING_REDUNDANTLY,
        DECREASING,
        DECREASES_REDUNDANTLY,
        LOOP_INVARIANT,
        LOOP_INVARIANT_FREE,
        LOOP_INVARIANT_REDUNDANTLY,
        MEASURED_BY,
        RETURNS,
        RETURNS_REDUNDANTLY,
        BREAKS,
        BREAKS_REDUNDANTLY,
        CONTINUES,
        CONTINUES_REDUNDANTLY,
        OLD,
        FORALL,
        SIGNALS,
        SIGNALS_REDUNDANTLY,
        SIGNALS_ONLY,
        WHEN,
        WORKING_SPACE,
        WORKING_SPACE_REDUNDANTLY,
        CAPTURES,
        CAPTURES_REDUNDANTLY,
        INITIALLY,
        INVARIANT_REDUNDANTLY,
        INVARIANT,
        ASSIGNABLE_REDUNDANTLY,
        MODIFIABLE_REDUNDANTLY,
        MODIFIES_REDUNDANTLY,
        CALLABLE,
        CALLABLE_REDUNDANTLY,
        DIVERGES,
        DIVERGES_REDUNDANTLY,
        DURATION,
        DURATION_REDUNDANTLY
    )

    private val BLOCK_CONTRACT_CLAUSES: EnumSet<JmlClauseKind> = EnumSet.of(
        ENSURES,
        ENSURES_FREE,
        ENSURES_REDUNDANTLY,
        REQUIRES,
        REQUIRES_FREE,
        REQUIRES_REDUNDANTLY,
        DECREASES,
        MODIFIES,
        MODIFIABLE,
        ASSIGNABLE,
        ACCESSIBLE,
        PRE,
        POST,
        PRE_REDUNDANTLY,
        POST_REDUNDANTLY,
        MAINTAINING,
        MAINTAINING_REDUNDANTLY,
        DECREASING,
        DECREASES_REDUNDANTLY,
        LOOP_INVARIANT,
        LOOP_INVARIANT_FREE,
        LOOP_INVARIANT_REDUNDANTLY,
        MEASURED_BY,
        RETURNS,
        RETURNS_REDUNDANTLY,
        BREAKS,
        BREAKS_REDUNDANTLY,
        CONTINUES,
        CONTINUES_REDUNDANTLY,
        OLD,
        FORALL,
        SIGNALS,
        SIGNALS_REDUNDANTLY,
        SIGNALS_ONLY,
        WHEN,
        WORKING_SPACE,
        WORKING_SPACE_REDUNDANTLY,
        CAPTURES,
        CAPTURES_REDUNDANTLY,
        INITIALLY,
        INVARIANT_REDUNDANTLY,
        INVARIANT,
        ASSIGNABLE_REDUNDANTLY,
        MODIFIABLE_REDUNDANTLY,
        MODIFIES_REDUNDANTLY,
        CALLABLE,
        CALLABLE_REDUNDANTLY,
        DIVERGES,
        DIVERGES_REDUNDANTLY,
        DURATION,
        DURATION_REDUNDANTLY
    )

    private val currentlyAllowed: Set<JmlClauseKind> = HashSet<JmlClauseKind>()

    override val visitor: VoidVisitorAdapter<LintProblemReporter>
        get() = object : VoidVisitorAdapter<LintProblemReporter>() {
            override fun visit(n: JmlContract, arg: LintProblemReporter) {
                val a: Optional<NodeWithContracts<*>> = n.findAncestor(NodeWithContracts::class.java)
                if (a.isPresent) {
                    val owner: NodeWithContracts<*> = a.get()
                    if (owner is ForEachStmt ||
                        owner is ForStmt ||
                        owner is WhileStmt ||
                        owner is DoStmt
                    ) {
                        if (n.type === ContractType.LOOP_INV) {
                            checkClauses(
                                arg,
                                n.clauses,
                                LOOP_INVARIANT_CLAUSES,
                                "loop_invariant"
                            )
                        } else {
                            checkClauses(arg, n.clauses, LOOP_CONTRACT_CLAUSES, "loop")
                        }
                    } else if (owner is MethodDeclaration) {
                        checkClauses(arg, n.clauses, METHOD_CONTRACT_CLAUSES, "method")
                    } else if (owner is BlockStmt) {
                        checkClauses(arg, n.clauses, BLOCK_CONTRACT_CLAUSES, "block")
                    }
                }
            }
        }


    private fun checkClauses(
        arg: LintProblemReporter, clauses: NodeList<JmlClause>,
        allowed: EnumSet<JmlClauseKind>, type: String
    ) {
        for (clause in clauses) {
            if (!allowed.contains(clause.kind)) {
                arg.warn(clause, "", "", "%s clause not allowed in a %s contract", clause.kind, type)
            }
        }
    }

    companion object {
        val METHOD_CONTRACT_CLAUSES: EnumSet<JmlClauseKind> = EnumSet.of(
            ENSURES,
            ENSURES_FREE,
            ENSURES_REDUNDANTLY,
            REQUIRES,
            REQUIRES_FREE,
            REQUIRES_REDUNDANTLY,
            DECREASES,
            MODIFIES,
            MODIFIABLE,
            ASSIGNABLE,
            ACCESSIBLE,
            PRE,
            POST,
            PRE_REDUNDANTLY,
            POST_REDUNDANTLY,
            DECREASING,
            DECREASES_REDUNDANTLY,
            MEASURED_BY,
            OLD,
            FORALL,
            SIGNALS,
            SIGNALS_REDUNDANTLY,
            SIGNALS_ONLY,
            WHEN,
            WORKING_SPACE,
            WORKING_SPACE_REDUNDANTLY,
            CAPTURES,
            CAPTURES_REDUNDANTLY,
            ASSIGNABLE_REDUNDANTLY,
            MODIFIABLE_REDUNDANTLY,
            MODIFIES_REDUNDANTLY,
            CALLABLE,
            CALLABLE_REDUNDANTLY,
            DIVERGES,
            DIVERGES_REDUNDANTLY,
            DURATION,
            DURATION_REDUNDANTLY
        )
    }
}

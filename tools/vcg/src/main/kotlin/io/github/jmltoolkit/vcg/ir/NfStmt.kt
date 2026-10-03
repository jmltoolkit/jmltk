/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg.ir

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.stmt.Statement

/**
 * Normal form IR of the statement block to be verified.
 *
 * Every assignment in this IR has the shape
 *   * `variable = <variable> <op> <variable>`
 *   * `variable = <literal>`
 *   * `variable = <methodcall>`
 * The engine ([io.github.jmltoolkit.vcg.Vcg]) realizes this shape by flattening the
 * right-hand side expressions into auxiliary (temporary) constants; the original
 * variable names are kept as base names of the versioned SMT constants.
 */
sealed class NfStmt {
    /** The original AST node this statement originates from (provenance). */
    var origin: Node? = null
}

/** Assignment target: local variable, field of a receiver, or an array cell. */
sealed class NfLocation {
    abstract val key: String
}

data class NfLocal(override val key: String, val declaredType: com.github.javaparser.ast.type.Type?) : NfLocation()

data class NfField(val receiver: String, override val key: String) : NfLocation()

data class NfArray(override val key: String, val index: Expression) : NfLocation()

/** `location = rhs`, where the engine flattens [rhs] into the normal form. */
data class NfAssign(val target: NfLocation, val rhs: Expression) : NfStmt()

/** A standalone (result-ignoring) method call. */
data class NfCall(val call: com.github.javaparser.ast.expr.MethodCallExpr) : NfStmt()

data class NfIf(val cond: Expression, val thenStmts: List<NfStmt>, val elseStmts: List<NfStmt>) : NfStmt()

/**
 * A loop. [loopNode] is the original loop node (`while`, `for`, `do`); [cond] is the
 * (already desugared) loop condition; [body] contains the body including the
 * increment/update statements of a `for` loop.
 */
data class NfLoop(val loopNode: Node, val cond: Expression, val body: List<NfStmt>) : NfStmt()

data class NfReturn(val value: Expression?) : NfStmt()

/** Throw an exception reference expression; sets the abrupt completion flags. */
data class NfThrow(val exception: Expression) : NfStmt()

data class NfBreak(val loopId: Int) : NfStmt()

data class NfContinue(val loopId: Int) : NfStmt()

/** Assume a JML/Java property (part of the context, not checked). */
data class NfAssume(val expr: Expression) : NfStmt()

/** Check a property: emits a verification condition. */
data class NfAssert(val expr: Expression) : NfStmt()

/** Havoc a location (fresh unconstrained constant). */
data class NfHavoc(val location: NfLocation) : NfStmt()

/** Catch clause of a [NfTryCatch]. */
data class NfCatchClause(
    /** the caught type as a string (may be absent for `catch (…)` without a declared type) */
    val type: com.github.javaparser.ast.type.Type?,
    /** name of the bound exception variable */
    val parameter: String,
    val body: List<NfStmt>,
)

/**
 * A `try/catch/finally` block, kept as a single node. The engine lowers it using
 * the abrupt-exception flags (`$exc`, `$excval`): the try body is executed normally;
 * a fresh exception value is bound per catch clause and the branch whose type matches
 * (via the subtype relation) handles the exception. The finally block is executed on
 * every path.
 */
data class NfTryCatch(
    val tryBody: List<NfStmt>,
    val catches: List<NfCatchClause>,
    val finallyBody: List<NfStmt>,
) : NfStmt()

/** A single case group of a [NfSwitch]. */
data class NfSwitchCase(
    /** the case labels (`null` for the default entry). */
    val labels: List<com.github.javaparser.ast.expr.Expression>,
    /** case statements; a `break` here terminates the whole switch. */
    val body: List<NfStmt>,
)

/**
 * A `switch` statement kept as a single node. The engine lowers it to sequential
 * guarded case groups with a fall-through flag (once a case is entered, subsequent
 * cases run until a `break`), matching Java semantics.
 */
data class NfSwitch(
    val selector: com.github.javaparser.ast.expr.Expression,
    val cases: List<NfSwitchCase>,
    val switchId: Int,
) : NfStmt()

fun List<NfStmt>.withOrigin(node: Node): List<NfStmt> {
    this.forEach { it.origin = node }
    return this
}

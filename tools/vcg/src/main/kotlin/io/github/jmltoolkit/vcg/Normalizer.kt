/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.ThisExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.jml.stmt.JmlExpressionStmt
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.BreakStmt
import com.github.javaparser.ast.stmt.ContinueStmt
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.SwitchStmt
import com.github.javaparser.ast.stmt.ThrowStmt
import com.github.javaparser.ast.stmt.TryStmt
import com.github.javaparser.ast.stmt.WhileStmt
import io.github.jmltoolkit.vcg.ir.NfArray
import io.github.jmltoolkit.vcg.ir.NfAssign
import io.github.jmltoolkit.vcg.ir.NfAssert
import io.github.jmltoolkit.vcg.ir.NfAssume
import io.github.jmltoolkit.vcg.ir.NfBlock
import io.github.jmltoolkit.vcg.ir.NfBreak
import io.github.jmltoolkit.vcg.ir.NfCall
import io.github.jmltoolkit.vcg.ir.NfContinue
import io.github.jmltoolkit.vcg.ir.NfField
import io.github.jmltoolkit.vcg.ir.NfHavoc
import io.github.jmltoolkit.vcg.ir.NfIf
import io.github.jmltoolkit.vcg.ir.NfLocal
import io.github.jmltoolkit.vcg.ir.NfLoop
import io.github.jmltoolkit.vcg.ir.NfLocation
import io.github.jmltoolkit.vcg.ir.NfReturn
import io.github.jmltoolkit.vcg.ir.NfStmt
import io.github.jmltoolkit.vcg.ir.NfThrow
import io.github.jmltoolkit.vcg.ir.NfTryCatch
import io.github.jmltoolkit.vcg.ir.NfCatchClause
import io.github.jmltoolkit.vcg.ir.NfSwitch
import io.github.jmltoolkit.vcg.ir.NfSwitchCase
import io.github.jmltoolkit.vcg.ir.withOrigin

/**
 * Transforms a Java statement block into the normal form IR (see [io.github.jmltoolkit.vcg.ir]).
 *
 * The transformation preserves the original semantics:
 *  * compound assignments and increments are rewritten to simple assignments,
 *  * `for` loops are desugared into initialization + `while` shape (contracts stay
 *    attached to the original loop node),
 *  * `do-while` loops are unrolled once,
 *  * `foreach` loops over arrays are desugared into an index loop,
 *  * `switch` is desugared (without fall-through) into an if-chain,
 *  * JML `assert`/`assume` statements are mapped to [NfAssert]/[NfAssume].
 *
 * Original variable names are preserved; the engine only adds version suffixes
 * and keeps a mapping back to the original names.
 */
class Normalizer {
    private var tmpCounter = 0
    private val loopStack = ArrayDeque<Int>()

    fun normalize(block: BlockStmt): List<NfStmt> = normalizeStmt(block)

    fun normalizeStmt(stmt: Statement): List<NfStmt> = when (stmt) {
        is BlockStmt -> stmt.statements.flatMap { normalizeStmt(it) }.withOrigin(stmt)

        is EmptyStmt -> emptyList()

        is ExpressionStmt -> normalizeExprStmt(stmt)

        is IfStmt -> {
            val elseStmts =
                if (stmt.elseStmt.isPresent) normalizeStmt(stmt.elseStmt.get()) else emptyList()
            listOf(NfIf(stmt.condition, normalizeStmt(stmt.thenStmt), elseStmts)).withOrigin(stmt)
        }

        is WhileStmt -> {
            loopStack.addLast(stmt.hashCode())
            val body = normalizeStmt(stmt.body)
            loopStack.removeLast()
            listOf(NfLoop(stmt, stmt.condition, body)).withOrigin(stmt)
        }

        is ForStmt -> normalizeFor(stmt)

        is DoStmt -> normalizeDo(stmt)

        is ForEachStmt -> normalizeForEach(stmt)

        is ReturnStmt -> listOf(NfReturn(stmt.expression.orElse(null))).withOrigin(stmt)

        is ThrowStmt -> listOf(NfThrow(stmt.expression)).withOrigin(stmt)

        is TryStmt -> normalizeTry(stmt)

        is BreakStmt -> listOf(NfBreak(loopStack.lastOrNull() ?: -1)).withOrigin(stmt)

        is ContinueStmt -> listOf(NfContinue(loopStack.lastOrNull() ?: -1)).withOrigin(stmt)

        is SwitchStmt -> normalizeSwitch(stmt)

        is JmlExpressionStmt -> normalizeJmlStmt(stmt)

        else -> throw UnsupportedOperationException(
            "Statement kind not supported by the normal form: ${stmt.javaClass.simpleName}"
        )
    }

    private fun normalizeTry(stmt: TryStmt): List<NfStmt> {
        val tryBody = normalizeStmt(stmt.tryBlock)
        val catches = stmt.catchClauses.map { c ->
            val param = c.parameter
            // the catch type may be a union `A | B`; the engine handles each via a
            // fresh sort but for v2 we pass the first (or the union) type text.
            NfCatchClause(param.type, param.nameAsString, normalizeStmt(c.body))
        }
        val finallyBody = stmt.finallyBlock.map { normalizeStmt(it) }.orElse(emptyList())
        return listOf(NfTryCatch(tryBody, catches, finallyBody)).withOrigin(stmt)
    }

    private fun normalizeJmlStmt(stmt: JmlExpressionStmt): List<NfStmt> =
        when (stmt.kind) {
            JmlExpressionStmt.JmlStmtKind.ASSERT,
            JmlExpressionStmt.JmlStmtKind.ASSERT_REDUNDANTLY,
            -> listOf(NfAssert(stmt.expression))

            JmlExpressionStmt.JmlStmtKind.ASSUME,
            JmlExpressionStmt.JmlStmtKind.ASSUME_REDUNDANTLY,
            -> listOf(NfAssume(stmt.expression))

            else -> emptyList()
        }.withOrigin(stmt)

    private fun normalizeExprStmt(stmt: ExpressionStmt): List<NfStmt> {
        val e = stmt.expression
        val res = when {
            e is AssignExpr -> listOf(NfAssign(target(e.target), normalizeAssignValue(e)))

            e is UnaryExpr && e.operator == UnaryExpr.Operator.POSTFIX_INCREMENT -> incDec(e.expression, +1)

            e is UnaryExpr && e.operator == UnaryExpr.Operator.POSTFIX_DECREMENT -> incDec(e.expression, -1)

            e is UnaryExpr && e.operator == UnaryExpr.Operator.PREFIX_INCREMENT -> incDec(e.expression, +1)

            e is UnaryExpr && e.operator == UnaryExpr.Operator.PREFIX_DECREMENT -> incDec(e.expression, -1)

            e is VariableDeclarationExpr -> e.variables.flatMap { normalizeDeclarator(it) }

            e is MethodCallExpr -> listOf(NfCall(e))

            else -> throw UnsupportedOperationException(
                "Expression statement not supported: ${e.javaClass.simpleName}"
            )
        }
        return res.withOrigin(stmt)
    }

    private fun incDec(target: Expression, delta: Int): List<NfStmt> {
        val one = IntegerLiteralExpr(Math.abs(delta).toString())
        val op = if (delta > 0) BinaryExpr.Operator.PLUS else BinaryExpr.Operator.MINUS
        return listOf(NfAssign(target(target), BinaryExpr(target.clone(), one, op)))
    }

    private fun normalizeDeclarator(v: VariableDeclarator): List<NfStmt> =
        if (v.initializer.isPresent) {
            listOf(NfAssign(NfLocal(v.nameAsString, v.type), v.initializer.get().clone()))
        } else {
            listOf(NfHavoc(NfLocal(v.nameAsString, v.type)))
        }

    /** `x op= e` becomes `x = x op e`. */
    private fun normalizeAssignValue(e: AssignExpr): Expression {
        val value = e.value.clone()
        if (e.operator == AssignExpr.Operator.ASSIGN) return value
        val op = when (e.operator) {
            AssignExpr.Operator.PLUS -> BinaryExpr.Operator.PLUS
            AssignExpr.Operator.MINUS -> BinaryExpr.Operator.MINUS
            AssignExpr.Operator.MULTIPLY -> BinaryExpr.Operator.MULTIPLY
            AssignExpr.Operator.DIVIDE -> BinaryExpr.Operator.DIVIDE
            AssignExpr.Operator.REMAINDER -> BinaryExpr.Operator.REMAINDER
            AssignExpr.Operator.BINARY_AND -> BinaryExpr.Operator.BINARY_AND
            AssignExpr.Operator.BINARY_OR -> BinaryExpr.Operator.BINARY_OR
            AssignExpr.Operator.XOR -> BinaryExpr.Operator.XOR
            AssignExpr.Operator.LEFT_SHIFT -> BinaryExpr.Operator.LEFT_SHIFT
            AssignExpr.Operator.SIGNED_RIGHT_SHIFT -> BinaryExpr.Operator.SIGNED_RIGHT_SHIFT
            AssignExpr.Operator.UNSIGNED_RIGHT_SHIFT -> BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT
            else -> throw UnsupportedOperationException("${e.operator}")
        }
        return BinaryExpr(e.target.clone(), value, op)
    }

    private fun target(t: Expression): NfLocation = when (t) {
        is NameExpr -> NfLocal(t.nameAsString, null)
        is FieldAccessExpr -> NfField(t.scope.toString(), key(t))
        is ArrayAccessExpr -> NfArray(key(t.name), t.index.clone())
        else -> throw UnsupportedOperationException("Assignment target: ${t.javaClass.simpleName}")
    }

    /** Stable key for a location, used to identify heap locations across versions. */
    fun key(e: Expression): String = when (e) {
        is NameExpr -> e.nameAsString

        is FieldAccessExpr ->
            if (e.scope is ThisExpr || e.scope.toString() == "this") {
                "this." + e.nameAsString
            } else {
                e.toString()
            }

        is ThisExpr -> "this"

        else -> e.toString()
    }

    private fun normalizeFor(stmt: ForStmt): List<NfStmt> {
        val res = mutableListOf<NfStmt>()
        for (init in stmt.initialization) {
            res.addAll(normalizeStmt(StaticJavaParser.parseStatement("$init;")))
        }
        val cond = stmt.compare.orElse(StaticJavaParser.parseExpression("true"))
        loopStack.addLast(stmt.hashCode())
        val body = mutableListOf<NfStmt>()
        body.addAll(normalizeStmt(stmt.body))
        for (update in stmt.update) {
            body.addAll(normalizeStmt(StaticJavaParser.parseStatement("$update;")))
        }
        loopStack.removeLast()
        res.add(NfLoop(stmt, cond, body))
        return res.withOrigin(stmt)
    }

    private fun normalizeDo(stmt: DoStmt): List<NfStmt> {
        // do B while (c);  ~~>  B; while (c) { B }
        loopStack.addLast(stmt.hashCode())
        val first = normalizeStmt(stmt.body)
        val second = normalizeStmt(stmt.body.clone())
        loopStack.removeLast()
        return (first + NfLoop(stmt, stmt.condition, second)).withOrigin(stmt)
    }

    private fun normalizeForEach(s: ForEachStmt): List<NfStmt> {
        // for (T x : arr) BODY  ~~>
        //   int $i = 0; while ($i < arr.length) { T x = arr[$i]; BODY; $i = $i + 1; }
        val iterKey = key(s.iterable as Expression)
        val i = "\$idx${tmpCounter++}"
        val v = s.variable.variables[0]
        val varName = v.nameAsString
        loopStack.addLast(s.hashCode())
        val body = normalizeStmt(s.body)
        loopStack.removeLast()
        val cond = BinaryExpr(
            NameExpr(i),
            FieldAccessExpr(NameExpr(iterKey), "length"),
            BinaryExpr.Operator.LESS
        )
        val loopBody = listOf<NfStmt>(
            NfAssign(
                NfLocal(varName, v.type),
                ArrayAccessExpr(NameExpr(iterKey), NameExpr(i))
            ).apply { origin = s }
        ) + body + listOf(
            NfAssign(
                NfLocal(i, null),
                BinaryExpr(NameExpr(i), IntegerLiteralExpr("1"), BinaryExpr.Operator.PLUS)
            ).apply { origin = s }
        )
        val res = mutableListOf<NfStmt>()
        res.add(NfAssign(NfLocal(i, null), IntegerLiteralExpr("0")).apply { origin = s })
        res.add(NfLoop(s, cond, loopBody).apply { origin = s })
        return res
    }

    private fun normalizeSwitch(s: SwitchStmt): List<NfStmt> {
        // Preserve Java fall-through: the engine lowers the case groups as sequential
        // guarded groups with a `started` flag; a `break` inside a case terminates the
        // whole switch and is encoded as a switch-scoped break (NfBreak(switchId)).
        val switchId = s.hashCode()
        val cases = mutableListOf<NfSwitchCase>()
        for (entry in s.entries) {
            loopStack.addLast(switchId) // so case `break` targets the switch
            val body = entry.statements.flatMap { normalizeStmt(it) }
            loopStack.removeLast()
            cases.add(NfSwitchCase(entry.labels.toList(), body))
        }
        return listOf(NfSwitch(s.selector.clone(), cases, switchId)).withOrigin(s)
    }
}

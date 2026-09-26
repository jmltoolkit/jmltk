/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.ArrayCreationExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.CharLiteralExpr
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.InstanceOfExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.NullLiteralExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.expr.ThisExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.jml.expr.JmlBinaryInfixExpr
import com.github.javaparser.ast.jml.expr.JmlMultiCompareExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr.JmlDefaultBinder
import com.github.javaparser.ast.visitor.GenericVisitorAdapter
import io.github.jmltoolkit.utils.JMLUtils
import io.github.jmltoolkit.smt.ArithmeticTranslator
import io.github.jmltoolkit.smt.SmtQuery
import io.github.jmltoolkit.smt.SmtTermFactory
import io.github.jmltoolkit.smt.SmtTermFactory.equality
import io.github.jmltoolkit.smt.model.SAtom
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.model.SList
import io.github.jmltoolkit.smt.model.SmtType

/**
 * Translates (JML) expressions to SMT against a given symbol table (SSA versions).
 * Unlike [io.github.jmltoolkit.smt.JmlExpr2Smt], no symbol resolution is needed:
 * the mapping from names to current versions is supplied by the engine.
 *
 * Special names recognized: `\result`, and `old(...)` / `\old(...)` calls.
 */
class ExprTranslator(
    private val smtLog: SmtQuery,
    val translator: ArithmeticTranslator,
    /** current version of each location */
    val env: Map<String, SExpr>,
    /** pre-state versions (entry of the method) used by `\old` */
    val oldEnv: Map<String, SExpr> = env,
    /** resolves and executes a method call; returns the result constant */
    val callHandler: (MethodCallExpr) -> SExpr,
    /** declares an unknown location (field not tracked yet) */
    val unknownHandler: (NameExpr) -> SExpr,
    /** resolves a field access `o.f` for an arbitrary receiver, given the receiver
     *  value, the field name, and the receiver's source text; returns null to fall
     *  back to the flat {@code this.field} constant handling. */
    val fieldAccessHandler: (receiver: SExpr, field: String, scopeText: String) -> SExpr? = { _, _, _ -> null },
) : GenericVisitorAdapter<SExpr, Any?>() {
    private val term = SmtTermFactory

    fun tr(e: Expression): SExpr = e.accept(this, null)!!

    override fun visit(n: BinaryExpr, arg: Any?): SExpr {
        val left = n.left.accept(this, arg)!!
        val right = n.right.accept(this, arg)!!
        // a reference comparison against an array is a sort mismatch: arrays (value
        // maps) are never null, so `arr == null` is false and `arr != null` is true.
        if (n.operator == BinaryExpr.Operator.EQUALS || n.operator == BinaryExpr.Operator.NOT_EQUALS) {
            val arrSide =
                if (left.smtType is SmtType.Array) {
                    left
                } else if (right.smtType is SmtType.Array) {
                    right
                } else {
                    null
                }
            val nullSide =
                if (isNull(left)) {
                    left
                } else if (isNull(right)) {
                    right
                } else {
                    null
                }
            if (arrSide != null && nullSide != null) {
                return if (n.operator == BinaryExpr.Operator.EQUALS) term.makeFalse() else term.makeTrue()
            }
        }
        return translator.binary(n.operator, left, right)
    }

    private fun isNull(x: SExpr): Boolean = (x as? SAtom)?.value == "null"

    override fun visit(n: EnclosedExpr, arg: Any?): SExpr = n.inner.accept(this, arg)!!

    override fun visit(n: CastExpr, arg: Any?): SExpr {
        val inner = n.expression.accept(this, arg)!!
        val t = n.type
        return if (t.isPrimitiveType || t is com.github.javaparser.ast.type.PrimitiveType) {
            inner // numeric casts are elided (see v1, mode-dependent width handling)
        } else {
            // reference cast: `(cast value sort_C)`
            term.list(
                null, SmtType.JAVA_OBJECT,
                term.symbol("cast"), inner,
                term.symbol(sortName(t.toString()))
            )
        }
    }

    override fun visit(n: InstanceOfExpr, arg: Any?): SExpr {
        val value = n.expression.accept(this, arg)!!
        return term.list(
            null, SmtType.BOOL,
            term.symbol("instanceof"), value,
            term.symbol(sortName(n.type.toString()))
        )
    }

    private fun sortName(t: String): String =
        "sort_" + qualify(t).replace('.', '_').lowercase()

    /** Resolves unqualified `java.lang.*` exception names to their qualified form. */
    private fun qualify(t: String): String =
        if (t.contains('.')) {
            t
        } else {
            when (t) {
            "Exception", "RuntimeException", "ArithmeticException", "NullPointerException",
            "ArrayIndexOutOfBoundsException", "Object", "Throwable",
            -> "java.lang.$t"

            else -> t
        }
        }

    override fun visit(n: UnaryExpr, arg: Any?): SExpr =
        translator.unary(n.operator, n.expression.accept(this, arg)!!)

    override fun visit(n: BooleanLiteralExpr, arg: Any?): SExpr = translator.makeBoolean(n.value)

    override fun visit(n: IntegerLiteralExpr, arg: Any?): SExpr = translator.makeInt(n)

    override fun visit(n: LongLiteralExpr, arg: Any?): SExpr = translator.makeLong(n)

    override fun visit(n: CharLiteralExpr, arg: Any?): SExpr = translator.makeChar(n)

    override fun visit(n: StringLiteralExpr, arg: Any?): SExpr = term.symbol(n.value)

    override fun visit(n: NullLiteralExpr, arg: Any?): SExpr = term.makeNull()

    override fun visit(n: ThisExpr, arg: Any?): SExpr = term.makeThis()

    override fun visit(n: ConditionalExpr, arg: Any?): SExpr {
        val c = n.condition.accept(this, arg)!!
        val t = n.thenExpr.accept(this, arg)!!
        val el = n.elseExpr.accept(this, arg)!!
        return SList(t.smtType ?: el.smtType, t.javaType, listOf(term.symbol("ite"), c, t, el))
    }

    override fun visit(n: JmlBinaryInfixExpr, arg: Any?): SExpr {
        val left = n.left.accept(this, arg)!!
        val right = n.right.accept(this, arg)!!
        return term.list(null, SmtType.BOOL, term.symbol(n.operator.identifier), left, right)
    }

    override fun visit(n: JmlMultiCompareExpr, arg: Any?): SExpr = tr(JMLUtils.unroll(n))

    override fun visit(n: JmlQuantifiedExpr, arg: Any?): SExpr {
        // binders are built without symbol resolution (contracts may be detached from the CU)
        val binders = n.variables.map { v ->
            term.binder(binderType(v.type), v.nameAsString)
        }
        // bind bound variables in the environment for the body translation
        val bound = HashMap(env)
        for (v in n.variables) {
            val sType = binderType(v.type)
            bound[v.nameAsString] = term.variable(sType, null, v.nameAsString)
        }
        val inner = ExprTranslator(smtLog, translator, bound, oldEnv, callHandler, unknownHandler)
        val conjuncts = { exprs: List<Expression> ->
            exprs.fold(term.makeTrue() as SExpr) { acc, e -> term.and(acc, e.accept(inner, arg)!!) }
        }
        // JML `(\forall T x; range; body)`: the range *guards* the body.
        // Encoding it as a conjunction would make every guarded quantifier
        // unsatisfiable (∀x. range ∧ body is false outside the range), turning
        // quantified preconditions into contradictions that vacuously prove
        // everything. `(\exists …)` genuinely conjoins range and body.
        return if (n.binder == JmlDefaultBinder.EXISTS) {
            term.exists(binders, conjuncts(n.expressions))
        } else if (n.expressions.size <= 1) {
            term.forall(binders, conjuncts(n.expressions))
        } else {
            val range = n.expressions[0].accept(inner, arg)!!
            term.forall(binders, term.impl(range, conjuncts(n.expressions.drop(1))))
        }
    }

    private fun binderType(t: com.github.javaparser.ast.type.Type): SmtType = try {
        when (t) {
            is com.github.javaparser.ast.type.PrimitiveType ->
                translator.getType(
                    com.github.javaparser.resolution.types.ResolvedPrimitiveType.byName(t.type.name)
                )

            is com.github.javaparser.ast.type.ArrayType ->
                SmtType.Array(SmtType.INT, binderType(t.elementType))

            else -> SmtType.INT
        }
    } catch (e: Exception) {
        SmtType.INT
    }

    override fun visit(n: NameExpr, arg: Any?): SExpr {
        val name = n.nameAsString
        if (name == "\\result") {
            return env[RESULT] ?: term.symbol(RESULT)
        }
        env[name]?.let { return it }
        // bare field access of the enclosing class
        env["this.$name"]?.let { return it }
        oldEnv[name]?.let { return it }
        return unknownHandler(n)
    }

    override fun visit(n: FieldAccessExpr, arg: Any?): SExpr {
        val scope = n.scope.accept(this, arg)!!
        if (n.nameAsString == "length") {
            return translator.arrayLength(scope)
        }
        // arbitrary receiver `o.f`: delegate to the heap-selector model
        fieldAccessHandler(scope, n.nameAsString, n.scope.toString())?.let { return it }
        env[n.toString()]?.let { return it }
        if (n.scope is ThisExpr) {
            env["this.${n.nameAsString}"]?.let { return it }
        }
        return term.fieldAccess(null, SmtType.JAVA_OBJECT, n.nameAsString, scope)
    }

    override fun visit(n: ArrayAccessExpr, arg: Any?): SExpr {
        val array = n.name.accept(this, arg)!!
        val index = n.index.accept(this, arg)!!
        // The element sort must come from the array's declared sort: bit-vector
        // element in BOUNDED mode, `Int` in UNBOUNDED. Hardcoding `Int` would make
        // bounded-mode array reads ill-sorted against BV32 literals and arrays.
        val elementType = (array.smtType as? SmtType.Array)?.to ?: SmtType.INT
        return term.select(elementType, null, array, index)
    }

    override fun visit(n: ArrayCreationExpr, arg: Any?): SExpr {
        val name = "anon_array_${anonCnt++}"
        val type = translator.getType(n.calculateResolvedType())
        smtLog.declareConst(name, type)
        val v = term.variable(type, n.calculateResolvedType(), name)
        if (n.levels.isNotEmpty() && n.levels[0].dimension.isPresent) {
            val len = n.levels[0].dimension.get().accept(this, arg)!!
            smtLog.addAssert(equality(translator.arrayLength(v), len))
        }
        return v
    }

    override fun visit(n: MethodCallExpr, arg: Any?): SExpr {
        // \old(x) (JML keyword spelling) or old(x): refer to the pre-state environment.
        // The JML keyword `\old` is lexed as the method name `\old`, whereas a literal
        // `old(...)` source call keeps the bare name; both must be recognized.
        if ((n.nameAsString == "old" || n.nameAsString == "\\old") && n.arguments.size == 1) {
            val inner = ExprTranslator(smtLog, translator, oldEnv, oldEnv, callHandler, unknownHandler)
            return n.arguments[0].accept(inner, arg)!!
        }
        return callHandler(n)
    }

    companion object {
        const val RESULT = "\$result"
        const val RET = "\$ret"
        const val BREAK = "\$break_"
        const val CONTINUE = "\$continue_"
        /** thrown flag (abrupt exceptional completion) */
        const val EXC = "\$exc"
        /** the thrown exception object */
        const val EXCVAL = "\$excval"
        private var anonCnt = 0
    }
}

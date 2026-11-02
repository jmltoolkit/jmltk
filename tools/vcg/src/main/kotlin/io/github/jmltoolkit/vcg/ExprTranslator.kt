/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.Range
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.ArrayCreationExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.CharLiteralExpr
import com.github.javaparser.ast.expr.ClassExpr
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
import com.github.javaparser.ast.jml.expr.JmlLabelExpr
import com.github.javaparser.ast.jml.expr.JmlMultiCompareExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr.JmlDefaultBinder
import com.github.javaparser.ast.visitor.GenericVisitorAdapter
import com.github.javaparser.resolution.types.ResolvedType
import io.github.jmltoolkit.utils.JMLUtils
import io.github.jmltoolkit.smt.ArithmeticTranslator
import io.github.jmltoolkit.smt.IntArithmeticTranslator
import io.github.jmltoolkit.smt.SmtQuery
import io.github.jmltoolkit.smt.SmtTermFactory
import io.github.jmltoolkit.smt.SmtTermFactory.equality
import io.github.jmltoolkit.smt.model.SAtom
import java.util.HashSet
import java.util.concurrent.atomic.AtomicInteger
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
    /** resolves a read of a `this`-receiver field against an *explicit* environment
     *  map. Unlike [fieldAccessHandler] the map is passed in so that `\old(...)`
     *  (which translates against the pre-state map) resolves correctly; returns null
     *  to fall back to the flat {@code this.field} constant handling. */
    val thisFieldRead: (env: Map<String, SExpr>, thisVal: SExpr, field: String) -> SExpr? = { _, _, _ -> null },
    /** maps an array value to its reference identity (a `U` constant); `null` when
     *  the operand is not an array. Used by `==`/`!=` on arrays, which compare
     *  identities (Java semantics) rather than extensional contents. */
    val arrayRefHandler: (SExpr) -> SExpr? = { null },
    /** Decides whether an array-typed source expression may denote `null` (P4.2):
     *  arrays are ordinary references — nullable by default, never-null only for
     *  `new T[...]` results and `non_null`-declared locations. When it answers
     *  false, `arr == null` / `arr != null` fold to constant false/true. */
    val arrayMayBeNull: (Expression) -> Boolean = { true },
    /** Maps an array term to the per-sort `$null_array_<sort>` constant (declared
     *  lazily in the query); `null` falls back to the legacy never-null folding. */
    val arrayNullTerm: (SExpr) -> SExpr? = { null },
    /** Length term of an array value: `bv$length`/`int$length` for int arrays, the
     *  per-sort `_<tag>` variant for other element sorts (whose signature differs
     *  and cannot reuse the int-array function). */
    val arrayLengthHandler: (SExpr) -> SExpr = { arr -> translator.arrayLength(arr) },
    /** expands a `\invariant_for(..)` / `\static_invariant_for(..)` expression:
     *  given the translated receiver term (null for type-level arguments such as a
     *  `ClassExpr`), the receiver's source expression, and the static flag, returns
     *  the folded class-invariant term; `null` falls back to ordinary method-call
     *  handling. */
    val invariantForHandler: (receiver: SExpr?, receiverExpr: Expression, isStatic: Boolean) -> SExpr? = { _, _, _ -> null },
    /** Counter for fresh `anon_array_` constants. Names only need to be unique
     *  within one generated query, so a *per-verification* counter (starting at 0)
     *  makes queries reproducible; the VCG passes one instance counter to all of a
     *  verification's translators (the main one and its `\old`/quantifier inner
     *  ones) so anonymous array constants never collide within the query text. */
    val anonArrayCounter: AtomicInteger = anonCnt,
    /** Shared, per-query state for the JML `\sum` model: the recursive `jmlSum`
     *  function and its recurrence axiom are declared exactly once per query
     *  (keyed by the array sort of the first use). All translators of one
     *  verification share a single instance so a later clause reuses the
     *  declaration instead of redeclaring `jmlSum`. */
    val sumState: JmlSumState = JmlSumState(),
) : GenericVisitorAdapter<SExpr, Any?>() {
    private val term = SmtTermFactory

    fun tr(e: Expression): SExpr = e.accept(this, null)!!

    override fun visit(n: BinaryExpr, arg: Any?): SExpr {
        // UNBOUNDED mode models int/long as mathematical integers, which have no
        // bit width: shifts and bitwise operators cannot be encoded. This is the
        // *expression* path (contract clauses, JML assert/assume statements); the
        // runtime path (`atom`) carries the same guard — both reject loudly with a
        // structured diagnostic instead of a bare sort mismatch.
        if (translator is IntArithmeticTranslator) {
            rejectBitWidthOp(n.operator, n.range.orElse(null))
        }
        val left = n.left.accept(this, arg)!!
        val right = n.right.accept(this, arg)!!
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
                // P4.2: arrays are ordinary references — `arr == null` compares
                // against the per-sort null-array value (arrays are nullable by
                // default; only `non_null`-declared locations and `new T[...]`
                // results keep the constant false/true folding).
                val arrAst = if (arrSide === left) n.left else n.right
                val nullArr = arrayNullTerm(arrSide)
                if (nullArr != null && arrayMayBeNull(arrAst)) {
                    return translator.binary(n.operator, arrSide, nullArr)
                }
                return if (n.operator == BinaryExpr.Operator.EQUALS) term.makeFalse() else term.makeTrue()
            }
            // `==`/`!=` on arrays is reference equality: compare identities.
            if (left.smtType is SmtType.Array && right.smtType is SmtType.Array) {
                val lr = arrayRefHandler(left)
                val rr = arrayRefHandler(right)
                if (lr != null && rr != null) {
                    return translator.binary(n.operator, lr, rr)
                }
            }
        }
        return translator.binary(n.operator, left, right)
    }

    private fun isNull(x: SExpr): Boolean = (x as? SAtom)?.value == "null"

    /** Structured P3.2 rejection for bit-width operators in UNBOUNDED mode. */
    private fun rejectBitWidthOp(op: BinaryExpr.Operator, range: com.github.javaparser.Range?) {
        val kind = when (op) {
            BinaryExpr.Operator.LEFT_SHIFT,
            BinaryExpr.Operator.SIGNED_RIGHT_SHIFT,
            BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT,
            -> "bit-shift operators"

            BinaryExpr.Operator.BINARY_AND,
            BinaryExpr.Operator.BINARY_OR,
            BinaryExpr.Operator.XOR,
            -> "bitwise operators"

            else -> null
        }
        if (kind != null) {
            throw UnsupportedFeatureException(
                "unsupported: $kind in UNBOUNDED mode (mathematical integers have no bit width); " +
                    "use BOUNDED (bit-vector) mode",
                range
            )
        }
    }

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

    /** Resolves unqualified `java.lang.*` names to their qualified form. */
    private fun qualify(t: String): String =
        if (t.contains('.')) {
            t
        } else {
            when (t) {
            "Exception", "RuntimeException", "ArithmeticException", "NullPointerException",
            "ArrayIndexOutOfBoundsException", "Object", "Throwable", "String",
            -> "java.lang.$t"

            else -> t
        }
        }

    override fun visit(n: UnaryExpr, arg: Any?): SExpr {
        // `~x` on a mathematical integer cannot be encoded (mirror of the binary
        // bitwise rejection above)
        if (translator is IntArithmeticTranslator && n.operator == UnaryExpr.Operator.BITWISE_COMPLEMENT) {
            throw UnsupportedFeatureException(
                "unsupported: bitwise operators in UNBOUNDED mode (mathematical integers have no bit width); " +
                    "use BOUNDED (bit-vector) mode",
                n.range.orElse(null)
            )
        }
        return translator.unary(n.operator, n.expression.accept(this, arg)!!)
    }

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
        if (n.binder == JmlDefaultBinder.SUM) {
            return translateSum(n, arg)
        }
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
        val inner = ExprTranslator(
            smtLog, translator, bound, oldEnv, callHandler, unknownHandler,
            fieldAccessHandler = fieldAccessHandler, arrayRefHandler = arrayRefHandler,
            arrayMayBeNull = arrayMayBeNull, arrayNullTerm = arrayNullTerm,
            arrayLengthHandler = arrayLengthHandler, invariantForHandler = invariantForHandler,
            anonArrayCounter = anonArrayCounter, sumState = sumState
        )
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

    //region JML `\sum` model (P6.1)

    /** Shape of a supported `\sum T j; <lo <= j && j < hi>; <a[j]>`. */
    private class SumShape(val lo: Expression, val hi: Expression, val array: Expression)

    /**
     * Translates a JML `\sum` as a bounded integer-range sum: `\sum T j; lo <= j
     * && j < hi; a[j]` becomes the application `(jmlSum a lo hi)` of a recursive
     * summation function (see [ensureSumFunction]). The range bounds and the
     * array expression are translated against the *outer* environment — only the
     * shape is validated against the bound variable — so symbolic ranges over
     * `a.length` or the KeY `\index` are modeled faithfully instead of being
     * unrolled statically. Any other shape (multi-variable sums, missing range,
     * non-array terms, …) is rejected with a structured [UnsupportedFeatureException]
     * rather than silently mis-encoded (P3.2).
     */
    private fun translateSum(n: JmlQuantifiedExpr, arg: Any?): SExpr {
        val shape = sumShape(n)
        val arr = shape.array.accept(this, arg)!!
        val lo = shape.lo.accept(this, arg)!!
        val hi = shape.hi.accept(this, arg)!!
        val arrSort = arr.smtType as? SmtType.Array
            ?: throw UnsupportedFeatureException(
                "unsupported: \\sum term must index an array (saw ${arr.smtType})",
                n.range.orElse(null)
            )
        val fnName = sumFunctionName(arrSort)
        ensureSumFunction(fnName, arrSort)
        return term.list(null, arrSort.to, term.symbol(fnName), arr, lo, hi)
    }

    private fun sumShape(n: JmlQuantifiedExpr): SumShape {
        val range = n.range.orElse(null)
        if (n.variables.size != 1) {
            throw UnsupportedFeatureException(
                "unsupported: \\sum must bind exactly one variable (only univariate sums are modeled)",
                range
            )
        }
        if (n.expressions.size != 2) {
            throw UnsupportedFeatureException(
                "unsupported: \\sum must have exactly a range and a term" +
                    " (`\\sum <type> v; <lo <= v && v < hi>; <a[v]>`)",
                range
            )
        }
        val j = n.variables[0].nameAsString
        var lo: Expression? = null
        var hi: Expression? = null
        for (c in flattenAnd(n.expressions[0])) {
            val uc = unwrap(c)
            when {
                // j <= hi
                uc is BinaryExpr && uc.operator == BinaryExpr.Operator.LESS_EQUALS && isName(uc.left, j) ->
                    hi = single(hi, uc.right, j, range, "upper")

                // lo <= j
                uc is BinaryExpr && uc.operator == BinaryExpr.Operator.LESS_EQUALS && isName(uc.right, j) ->
                    lo = single(lo, uc.left, j, range, "lower")

                // j >= lo
                uc is BinaryExpr && uc.operator == BinaryExpr.Operator.GREATER_EQUALS && isName(uc.left, j) ->
                    lo = single(lo, uc.right, j, range, "lower")

                // j > lo
                uc is BinaryExpr && uc.operator == BinaryExpr.Operator.GREATER && isName(uc.left, j) ->
                    lo = single(lo, uc.right, j, range, "lower")

                // j < hi
                uc is BinaryExpr && uc.operator == BinaryExpr.Operator.LESS && isName(uc.left, j) ->
                    hi = single(hi, uc.right, j, range, "upper")

                // hi > j
                uc is BinaryExpr && uc.operator == BinaryExpr.Operator.LESS && isName(uc.right, j) ->
                    hi = single(hi, uc.left, j, range, "upper")

                else -> throw UnsupportedFeatureException(
                    "unsupported: \\sum range conjunct `$c` does not bound $j" +
                        " (supported shapes: lo <= v, v >= lo, v < hi, v <= hi)",
                    c.range.orElse(range)
                )
            }
        }
        if (lo == null || hi == null) {
            throw UnsupportedFeatureException(
                "unsupported: \\sum range must bound $j both below and above" +
                    " (supported shapes: `lo <= v && v < hi`)",
                range
            )
        }
        if (mentions(lo, j) || mentions(hi, j)) {
            throw UnsupportedFeatureException(
                "unsupported: \\sum range bounds must not reference the bound variable $j",
                range
            )
        }
        val body = n.expressions[1]
        if (body !is ArrayAccessExpr || !isName(body.index, j) || mentions(body.name, j)) {
            throw UnsupportedFeatureException(
                "unsupported: \\sum term must be a direct array read `a[$j]`" +
                    " over an array expression free of $j",
                body.range.orElse(range)
            )
        }
        return SumShape(lo, hi, body.name)
    }

    private fun single(prev: Expression?, e: Expression, j: String, range: Range?, bound: String): Expression {
        if (prev != null) {
            throw UnsupportedFeatureException(
                "unsupported: \\sum range constrains $j by two $bound bounds",
                e.range.orElse(range)
            )
        }
        return e
    }

    /** Flattens a (possibly parenthesized/nested) `&&` conjunction into its conjuncts. */
    private fun flattenAnd(e: Expression): List<Expression> {
        val u = unwrap(e)
        return if (u is BinaryExpr && u.operator == BinaryExpr.Operator.AND) {
            flattenAnd(u.left) + flattenAnd(u.right)
        } else {
            listOf(e)
        }
    }

    private fun unwrap(e: Expression): Expression = when (e) {
        is EnclosedExpr -> unwrap(e.inner)
        else -> e
    }

    private fun isName(e: Expression, name: String): Boolean {
        val u = unwrap(e)
        return u is NameExpr && u.nameAsString == name
    }

    /** Whether the expression mentions [name] anywhere below it (bound-variable escape check). */
    private fun mentions(e: Expression, name: String): Boolean =
        e.findAll(NameExpr::class.java).any { it.nameAsString == name }

    /** The `jmlSum` function name for an array sort: the plain name for the common
     *  int-array sort (index and element sorts coincide), a deterministic
     *  sort-suffixed variant otherwise — stable so committed goldens hold. */
    private fun sumFunctionName(arrSort: SmtType.Array): String =
        if (arrSort.from == arrSort.to) "jmlSum" else "jmlSum\$${arrSort.from}_${arrSort.to}"

    /**
     * Declares the recursive `jmlSum` function and its recurrence axiom once per
     * query (per array sort). The definition mirrors the JML semantics of a
     * bounded integer-range sum (Z3 4.16-proven in both modes):
     *
     * ```
     * (define-fun-rec jmlSum ((a (Array I E)) (lo I) (hi I)) E
     *   (ite (<= hi lo) 0 (+ (jmlSum a lo (- hi 1)) (select a (- hi 1)))))
     * (assert (forall ((a (Array I E)) (lo I) (hi I))
     *   (! (= (jmlSum a lo (+ hi 1)) (+ (jmlSum a lo hi) (select a hi)))
     *      :pattern ((jmlSum a lo (+ hi 1))))))
     * ```
     *
     * with `I`/`E` the mode's index/element sorts (`Int` in UNBOUNDED, `(_ BitVec 32)`
     * in BOUNDED), so `\sum` works over symbolic ranges (`a.length`, the KeY
     * `\index`) in both modes. The `+1` E-matching trigger lets Z3 drive the
     * SMT induction that discharges the invariant-preservation obligations;
     * without the trigger the recurrence is inert (the model-based prover would
     * not instantiate it).
     */
    private fun ensureSumFunction(fnName: String, arrSort: SmtType.Array) {
        if (sumState.contains(fnName)) return
        sumState.declare(fnName)
        val idxSort = arrSort.from
        val elemSort = arrSort.to
        val zero = translator.makeInt(java.math.BigInteger.ZERO)
        val one = translator.makeInt(java.math.BigInteger.ONE)
        val a = term.binder(arrSort, "a")
        val loB = term.binder(idxSort, "lo")
        val hiB = term.binder(idxSort, "hi")
        val aR = term.variable(arrSort, null, "a")
        val loR = term.variable(idxSort, null, "lo")
        val hiR = term.variable(idxSort, null, "hi")
        val call = { array: SExpr, lo: SExpr, hi: SExpr ->
            term.list(null, elemSort, term.symbol(fnName), array, lo, hi)
        }
        // definition: jmlSum(a, lo, hi) = ite(hi <= lo, 0, jmlSum(a, lo, hi-1) + a[hi-1])
        val hiMinus1 = term.subtract(hiR, one)
        val body = SList(
            elemSort, null,
            listOf(
                term.symbol("ite"),
                term.lessOrEquals(hiR, loR, true),
                zero,
                term.add(call(aR, loR, hiMinus1), term.select(elemSort, null, aR, hiMinus1))
            )
        )
        smtLog.addCommand(
            term.command(
                "define-fun-rec", term.symbol(fnName),
                SList(null, null, listOf(a, loB, hiB)),
                term.type(elemSort),
                body
            )
        )
        // recurrence: jmlSum(a, lo, hi+1) = jmlSum(a, lo, hi) + a[hi]
        val hiPlus1 = term.add(hiR, one)
        val sumPlus = call(aR, loR, hiPlus1)
        val axiom = term.forall(
            listOf(a, loB, hiB),
            withPattern(
                term.equality(sumPlus, term.add(call(aR, loR, hiR), term.select(elemSort, null, aR, hiR))),
                listOf(sumPlus)
            )
        )
        smtLog.addAssert(axiom)
    }

    /** `(! formula :pattern (patterns...))`: attaches an E-matching trigger to a
     *  quantified axiom so Z3 instantiates it on matching ground terms. */
    private fun withPattern(formula: SExpr, patterns: List<SExpr>): SExpr =
        SList(
            SmtType.BOOL, null,
            listOf(
                term.symbol("!"), formula,
                term.symbol(":pattern"), SList(null, null, patterns)
            )
        )

    //endregion

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
        // bare field access of the enclosing class: when the identity-keyed field
        // map is live, a write through an alias of `this` (`Box b = this; b.value =
        // 5`) may have updated the field, so route through the this-read handler
        // (which prefers the map) instead of reading the flat constant directly.
        if (env["this.$name"] != null || oldEnv["this.$name"] != null) {
            val thisVal = env["this"] ?: oldEnv["this"]
            if (thisVal != null) {
                thisFieldRead(env, thisVal, name)?.let { return it }
            }
        }
        env["this.$name"]?.let { return it }
        oldEnv[name]?.let { return it }
        return unknownHandler(n)
    }

    override fun visit(n: FieldAccessExpr, arg: Any?): SExpr {
        val scope = n.scope.accept(this, arg)!!
        if (n.nameAsString == "length") {
            return arrayLengthHandler(scope)
        }
        // concrete `this.f` reads go through the this-read handler against this
        // translator's own maps (so `\old(this.f)` sees the pre-state), preferring
        // the identity-keyed field map when it is live
        if (n.scope is ThisExpr) {
            val thisVal = env["this"] ?: oldEnv["this"]
            if (thisVal != null) {
                thisFieldRead(env, thisVal, n.nameAsString)?.let { return it }
            }
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
        val type = translator.getType(n.calculateResolvedType())
        val len = if (n.levels.isNotEmpty() && n.levels[0].dimension.isPresent) {
            n.levels[0].dimension.get().accept(this, arg)!!
        } else {
            null
        }
        return createArray(n.calculateResolvedType(), type, len)
    }

    /** Declares a fresh anonymous array constant of [type]; optionally binds its length. */
    fun createArray(javaType: ResolvedType?, type: SmtType, length: SExpr?): SExpr {
        val name = "anon_array_${anonArrayCounter.getAndIncrement()}"
        smtLog.declareConst(name, type)
        val v = term.variable(type, javaType, name)
        if (length != null) smtLog.addAssert(equality(arrayLengthHandler(v), length))
        // P4.2: `new T[...]` never returns null (Java) — tie the fresh array to the
        // per-sort null-array constant so `== null` and deref nullchecks on it are
        // decided by the solver instead of staying open.
        if (type is SmtType.Array) {
            arrayNullTerm(v)?.let { nullArr -> smtLog.addAssert(term.not(equality(v, nullArr))) }
        }
        return v
    }

    override fun visit(n: MethodCallExpr, arg: Any?): SExpr {
        // \old(x) (JML keyword spelling) or old(x): refer to the pre-state environment.
        // The JML keyword `\old` is lexed as the method name `\old`, whereas a literal
        // `old(...)` source call keeps the bare name; both must be recognized.
        if ((n.nameAsString == "old" || n.nameAsString == "\\old") && n.arguments.size == 1) {
            val inner = ExprTranslator(
                smtLog, translator, oldEnv, oldEnv, callHandler, unknownHandler,
                fieldAccessHandler = fieldAccessHandler, thisFieldRead = thisFieldRead,
                arrayRefHandler = arrayRefHandler, arrayMayBeNull = arrayMayBeNull,
                arrayNullTerm = arrayNullTerm, arrayLengthHandler = arrayLengthHandler,
                invariantForHandler = invariantForHandler, anonArrayCounter = anonArrayCounter
            )
            return n.arguments[0].accept(inner, arg)!!
        }
        // \invariant_for(this) / \static_invariant_for(T.class): fold the class
        // invariants of the argument's type (JML's statically-visible set). The
        // keywords are lexed as `\`-prefixed names, so they arrive as method calls —
        // never as resolvable Java methods — and must be expanded here instead of
        // being routed to the call handler.
        val name = n.nameAsString
        if ((name == "\\invariant_for" || name == "\\static_invariant_for") && n.arguments.size == 1) {
            val a = n.arguments[0]
            val recv = if (a is ClassExpr) null else a.accept(this, arg)
            // a recognized `\invariant_for` never falls through to call handling:
            // an unresolved target folds to `true` (vacuously satisfiable)
            return invariantForHandler(recv, a, name == "\\static_invariant_for") ?: term.makeTrue()
        }
        return callHandler(n)
    }

    /** A `\lbl` / `\lblpos` / `\lblneg` label is transparent to verification: the
     *  clause is the wrapped expression. The label only names the conjunct (on
     *  failure it identifies the violated invariant clause). */
    override fun visit(n: JmlLabelExpr, arg: Any?): SExpr = n.expression.accept(this, arg)!!

    companion object {
        const val RESULT = "\$result"
        const val RET = "\$ret"
        const val BREAK = "\$break_"
        const val CONTINUE = "\$continue_"
        /** thrown flag (abrupt exceptional completion) */
        const val EXC = "\$exc"
        /** the thrown exception object */
        const val EXCVAL = "\$excval"
        /** unique suffix for anonymous array constants. Standalone translators
         *  (tests, legacy callers) share this counter; the VCG passes a fresh
         *  per-verification counter instead so golden queries are reproducible. */
        private val anonCnt = java.util.concurrent.atomic.AtomicInteger()
    }
}

/**
 * Shared, per-query state for the JML `\sum` model: the recursive `jmlSum`
 * summation functions declared so far, keyed by the deterministic function name
 * derived from the array sort ([ExprTranslator.sumFunctionName]). Every
 * [ExprTranslator] of one verification shares a single instance so the
 * `define-fun-rec` declaration and its recurrence axiom are emitted exactly once
 * per query — redeclaring a function in SMT-LIB would be a hard error, and a
 * sum-manifested query must not duplicate axioms (goldens, solver effort).
 */
class JmlSumState {
    private val declared = HashSet<String>()

    fun contains(fnName: String): Boolean = fnName in declared

    fun declare(fnName: String) {
        declared.add(fnName)
    }
}

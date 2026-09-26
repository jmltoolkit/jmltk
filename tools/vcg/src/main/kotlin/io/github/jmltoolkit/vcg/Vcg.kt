/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.ThisExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.jml.NodeWithContracts
import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import com.github.javaparser.ast.jml.clauses.JmlContract
import com.github.javaparser.ast.jml.clauses.JmlLabeledClause
import com.github.javaparser.ast.jml.clauses.JmlMultiExprClause
import com.github.javaparser.ast.jml.clauses.JmlSimpleExprClause
import com.github.javaparser.ast.jml.expr.JmlExpression
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.resolution.types.ResolvedType
import io.github.jmltoolkit.smt.ArithmeticTranslator
import io.github.jmltoolkit.smt.BitVectorArithmeticTranslator
import io.github.jmltoolkit.smt.IntArithmeticTranslator
import io.github.jmltoolkit.smt.SmtQuery
import io.github.jmltoolkit.smt.SmtTermFactory
import io.github.jmltoolkit.smt.SmtTermFactory.equality
import io.github.jmltoolkit.smt.model.SAtom
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.model.SList
import io.github.jmltoolkit.smt.model.SmtType
import io.github.jmltoolkit.utils.JMLUtils
import io.github.jmltoolkit.vcg.ir.NfArray
import io.github.jmltoolkit.vcg.ir.NfAssert
import io.github.jmltoolkit.vcg.ir.NfAssign
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
import io.github.jmltoolkit.vcg.ir.NfSwitch
import io.github.jmltoolkit.vcg.ir.NfSwitchCase
import io.github.jmltoolkit.vcg.ir.NfThrow
import io.github.jmltoolkit.vcg.ir.NfTryCatch
import io.github.jmltoolkit.vcg.ir.NfCatchClause

/**
 * Verification condition generator for Java + JML based on strongest postconditions
 * over an SSA-style normal form (see [Normalizer] and [io.github.jmltoolkit.vcg.ir]).
 *
 * The generator takes
 *  * a [VcgContext] (the method/constructor to verify with its [JmlContract] and class members),
 *  * [VcgOptions] (bounded/unbounded mode, loop and call handling)
 * and produces a [VcgResult], i.e. an [SmtQuery] with one check-sat block per
 * verification condition. A condition is proven iff the solver answers `unsat`.
 */
class Vcg(private val ctx: VcgContext, private val options: VcgOptions) {
    private val term = SmtTermFactory
    val query = SmtQuery()
    val translator: ArithmeticTranslator =
        if (options.mode == VerificationMode.UNBOUNDED) {
            IntArithmeticTranslator(query)
        } else {
            BitVectorArithmeticTranslator(query)
        }

    /** SSA environment: location key -> current symbol. */
    private var env = LinkedHashMap<String, SExpr>()
    private val envTypes = HashMap<String, SmtType>()
    private val envJavaTypes = HashMap<String, ResolvedType?>()
    private val oldEnv = HashMap<String, SExpr>()
    private val versions = HashMap<String, Int>()
    private val obligations = mutableListOf<VerificationCondition>()
    private var uid = 0
    private var inlineDepth = 0

    /** Recording mode: assertions are collected into a local formula, not the query. */
    private class Recording(val premise: SExpr) {
        val conjuncts = mutableListOf<SExpr>()
    }

    private sealed class Mode {
        object Real : Mode()
        data class Record(val rec: Recording) : Mode()
    }

    //endregion
    //region entry point
    fun verify(): VcgResult {
        query.addCommand(term.command("set-logic", term.symbol("ALL")))
        setupTypeHierarchy()
        setupContext()
        assumeRequires()
        val md = ctx.callable as? MethodDeclaration
        val cd = ctx.callable as? com.github.javaparser.ast.body.ConstructorDeclaration
        val body: BlockStmt = when {
            md != null -> md.body.orElseThrow {
                IllegalArgumentException("Method has no body: ${md.nameAsString}")
            }

            cd != null -> cd.body.get()

            else -> throw IllegalArgumentException("Unsupported callable: ${ctx.callable}")
        }
        val nf = Normalizer().normalize(body)
        exec(nf, term.makeTrue(), Mode.Real)
        checkPostcondition(term.makeTrue())
        emitVcs()
        return VcgResult(query, obligations.toList())
    }

    /**
     * Declares the reference/type-hierarchy encoding:
     *   - sorts `T` (types) and `U` (references),
     *   - `typeof: U -> T`, `subtype: TT -> Bool`, `instanceof: U T -> Bool`, `cast`,
     *   - one sort constant per user type in the compilation unit and the boxed
     *     primitive sorts, together with their subtype edges (class hierarchy).
     * Reference values (`this`, object parameters, fields) are `U`-sorted constants.
     */
    private fun setupTypeHierarchy() {
        query.addCommand(term.command("declare-sort", term.symbol("T"), term.intValue(0)))
        query.addCommand(term.command("declare-sort", term.symbol("U"), term.intValue(0)))
        fun fun1(name: String, arg: String, ret: String) =
            query.addCommand(
                term.command(
                    "declare-fun", term.symbol(name),
                    SList(null, null, listOf(term.symbol(arg))), term.symbol(ret)
                )
            )
        fun1("typeof", "U", "T")
        fun2("subtype", "T", "T", "Bool")
        fun2("instanceof", "U", "T", "Bool")
        fun2("exactinstanceof", "U", "T", "Bool")
        // cast: U T -> U
        query.addCommand(
            term.command(
                "declare-fun", term.symbol("cast"),
                SList(null, null, listOf(term.symbol("U"), term.symbol("T"))),
                term.symbol("U")
            )
        )
        // special reference constant `null`
        query.addCommand(term.command("declare-const", term.symbol("null"), term.symbol("U")))

        // collect all user types reachable from the compilation unit
        val allTypes = LinkedHashSet<String>()
        val cu = ctx.callable.findAncestor(com.github.javaparser.ast.CompilationUnit::class.java)
            .orElse(null)
        if (cu != null) {
            cu.findAll(com.github.javaparser.ast.body.TypeDeclaration::class.java)
                .forEach { allTypes.add(it.nameAsString) }
        }
        allTypes.addAll(
            setOf(
                "java.lang.Object",
                "java.lang.Exception",
                "java.lang.RuntimeException",
                "java.lang.ArithmeticException",
                "java.lang.NullPointerException",
                "java.lang.ArrayIndexOutOfBoundsException",
            )
        )

        for (t in allTypes) {
            val sort = typeSortName(t)
            query.addCommand(term.command("declare-const", term.symbol(sort), term.symbol("T")))
        }
        // object is the root of the (non-null) hierarchy
        val objSort = typeSortName("java.lang.Object")
        for (t in allTypes) {
            if (t != "java.lang.Object") {
                // every user type is a subtype of Object
                query.addAssert(
                    term.list(
                        null, SmtType.BOOL, term.symbol("subtype"),
                        term.symbol(typeSortName(t)), term.symbol(objSort)
                    )
                )
            }
        }
        // exceptions are Object subtypes (transitively handled by z3 via the subtype axiom)
    }

    private fun fun2(name: String, a: String, b: String, ret: String) =
        query.addCommand(
            term.command(
                "declare-fun", term.symbol(name),
                SList(null, null, listOf(term.symbol(a), term.symbol(b))), term.symbol(ret)
            )
        )

    private fun typeSortName(qualified: String): String = "sort_" + qualified.replace('.', '_').lowercase()

    /** True if the SMT type denotes a reference (object) value of the Java hierarchy. */
    private fun isRefType(t: SmtType): Boolean = t === SmtType.JAVA_OBJECT

    /** Declares a reference-typed constant with the `U` sort of the hierarchy. */
    private fun declareRef(name: String, key: String, jType: ResolvedType?): SExpr {
        query.addCommand(
            term.command("declare-const", term.symbol(name), term.symbol("U"))
        )
        val v = term.variable(SmtType.JAVA_OBJECT, jType, name)
        env[key] = v
        envTypes[key] = SmtType.JAVA_OBJECT
        envJavaTypes[key] = jType
        return v
    }

    /** Declares parameters, `this` and the member variables. */
    private fun setupContext() {
        val callable = ctx.callable
        val isStatic = callable.isStatic
        if (!isStatic) {
            declareRef("this", "this", null)
            query.addAssert(term.nonNull(term.makeThis()))
        }
        for (p in callable.parameters) {
            val key = p.nameAsString
            val jType = resolveType(p.type)
            val sType = safeType(jType)
            if (isRefType(sType)) {
                declareRef(key, key, jType)
            } else {
                query.declareConst(key, sType)
                env[key] = term.variable(sType, jType, key)
                envTypes[key] = sType
                envJavaTypes[key] = jType
            }
        }
        for (field in ctx.enclosingType.fields) {
            for (v in field.variables) {
                val key = if (field.isStatic) v.nameAsString else "this." + v.nameAsString
                val jType = resolveType(v.type)
                val sType = safeType(jType)
                if (isRefType(sType)) {
                    declareRef(key, key, jType)
                } else {
                    query.declareConst(key, sType)
                    env[key] = term.variable(sType, jType, key)
                    envTypes[key] = sType
                    envJavaTypes[key] = jType
                }
                if (field.isStatic) {
                    // make the field also visible under its bare name
                    env[v.nameAsString] = env[key]!!
                    envTypes[v.nameAsString] = sType
                }
            }
        }
        // return flag and result variable
        env[ExprTranslator.RET] = term.makeFalse()
        env[ExprTranslator.EXC] = term.makeFalse()
        env[ExprTranslator.EXCVAL] = term.makeNull()
        envTypes[ExprTranslator.EXC] = SmtType.BOOL
        envTypes[ExprTranslator.EXCVAL] = SmtType.JAVA_OBJECT
        val returnType = returnTypeOf(callable)
        if (returnType != null) {
            val sType = safeType(returnType)
            query.declareConst(ExprTranslator.RESULT, sType)
            env[ExprTranslator.RESULT] = term.variable(sType, returnType, ExprTranslator.RESULT)
            envTypes[ExprTranslator.RESULT] = sType
        }
        oldEnv.putAll(env)
    }

    private fun returnTypeOf(callable: CallableDeclaration<*>): ResolvedType? {
        if (callable is MethodDeclaration) {
            return try {
                callable.type.resolve()
            } catch (e: Exception) {
                null
            }
        }
        return null // constructors have no result
    }

    private fun resolveType(t: com.github.javaparser.ast.type.Type): ResolvedType? = try {
        t.resolve()
    } catch (e: Exception) {
        null
    }

    private fun safeType(jType: ResolvedType?): SmtType {
        val t = try {
            if (jType != null) translator.getType(jType) else SmtType.INT
        } catch (e: Exception) {
            SmtType.INT
        }
        if (t is SmtType.Array) ensureLengthFn(t)
        return t
    }

    /** Declares the array-length function for the current mode once. */
    private fun ensureLengthFn(@Suppress("UNUSED_PARAMETER") arr: SmtType.Array) {
        if (lengthFnDeclared) return
        lengthFnDeclared = true
        val unbounded = options.mode == VerificationMode.UNBOUNDED
        val name = if (unbounded) "int\$length" else "bv\$length"
        val ret = if (unbounded) SmtType.INT else SmtType.BV32
        val arg = if (unbounded) {
            SmtType.Array(SmtType.INT, SmtType.INT)
        } else {
            SmtType.Array(SmtType.BV32, SmtType.BV32)
        }
        query.addCommand(
            term.command(
                "declare-fun",
                term.symbol(name),
                SList(null, null, listOf(term.type(arg))),
                term.type(ret)
            )
        )
        // Java array lengths are always non-negative; without this the bit-vector
        // length may take a huge negative value that passes `len <= 4` under signed
        // comparison, turning e.g. a binary search bound into nonsense. This is a
        // guarded-free quantifier that E-matching instantiates only on `length`
        // terms, so it stays cheap (unlike a quantified store-preservation axiom,
        // which made the solver answer `unknown` on simple queries).
        val lenVar = term.binder(arg, "a")
        val lenTerm = translator.arrayLength(term.variable(arg, null, "a"))
        val zero = if (unbounded) translator.makeInt(java.math.BigInteger.ZERO) else translator.makeInt(0)
        query.addCommand(
            term.command(
                "assert",
                term.forall(listOf(lenVar), term.greaterOrEquals(lenTerm, zero, true))
            )
        )
    }

    private var lengthFnDeclared = false

    /**
     * Declared heap selector functions for instance fields, keyed by field name.
     * Each is `(declare-fun <Class>.<field> (U) <ret>)`, applied to an arbitrary
     * receiver to read `o.field` (see [fieldRead]).
     */
    private val instanceFieldSelectors = HashMap<String, String>()

    /** The field-selector function name for an instance field, declaring it lazily. */
    private fun fieldSelector(fieldName: String, retType: SmtType, jType: ResolvedType?): String {
        instanceFieldSelectors[fieldName]?.let { return it }
        val fn = "\$field__" + fieldName
        query.addCommand(
            term.command(
                "declare-fun", term.symbol(fn),
                SList(null, null, listOf(term.symbol("U"))),
                if (retType === SmtType.JAVA_OBJECT) term.symbol("U") else term.type(retType)
            )
        )
        instanceFieldSelectors[fieldName] = fn
        return fn
    }

    private fun assumeRequires() {
        for (e in clauseExprs(ctx.contract, REQUIRES)) {
            query.addAssert(translatorOf(e).tr(e))
        }
    }

    private fun checkPostcondition(guard: SExpr) {
        val ensures = clauseExprs(ctx.contract, ENSURES)
        if (ensures.isEmpty()) return
        var post = term.makeTrue()
        for (e in ensures) {
            post = term.and(post, translatorOf(e).tr(e))
        }
        val callable = ctx.callable
        val m = callable as? MethodDeclaration
        val isVoid = m == null || m.type.toString() == "void"
        val ret = flag(ExprTranslator.RET)
        val ob =
            if (isVoid) {
                term.impl(guard, post)
            } else {
                term.impl(term.and(guard, ret), post)
            }
        addObligation("postcondition", ob, Mode.Real, ctx.callable)
    }

    private fun emitVcs() {
        for (vc in obligations) {
            query.push()
            val named = SList(
                SmtType.COMMAND, null,
                listOf(
                    term.symbol("!"), term.not(vc.obligation),
                    term.symbol(":named"), term.symbol(vc.id)
                )
            )
            query.addAssert(named)
            query.checkSat()
            query.pop()
        }
    }
    //endregion

    //region clause helpers
    companion object {
        val REQUIRES = setOf(
            JmlClauseKind.REQUIRES, JmlClauseKind.REQUIRES_FREE, JmlClauseKind.PRE
        )
        val ENSURES = setOf(
            JmlClauseKind.ENSURES, JmlClauseKind.ENSURES_FREE, JmlClauseKind.POST
        )
        val LOOP_INVARIANTS = setOf(
            JmlClauseKind.LOOP_INVARIANT, JmlClauseKind.LOOP_INVARIANT_FREE,
            JmlClauseKind.MAINTAINING, JmlClauseKind.MAINTAINING_REDUNDANTLY,
            JmlClauseKind.INVARIANT
        )
        val LOOP_VARIANTS = setOf(
            JmlClauseKind.DECREASES, JmlClauseKind.DECREASING,
            JmlClauseKind.DECREASES_REDUNDANTLY, JmlClauseKind.MEASURED_BY
        )
        val ASSIGNABLE = setOf(
            JmlClauseKind.ASSIGNABLE, JmlClauseKind.ASSIGNABLE_REDUNDANTLY,
            JmlClauseKind.MODIFIABLE, JmlClauseKind.MODIFIABLE_REDUNDANTLY,
            JmlClauseKind.MODIFIES
        )
        val BREAKS = setOf(JmlClauseKind.BREAKS, JmlClauseKind.BREAKS_REDUNDANTLY)
        val CONTINUES = setOf(JmlClauseKind.CONTINUES, JmlClauseKind.CONTINUES_REDUNDANTLY)
        val RETURNS = setOf(JmlClauseKind.RETURNS, JmlClauseKind.RETURNS_REDUNDANTLY)
    }

    private fun clauseExprs(contract: JmlContract, kinds: Set<JmlClauseKind>): List<Expression> {
        val res = mutableListOf<Expression>()
        for (c in contract.clauses) {
            when (c) {
                is JmlSimpleExprClause -> if (c.kind in kinds) res.add(c.expression)
                is JmlMultiExprClause -> if (c.kind in kinds) res.addAll(c.expressions)
                is JmlLabeledClause -> if (c.kind in kinds) res.add(c.expression)
                else -> {}
            }
        }
        return res
    }

    private fun loopContracts(loop: Any): List<JmlContract> {
        if (loop is NodeWithContracts<*>) {
            JMLUtils.unroll(loop.contracts)
            return loop.contracts.toList()
        }
        return emptyList()
    }

    private fun loopClauses(loop: Any, kinds: Set<JmlClauseKind>): List<Expression> =
        loopContracts(loop).flatMap { clauseExprs(it, kinds) }

    /** Create an expression translator against the current environment. */
    private fun translatorOf(
        e: Expression? = null, envOverride: Map<String, SExpr>? = null,
                            oldOverride: Map<String, SExpr>? = null
    ): ExprTranslator {
        val e1 = envOverride ?: env
        return ExprTranslator(
            query, translator, e1,
            oldOverride ?: oldEnv,
            callHandler = { call -> handleCall(call, term.makeTrue(), Mode.Real) },
            unknownHandler = { n -> declareUnknown(n.nameAsString) },
            fieldAccessHandler = { receiver, field, scopeText -> fieldReadFor(e1, receiver, field, scopeText) }
        )
    }

    /**
     * Resolves `receiver.field` in [activeEnv]. For the concrete `this` receiver the
     * flat constant is used (the receiver may be rebound per call site via the `this`
     * entry of the active environment); for arbitrary receivers a previous assignment
     * (write-versioned `scope.field`) is preferred, otherwise a per-field heap
     * selector application is returned.
     */
    private fun fieldReadFor(
        activeEnv: Map<String, SExpr>,
        receiver: SExpr,
        field: String,
        scopeText: String,
    ): SExpr {
        val thisRepr = activeEnv["this"]
        if (thisRepr != null && (receiver === thisRepr || scopeText == "this")) {
            return activeEnv["this.$field"] ?: declareUnknownField(field)
        }
        val versioned = activeEnv["$scopeText.$field"]
        if (versioned != null) return versioned
        // The field's SMT sort must come from the *receiver's* type (not the
        // enclosing class): an `int` field of a referenced object is a bit-vector
        // in BOUNDED mode, so a selector typed as `Int` would be ill-sorted.
        val (jType, sType) = fieldSelectorType(receiver, scopeText, field)
        val fn = fieldSelector(field, sType, jType)
        return term.list(jType, sType, term.symbol(fn), receiver)
    }

    /** Resolves the Java/SMT type of `scope.field` from the resolved receiver type. */
    private fun fieldSelectorType(
        receiver: SExpr, scopeText: String, field: String
    ): Pair<ResolvedType?, SmtType> {
        val thisJ = envJavaTypes["this.$field"]
        if (thisJ != null) return thisJ to (envTypes["this.$field"] ?: safeType(thisJ))
        val resolved = receiver.javaType
        val fieldJType = (resolved as? com.github.javaparser.resolution.types.ResolvedReferenceType)
            ?.getFieldType(field)?.orElse(null)
        if (fieldJType != null) return fieldJType to safeType(fieldJType)
        // last resort: mode-appropriate scalar default
        val default = if (options.mode == VerificationMode.BOUNDED) SmtType.BV32 else SmtType.INT
        return null to (envTypes["this.$field"] ?: default)
    }

    /** SMT sort for writing `scope.field`, mirroring [fieldSelectorType]. */
    private fun fieldWriteType(t: NfField, key: String): SmtType {
        envTypes[key]?.let { return it }
        if (t.receiver == "this") return locType(t)
        val receiverExpr = env[t.receiver]
        val fieldName = t.key.substringAfterLast('.')
        if (receiverExpr != null) return fieldSelectorType(receiverExpr, t.receiver, fieldName).second
        return locType(t)
    }

    /** Declares an unknown field of `this` on first use (flat constant model). */
    private fun declareUnknownField(field: String): SExpr {
        val key = "this.$field"
        val sType = envTypes[key] ?: SmtType.INT
        query.declareConst(key, sType)
        val v = term.variable(sType, envJavaTypes[key], key)
        env[key] = v
        envTypes[key] = sType
        return v
    }

    private fun declareUnknown(name: String): SExpr {
        val key = unknownKey(name)
        val sType = envTypes[key] ?: SmtType.INT
        query.declareConst(key, sType)
        val v = term.variable(sType, envJavaTypes[key], key)
        env[key] = v
        envTypes[key] = sType
        return v
    }

    private fun unknownKey(name: String): String =
        if (env.containsKey("this.$name")) "this.$name" else "unk_" + name
    //endregion

    //region symbol table
    private fun nextName(key: String): String {
        val v = (versions[key] ?: 0) + 1
        versions[key] = v
        // A write to a location that already holds a value (a field/parameter
        // declared by [setupContext] or an earlier assignment) must get a fresh
        // `key$N` constant. Reusing the base name `key` — allowed only for the
        // write that actually *introduces* a location — would make a self-referential
        // first write such as `f = f + 1` assert `f = f + 1`, an unsatisfiable
        // context that silently turns every VC into a trivial proof.
        return if (v == 1 && !env.containsKey(key)) key else "$key\$$v"
    }

    private fun freshVersion(key: String, type: SmtType, javaType: ResolvedType? = null): SExpr {
        val name = nextName(key)
        if (type === SmtType.JAVA_OBJECT) {
            query.addCommand(
                term.command("declare-const", term.symbol(name), term.symbol("U"))
            )
        } else {
            query.declareConst(name, type)
        }
        val v = term.variable(type, javaType ?: envJavaTypes[key], name)
        env[key] = v
        envTypes[key] = type
        return v
    }

    private fun freshTemp(type: SmtType, javaType: ResolvedType? = null): SExpr {
        val name = "\$t${++uid}"
        if (type === SmtType.JAVA_OBJECT) {
            query.addCommand(
                term.command("declare-const", term.symbol(name), term.symbol("U"))
            )
        } else {
            query.declareConst(name, type)
        }
        return term.variable(type, javaType, name)
    }

    private fun flag(key: String): SExpr = env[key] ?: term.makeFalse()

    /** `ite` that tolerates missing java types (e.g. auxiliary boolean flags). */
    private fun iteTerm(cond: SExpr, then: SExpr, otherwise: SExpr): SExpr =
        SList(then.smtType ?: otherwise.smtType, then.javaType, listOf(term.symbol("ite"), cond, then, otherwise))

    /** A location key for an [NfLocal]: local variable, or (implicit this-)field. */
    private fun localKey(name: String): String = when {
        env.containsKey(name) && !envTypes.containsKey("this.$name") -> name
        env.containsKey("this.$name") -> "this.$name"
        isFieldOfEnclosing(name) -> "this.$name"
        else -> name
    }

    private fun isFieldOfEnclosing(name: String): Boolean =
        ctx.enclosingType.fields.any { f -> f.variables.any { it.nameAsString == name } }

    private fun emit(mode: Mode, guard: SExpr, formula: SExpr) {
        val f = if (guard == term.makeTrue() && guard.toString() == "true") formula else term.impl(guard, formula)
        when (mode) {
            is Mode.Real -> query.addAssert(f)
            is Mode.Record -> mode.rec.conjuncts.add(f)
        }
    }

    /** Emits [formula] without wrapping it in the guard: used for phi-style bindings
     *  (`v' = ite(cond, new, old)`) whose guards re-appear inside the formula, so that
     *  dead paths (e.g. a `return` after an unrolled loop that never exits) still keep
     *  the previous value instead of leaving the new constant unconstrained. */
    private fun emitPhi(mode: Mode, formula: SExpr) {
        when (mode) {
            is Mode.Real -> query.addAssert(formula)
            is Mode.Record -> mode.rec.conjuncts.add(formula)
        }
    }

    private fun addObligation(description: String, prop: SExpr, mode: Mode, node: com.github.javaparser.ast.Node? = null) {
        val ob =
            when (mode) {
                is Mode.Real -> prop

                is Mode.Record -> {
                    var premise = mode.rec.premise
                    for (c in mode.rec.conjuncts) premise = term.and(premise, c)
                    term.impl(premise, prop)
                }
            }
        val id = "vc${obligations.size + 1}"
        obligations.add(VerificationCondition(id, description, ob, node?.range?.orElse(null)))
    }
    //endregion

    //region statement execution (SP semantics)
    private fun exec(stmts: List<NfStmt>, guard: SExpr, mode: Mode): SExpr {
        var g = guard
        for (s in stmts) g = execStmt(s, g, mode)
        return g
    }

    /** Executes a statement; returns the path guard for the *following* statements. */
    private fun execStmt(s: NfStmt, guard: SExpr, mode: Mode): SExpr {
        when (s) {
            is NfAssign -> execAssign(s, guard, mode)

            is NfCall -> handleCall(s.call, effectiveGuard(guard, s), mode)

            is NfIf -> execIf(s, guard, mode)

            is NfLoop -> {
                execLoop(s, guard, mode)
                return exitGuards[s.loopNode.hashCode()] ?: guard
            }

            is NfThrow -> execThrow(s, guard, mode)

            is NfTryCatch -> return execTryCatch(s, guard, mode)

            is NfSwitch -> throw UnsupportedOperationException(
                "switch statements are not yet supported by the VCG engine" +
                    (s.origin?.let { " (${describe(it)})" } ?: "")
            )

            is NfReturn -> execReturn(s, guard, mode)

            is NfBreak -> {
                val g = effectiveGuard(guard, s)
                setFlag(ExprTranslator.BREAK + s.loopId, g, mode)
                // loop contract: the breaks clause holds when the loop is left via break
                val f = loopFrames.lastOrNull { it.loopId == s.loopId }
                if (f != null && f.strategy != LoopStrategy.UNROLL && f.breaks.isNotEmpty()) {
                    var clause = term.makeTrue()
                    for (b in f.breaks) clause = term.and(clause, translatorOf().tr(b))
                    addObligation("loop breaks clause ${describe(s.origin!!)}", term.impl(g, clause), mode)
                }
            }

            is NfContinue -> {
                val g = effectiveGuard(guard, s)
                setFlag(ExprTranslator.CONTINUE + s.loopId, g, mode)
                val f = loopFrames.lastOrNull { it.loopId == s.loopId }
                if (f != null && f.strategy != LoopStrategy.UNROLL) {
                    var clause = term.makeTrue()
                    for (c in f.continues) clause = term.and(clause, translatorOf().tr(c))
                    if (f.continues.isNotEmpty()) {
                        addObligation("loop continues clause ${describe(s.origin!!)}", term.impl(g, clause), mode)
                    }
                    // a continue reaches the loop head, so the invariant must hold here
                    if (f.invariants.isNotEmpty()) {
                        var inv = term.makeTrue()
                        for (e in f.invariants) inv = term.and(inv, translatorOf().tr(e))
                        addObligation("loop invariant at continue ${describe(s.origin!!)}", term.impl(g, inv), mode)
                    }
                }
            }

            is NfAssume -> emit(mode, effectiveGuard(guard, s), translatorOf().tr(s.expr))

            is NfAssert -> addObligation(
                "assert ${s.origin?.let { describe(it) } ?: ""}",
                term.impl(effectiveGuard(guard, s), translatorOf().tr(s.expr)), mode,
                s.origin
            )

            is NfHavoc -> freshVersion(locationKey(s.location), locType(s.location))

            is NfBlock -> return exec(s.stmts, guard, mode)
        }
        return guard
    }

    private fun describe(n: com.github.javaparser.ast.Node): String {
        val r = n.range.orElse(null)
        return "at line ${r?.begin?.line ?: "?"}"
    }

    /** Guard extended by the negation of the abrupt-completion flags that make the
     * statement unreachable (method return, thrown exception, breaks of enclosing
     * loops, continue of the innermost loop). */
    private fun effectiveGuard(guard: SExpr, s: NfStmt): SExpr {
        var g = term.and(guard, term.not(flag(ExprTranslator.RET)))
        g = term.and(g, term.not(flag(ExprTranslator.EXC)))
        for (id in enclosingBreakIds(s)) {
            g = term.and(g, term.not(flag(ExprTranslator.BREAK + id)))
        }
        innermostContinueId(s)?.let { id ->
            g = term.and(g, term.not(flag(ExprTranslator.CONTINUE + id)))
        }
        return g
    }

    private val loopContext = ArrayDeque<Pair<Int, Boolean>>() // (loopId, isSwitchLike?)

    /** Per-loop contract data used at break/continue points. */
    private class LoopFrame(
        val loopId: Int,
        val strategy: LoopStrategy,
        val invariants: List<Expression>,
        val breaks: List<Expression>,
        val continues: List<Expression>,
    )

    private val loopFrames = ArrayDeque<LoopFrame>()

    private fun enclosingBreakIds(@Suppress("UNUSED_PARAMETER") s: NfStmt): List<Int> =
        loopContext.map { it.first }.toList()

    private fun innermostContinueId(@Suppress("UNUSED_PARAMETER") s: NfStmt): Int? =
        loopContext.lastOrNull()?.first

    private fun setFlag(key: String, guard: SExpr, mode: Mode) {
        val prev = env[key] ?: term.makeFalse()
        val f = freshVersion(key, SmtType.BOOL)
        // phi-style binding: `f = ite(guard, true, prev)`. Without it, `f` stays an
        // unconstrained fresh constant on paths where the guard is false, which lets
        // the solver spuriously take the abrupt-completion branch (`$break`/`$continue`)
        // later (e.g. in an unrolled loop's exit conditions) with unbound post values.
        emitPhi(mode, equality(f, iteTerm(guard, term.makeTrue(), prev)))
    }

    /** Executes a `throw`; sets the exceptional completion flags, disabling normal
     * completion of the current path (effectiveGuard filters on `$exc` afterwards). */
    private fun execThrow(s: NfThrow, guard: SExpr, mode: Mode) {
        val g = effectiveGuard(guard, s)
        val v = atom(s.exception, g, mode)
        // exception value (fresh) and thrown flag
        val ev = freshVersion(ExprTranslator.EXCVAL, SmtType.JAVA_OBJECT)
        emit(mode, g, equality(ev, v))
        val exc = freshVersion(ExprTranslator.EXC, SmtType.BOOL)
        emit(mode, g, equality(exc, term.makeTrue()))
    }

    /**
     * Lowers `try/catch/finally`. The try body is executed; if it threw an exception
     * (`$exc`) whose type matches a catch clause (via the subtype relation of the
     * reference encoding), the first matching catch body runs with the bound variable
     * equal to the thrown value. If `finally` is present it runs on the handled and
     * normal paths. The exception flag is reset so the enclosing path continues
     * normally whenever the exception was handled or none was thrown.
     */
    private fun execTryCatch(s: NfTryCatch, guard: SExpr, mode: Mode): SExpr {
        val g = effectiveGuard(guard, s)

        // (1) execute the try body
        exec(s.tryBody, g, mode)
        val afterTry = LinkedHashMap(env)
        val exc = flag(ExprTranslator.EXC)
        val excval = flag(ExprTranslator.EXCVAL)
        // the try body is kept as the continuing state unless a catch overrides it
        var continueEnv = LinkedHashMap(afterTry)

        // (2) run the first matching catch clause.
        //     Each catch `C_i` matches when an exception was thrown and (if C_i has a
        //     declared type) the thrown object is a subtype of that type, and no earlier
        //     clause already matched.
        env = LinkedHashMap(afterTry)
        val snapshot = LinkedHashMap(env)
        var handled = term.makeFalse()
        var anyCatch = false
        for ((i, c) in s.catches.withIndex()) {
            val match =
                if (c.type != null) {
                    term.and(exc, typeMatch(excval, c.type))
                } else {
                    exc
                }
            // a clause matches when an exception is thrown and it matches and no
            // earlier clause already matched
            val cg = term.and(g, match, term.not(handled))
            // later clauses must additionally account for this one matching
            if (i < s.catches.size - 1) handled = term.or(handled, match)
            env = LinkedHashMap(snapshot)
            val paramType =
                c.type?.let { safeType(tryResolve(it)) } ?: SmtType.JAVA_OBJECT
            val bound = freshVersion(c.parameter, paramType)
            emit(mode, cg, equality(bound, excval))
            exec(c.body, cg, mode)
            // a handled exception's modifications continue past the try
            if (i == s.catches.size - 1) continueEnv = LinkedHashMap(env)
            anyCatch = true
        }

        // (3) merge the normal (no exception) with the handled state, run finally,
        //     and reset the exception flag.
        val normalGuard = term.and(g, term.not(exc))
        val resumeGuard = if (anyCatch) term.or(normalGuard, handled) else normalGuard
        // phi-merge between the try-normal state (`afterTry`) and the handled state
        // (`continueEnv`), selected by the `handled` predicate, so the value on the
        // no-exception path is the try's own modification.
        if (anyCatch) {
            env = LinkedHashMap(afterTry)
            for (key in afterTry.keys.union(continueEnv.keys)) {
                val normalV = afterTry[key]
                val handledV = continueEnv[key]
                if (normalV != handledV && normalV != null && handledV != null) {
                    val type = envTypes[key] ?: continue
                    val phi = freshVersion(key, type)
                    emit(mode, resumeGuard, equality(phi, iteTerm(handled, handledV, normalV)))
                    env[key] = phi
                }
            }
        } else {
            env = LinkedHashMap(afterTry)
        }
        if (s.finallyBody.isNotEmpty()) {
            exec(s.finallyBody, resumeGuard, mode)
        }
        // reset the exception flag on all paths that continue past the try
        val excFresh = freshVersion(ExprTranslator.EXC, SmtType.BOOL)
        emit(mode, resumeGuard, equality(excFresh, term.makeFalse()))
        // an unhandled exception keeps the path unreachable for the caller level
        return resumeGuard
    }
    /** Builds an `instanceof`-style type match term for the reference encoding:
     *  `(instanceof value sort_C)`, true iff the runtime type of `value` is a subtype
     *  of `C`. Unqualified standard-library names are resolved to `java.lang.*`. */
    private fun typeMatch(value: SExpr, type: com.github.javaparser.ast.type.Type): SExpr {
        val name = qualifyType(type.toString())
        return term.list(
            null, SmtType.BOOL,
            term.symbol("instanceof"), value,
            typeSortName(name)
        )
    }

    /** Resolves a possibly-unqualified type name to a canonical `java.lang.*` name. */
    private fun qualifyType(name: String): String {
        if (name.contains('.')) return name
        return when (name) {
            "Exception", "RuntimeException", "ArithmeticException", "NullPointerException",
            "ArrayIndexOutOfBoundsException", "IndexOutOfBoundsException", "Throwable", "Object",
            -> "java.lang.$name"

            else -> name
        }
    }

    private fun execReturn(s: NfReturn, guard: SExpr, mode: Mode) {
        val g = effectiveGuard(guard, s)
        if (s.value != null && envTypes.containsKey(ExprTranslator.RESULT)) {
            // capture the previous value *before* freshVersion renames the env slot
            val prev = env[ExprTranslator.RESULT]
            val v = atom(s.value, g, mode)
            val res = freshVersion(ExprTranslator.RESULT, envTypes[ExprTranslator.RESULT]!!)
            // `return` may be dead on some paths (an earlier return/abrupt exit already
            // happened, or an enclosing unrolled loop could not exit). Bind the new
            // version phi-style so those paths keep the previous value instead of
            // leaving the constant unconstrained (which would produce spurious
            // counterexamples for the postcondition); the phi is emitted as a hard
            // equality because `g` reappears inside the ite.
            emitPhi(mode, equality(res, iteTerm(g, v, prev ?: res)))
        }
        val prevRet = env[ExprTranslator.RET]
        val ret = freshVersion(ExprTranslator.RET, SmtType.BOOL)
        emitPhi(mode, equality(ret, iteTerm(g, term.makeTrue(), prevRet ?: ret)))
    }

    private fun locationKey(l: NfLocation): String = when (l) {
        is NfLocal -> localKey(l.key)
        is NfField -> l.key
        is NfArray -> localKey(l.key)
    }

    private fun locType(l: NfLocation): SmtType =
        envTypes[locationKey(l)] ?: SmtType.INT

    private fun execAssign(s: NfAssign, guard: SExpr, mode: Mode) {
        val g = effectiveGuard(guard, s)
        when (val t = s.target) {
            is NfArray -> {
                val key = localKey(t.key)
                val arrType = envTypes[key] ?: SmtType.Array(SmtType.INT, SmtType.INT)
                val arr = env[key] ?: freshVersion(key, arrType)
                val idx = atom(t.index, g, mode)
                if (options.checkIndex) {
                    addObligation(
                        "array store bound ${describe(s.origin!!)}",
                        term.impl(
                            g,
                            term.and(
                                term.greaterOrEquals(idx, translator.makeInt(java.math.BigInteger.ZERO), true),
                                term.lessThan(idx, translator.arrayLength(arr))
                            )
                        ),
                        mode
                    )
                }
                val v = atom(s.rhs, g, mode)
                val next = freshVersion(key, arrType)
                // phi-style store: on paths where the store does not execute (dead
                // guard, e.g. statements after a `break`), the array keeps its old
                // value instead of becoming a fresh unconstrained constant.
                emitPhi(mode, equality(next, iteTerm(g, term.store(arr, idx, v), arr)))
                // frame the array length across the store: len(a[i := v]) == len(a).
                // Without this, a fresh version introduced by the store has an
                // unconstrained length and bounds checks on a read *afterwards*
                // can no longer be discharged from the `a.length` constraints.
                emit(mode, g, equality(translator.arrayLength(next), translator.arrayLength(arr)))
            }

            else -> {
                val key = locationKey(t)
                val type = when (t) {
                    is NfLocal -> t.declaredType?.let { dt -> safeType(tryResolve(dt)) } ?: locType(t)
                    is NfField -> fieldWriteType(t, key)
                    else -> locType(t)
                }
                val v = atom(s.rhs, g, mode)
                val prev = env[key]
                val next = freshVersion(key, type)
                // phi-style assignment: when the guard is false (dead path — e.g.
                // statements after a `break`/`continue`/`return`) the variable keeps
                // its previous value. Without this, `next` is an unconstrained fresh
                // constant that leaks into loop merges and postconditions.
                if (prev != null) {
                    emitPhi(mode, equality(next, iteTerm(g, v, prev)))
                } else {
                    emit(mode, g, equality(next, v))
                }
            }
        }
    }

    private fun tryResolve(t: com.github.javaparser.ast.type.Type): ResolvedType? = try {
        t.resolve()
    } catch (e: Exception) {
        null
    }

    private fun execIf(s: NfIf, guard: SExpr, mode: Mode) {
        val g = effectiveGuard(guard, s)
        val c = atom(s.cond, g, mode)
        val snapshot = LinkedHashMap(env)
        exec(s.thenStmts, term.and(g, c), mode)
        val thenEnv = LinkedHashMap(env)
        env = snapshot
        exec(s.elseStmts, term.and(g, term.not(c)), mode)
        // merge with ite-phis
        for (key in thenEnv.keys.union(env.keys)) {
            val tv = thenEnv[key]
            val ev = env[key]
            if (tv != ev && tv != null && ev != null) {
                val type = envTypes[key] ?: SmtType.INT
                val phi = freshVersion(key, type)
                emit(mode, g, equality(phi, iteTerm(c, tv, ev)))
                env[key] = phi
            } else if (tv != null && ev == null) {
                env[key] = tv
            }
        }
    }
    //endregion

    //region loops
    private fun execLoop(s: NfLoop, guard: SExpr, mode: Mode) {
        val annotated = loopClauses(s.loopNode, LOOP_INVARIANTS).isNotEmpty()
        val strategy =
            if (annotated && s.loopNode !in options.loopStrategies) {
                // an annotated loop is abstracted by default; abrupt clauses indicate a loop contract
                if (loopClauses(s.loopNode, BREAKS + CONTINUES + RETURNS).isNotEmpty()) {
                    LoopStrategy.LOOP_CONTRACT
                } else {
                    LoopStrategy.INVARIANT
                }
            } else {
                options.loopStrategy(s.loopNode)
            }
        val loopId = s.loopNode.hashCode()
        val frame = LoopFrame(
            loopId, strategy,
            loopClauses(s.loopNode, LOOP_INVARIANTS),
            if (strategy == LoopStrategy.LOOP_CONTRACT) loopClauses(s.loopNode, BREAKS) else emptyList(),
            if (strategy == LoopStrategy.LOOP_CONTRACT) loopClauses(s.loopNode, CONTINUES) else emptyList()
        )
        loopFrames.addLast(frame)
        when (strategy) {
            LoopStrategy.UNROLL -> unrollLoop(s, guard, mode, options.unrollDepth(s.loopNode), loopId)

            LoopStrategy.INVARIANT, LoopStrategy.LOOP_CONTRACT ->
                invariantLoop(s, guard, mode, loopId, strategy)
        }
        loopFrames.removeLast()
    }

    private fun unrollLoop(s: NfLoop, guard: SExpr, mode: Mode, depth: Int, loopId: Int) {
        loopContext.addLast(loopId to true)
        val entryGuard = effectiveGuard(guard, s)
        val preLoop = LinkedHashMap(env)
        // `prefix` is the path condition under which iteration i is entered.
        // Each exit point records its disjunct and the variable values at that point.
        var prefix = entryGuard
        data class Exit(val cond: SExpr, val values: Map<String, SExpr>)
        val exits = mutableListOf<Exit>()
        for (i in 0 until depth) {
            val c = atom(s.cond, prefix, mode)
            val snapshot = LinkedHashMap(env)
            exec(s.body, term.and(prefix, c), mode)
            val postBody = LinkedHashMap(env)
            val brkInBody = env[ExprTranslator.BREAK + loopId]
            // leave the loop: condition false (values = pre-iteration), or break (post-body values)
            exits.add(Exit(term.and(prefix, term.not(c)), snapshot))
            if (brkInBody != null) exits.add(Exit(term.and(prefix, c, brkInBody), postBody))
            // leave the loop via `return`: the post-body values already carry the
            // returned RESULT/RET, so record them as an exit — without this, the
            // post-loop result merge has no condition selecting the returned value
            // and the postcondition becomes unconstrained on return paths.
            val retInBody = env[ExprTranslator.RET]
            if (retInBody != null) exits.add(Exit(term.and(prefix, c, retInBody), postBody))
            // merge the taken/not-taken paths with ite-phis (needed for later iterations)
            for (key in snapshot.keys.union(env.keys)) {
                val bv = env[key]
                val sv = snapshot[key]
                if (bv != sv && bv != null && sv != null) {
                    val type = envTypes[key] ?: SmtType.INT
                    val phi = freshVersion(key, type)
                    emit(mode, prefix, equality(phi, iteTerm(c, bv, sv)))
                    env[key] = phi
                }
            }
            // reset continue flag of this loop for the next iteration
            val cont = freshVersion(ExprTranslator.CONTINUE + loopId, SmtType.BOOL)
            emit(mode, term.and(prefix, c), equality(cont, term.makeFalse()))
            // enter the next iteration only if this one was completed normally
            prefix = term.and(
                prefix, c,
                term.not(flag(ExprTranslator.BREAK + loopId)),
                term.not(flag(ExprTranslator.RET))
            )
        }
        // bounded semantics: the loop left after `depth` iterations
        val cN = atom(s.cond, prefix, mode)
        val afterLoop = LinkedHashMap(env)
        exits.add(Exit(term.and(prefix, term.not(cN)), afterLoop))
        val brkFinal = env[ExprTranslator.BREAK + loopId]
        if (brkFinal != null) exits.add(Exit(term.and(prefix, brkFinal), afterLoop))
        loopContext.removeLast()

        // final value of every modified location: ite-chain over the exit points
        val modified = modifiedLocations(s.body) + ExprTranslator.RET + ExprTranslator.RESULT +
            (ExprTranslator.BREAK + loopId) + (ExprTranslator.CONTINUE + loopId)
        for (key in modified) {
            val type = envTypes[key] ?: continue
            var acc: SExpr? = null
            // locations declared inside the loop body (e.g. `int mid = ...`) are absent
            // from pre-entry and early-exit snapshots; on those paths the value is
            // undefined, so fall back to an unconstrained constant of the right sort
            // (a boolean literal would be a sort mismatch for non-BOOL locations).
            var undef: SExpr? = null
            for (j in exits.indices.reversed()) {
                val e = exits[j]
                val v = e.values[key] ?: preLoop[key] ?: undef ?: run {
                    val name = "\$undef_${++uid}"
                    query.declareConst(name, type)
                    term.variable(type, envJavaTypes[key], name).also { undef = it }
                }
                acc = if (acc == null) v else iteTerm(e.cond, v, acc)
            }
            if (acc != null) {
                val fin = freshVersion(key, type)
                emit(mode, entryGuard, equality(fin, acc))
            }
        }

        // the exit condition (disjunction of the exit points) guards subsequent statements
        var exitCond = term.makeFalse()
        for (e in exits) exitCond = term.or(exitCond, e.cond)
        exitGuards[loopId] = term.and(entryGuard, exitCond)
    }

    /** Exit conditions collected per loop; used to extend the guard after a loop. */
    private val exitGuards = HashMap<Int, SExpr>()

    private fun invariantLoop(s: NfLoop, guard: SExpr, mode: Mode, loopId: Int, strategy: LoopStrategy) {
        val invariants = loopClauses(s.loopNode, LOOP_INVARIANTS)
        val variants = loopClauses(s.loopNode, LOOP_VARIANTS)
        val breaks = if (strategy == LoopStrategy.LOOP_CONTRACT) loopClauses(s.loopNode, BREAKS) else emptyList()
        val continues = if (strategy == LoopStrategy.LOOP_CONTRACT) loopClauses(s.loopNode, CONTINUES) else emptyList()
        if (invariants.isEmpty()) {
            throw IllegalArgumentException(
                "Loop ${describe(s.loopNode)} shall be verified with an invariant, but has none. " +
                    "Use ${LoopStrategy.UNROLL} instead."
            )
        }
        val g = effectiveGuard(guard, s)

        // (1) invariant established on entry
        var inv = term.makeTrue()
        for (e in invariants) inv = term.and(inv, translatorOf().tr(e))
        addObligation("loop invariant initially valid ${describe(s.loopNode)}", term.impl(g, inv), mode)

        // (2) one arbitrary iteration preserves the invariant (and decreases the variant)
        val snapshot = LinkedHashMap(env)
        val havocEnv = LinkedHashMap(env)
        val modified = modifiedLocations(s.body) + ExprTranslator.RET +
            (ExprTranslator.BREAK + loopId) + (ExprTranslator.CONTINUE + loopId)
        for (key in modified) {
            if (envTypes.containsKey(key)) {
                val name = "\$havoc_${++uid}"
                query.declareConst(name, envTypes[key]!!)
                havocEnv[key] = term.variable(envTypes[key]!!, envJavaTypes[key], name)
            }
        }
        // snapshot of the havoced state; needed because executing the body mutates havocEnv
        val havocBefore = LinkedHashMap(havocEnv)
        val savedEnv = env
        env = havocEnv
        val invHavoc = translatorOf(envOverride = havocEnv).let { tr ->
            var a = term.makeTrue()
            for (e in invariants) a = term.and(a, tr.tr(e))
            a
        }
        val condHavoc = atom(s.cond, term.makeTrue(), mode)
        val rec = Recording(term.and(invHavoc, condHavoc))
        loopContext.addLast(loopId to true)
        exec(s.body, term.makeTrue(), Mode.Record(rec))
        loopContext.removeLast()
        val envAfter = env
        val invAfter = translatorOf(envOverride = envAfter).let { tr ->
            var a = term.makeTrue()
            for (e in invariants) a = term.and(a, tr.tr(e))
            a
        }
        val normal = term.and(
            term.not(flagIn(envAfter, ExprTranslator.RET)),
            term.not(flagIn(envAfter, ExprTranslator.BREAK + loopId)),
            term.not(flagIn(envAfter, ExprTranslator.CONTINUE + loopId))
        )
        addObligation(
            "loop invariant preserved ${describe(s.loopNode)}",
            term.impl(normal, invAfter), Mode.Record(rec)
        )
        for (v in variants) {
            val d0 = translatorOf(envOverride = havocBefore).tr(v)
            val d1 = translatorOf(envOverride = envAfter).tr(v)
            val zero = translator.makeInt(java.math.BigInteger.ZERO)
            addObligation(
                "loop variant decreases ${describe(s.loopNode)}",
                term.impl(
                    normal,
                    term.and(term.greaterOrEquals(d0, zero, true), term.lessThan(d1, d0))
                ),
                Mode.Record(rec)
            )
        }
        env = savedEnv

        // (3) continue on the main path with a havoced, invariant-constrained state
        for (key in modified) {
            if (envTypes.containsKey(key)) {
                val name = "\$havoc_${++uid}"
                query.declareConst(name, envTypes[key]!!)
                env[key] = term.variable(envTypes[key]!!, envJavaTypes[key], name)
            }
        }
        val invMain = translatorOf().let { tr ->
            var a = term.makeTrue()
            for (e in invariants) a = term.and(a, tr.tr(e))
            a
        }
        val condMain = atom(s.cond, term.makeTrue(), mode)
        val brkMain = flag(ExprTranslator.BREAK + loopId)
        emit(mode, g, invMain)
        emit(mode, g, term.not(condMain))
        if (strategy == LoopStrategy.LOOP_CONTRACT && breaks.isNotEmpty()) {
            for (b in breaks) {
                emit(mode, term.and(g, brkMain), translatorOf().tr(b))
            }
        }
        exitGuards[loopId] =
            if (strategy == LoopStrategy.LOOP_CONTRACT) {
                term.and(g, term.or(term.not(condMain), brkMain))
            } else {
                term.and(g, term.not(condMain))
            }
    }

    private fun flagIn(e: Map<String, SExpr>, key: String): SExpr = e[key] ?: term.makeFalse()

    /** Statically collected locations assigned in a statement list. */
    private fun modifiedLocations(stmts: List<NfStmt>): Set<String> {
        val res = HashSet<String>()
        fun go(l: List<NfStmt>) {
            for (s in l) {
                when (s) {
                    is NfAssign -> res.add(locationKey(s.target))

                    is NfHavoc -> res.add(locationKey(s.location))

                    is NfIf -> {
                        go(s.thenStmts); go(s.elseStmts)
                    }

                    is NfLoop -> go(s.body)

                    is NfBlock -> go(s.stmts)

                    else -> {}
                }
            }
        }
        go(stmts)
        return res
    }
    //endregion

    //region expression flattening into the normal form
    /**
     * Flattens an expression so that every emitted constraint has the normal form
     * `v = a <op> b`, `v = <literal>` or `v = <methodcall>`; complex sub-expressions
     * are bound to fresh temporary constants. Boolean expressions are returned as
     * formulas (no temp needed).
     */
    private fun atom(e: Expression, guard: SExpr, mode: Mode): SExpr = when (e) {
        is BinaryExpr -> {
            val l = atom(e.left, guard, mode)
            val r = atom(e.right, guard, mode)
            checkDivision(e, r, guard, mode)
            checkOverflow(e, guard, mode)
            // a reference comparison against an array mixes sorts (arrays are value
            // maps and are never null): `arr == null` is false, `arr != null` is true.
            if (e.operator == BinaryExpr.Operator.EQUALS || e.operator == BinaryExpr.Operator.NOT_EQUALS) {
                val arr = if (l.smtType is SmtType.Array) {
                    l
                } else if (r.smtType is SmtType.Array) {
                    r
                } else {
                    null
                }
                val nul = if ((l as? SAtom)?.value == "null") {
                    l
                } else if ((r as? SAtom)?.value == "null") {
                    r
                } else {
                    null
                }
                if (arr != null && nul != null) {
                    return if (e.operator == BinaryExpr.Operator.EQUALS) term.makeFalse() else term.makeTrue()
                }
            }
            val res = translator.binary(e.operator, l, r)
            if (res.smtType == SmtType.BOOL) res else tempAssign(res, guard, mode, e)
        }

        is UnaryExpr -> {
            val x = atom(e.expression, guard, mode)
            val res = translator.unary(e.operator, x)
            if (res.smtType == SmtType.BOOL) res else tempAssign(res, guard, mode, e)
        }

        is ConditionalExpr -> {
            val c = atom(e.condition, guard, mode)
            val t = atom(e.thenExpr, term.and(guard, c), mode)
            val el = atom(e.elseExpr, term.and(guard, term.not(c)), mode)
            SList(t.smtType ?: el.smtType, t.javaType, listOf(term.symbol("ite"), c, t, el))
        }

        is EnclosedExpr -> atom(e.inner, guard, mode)

        is CastExpr -> atom(e.expression, guard, mode)

        is MethodCallExpr -> handleCall(e, guard, mode)

        is ObjectCreationExpr -> {
            // `new T(...)`: a fresh, unconstrained object of the (reference) sort.
            // Constructor arguments are still evaluated for their side effects.
            for (a in e.arguments) atom(a, guard, mode)
            freshTemp(SmtType.JAVA_OBJECT, tryExprType(e))
        }

        is ArrayAccessExpr -> {
            val arrayS: SExpr = atom((e as ArrayAccessExpr).name, guard, mode)
            val indexS: SExpr = atom(e.index, guard, mode)
            checkIndex(e, arrayS, indexS, guard, mode)
            val elementType = (arrayS.smtType as? SmtType.Array)?.to ?: SmtType.INT
            term.select(elementType, null, arrayS, indexS)
        }

        is NameExpr, is ThisExpr, is FieldAccessExpr, is LiteralExpr, is JmlExpression ->
            translatorOf().tr(e)

        else -> translatorOf().tr(e)
    }

    private fun tempAssign(value: SExpr, guard: SExpr, mode: Mode, e: Expression): SExpr {
        val t = freshTemp(value.smtType ?: SmtType.INT, tryExprType(e))
        emit(mode, guard, equality(t, value))
        return t
    }

    //region runtime checks (Phase 3: bounded-verification parity)
    private fun checkDivision(e: BinaryExpr, divisor: SExpr, guard: SExpr, mode: Mode) {
        if (!options.checkDivision) return
        if (e.operator != BinaryExpr.Operator.DIVIDE && e.operator != BinaryExpr.Operator.REMAINDER) return
        addObligation(
            "division by zero ${describe(e)}",
            term.impl(guard, term.not(equality(divisor, translator.makeInt(java.math.BigInteger.ZERO)))),
            mode
        )
    }

    private fun checkOverflow(e: BinaryExpr, guard: SExpr, mode: Mode) {
        if (!options.checkOverflow || options.mode != VerificationMode.BOUNDED) return
        if (e.operator != BinaryExpr.Operator.PLUS &&
            e.operator != BinaryExpr.Operator.MINUS &&
            e.operator != BinaryExpr.Operator.MULTIPLY
        ) {
            return
        }
        val fn = when (e.operator) {
            BinaryExpr.Operator.PLUS -> "bvsaddo"
            BinaryExpr.Operator.MINUS -> "bvssubo"
            else -> "bvsmulo"
        }
        addObligation(
            "arithmetic overflow ${describe(e)}",
            term.impl(guard, term.not(fnApplyOverflow(fn, e))),
            mode
        )
    }

    private fun fnApplyOverflow(fn: String, e: BinaryExpr): SExpr {
        val a = e.left.accept(translatorOf(), null)!!
        val b = e.right.accept(translatorOf(), null)!!
        // z3's bvsaddo/bvssubo/bvsmulo take exactly two bit-vector arguments; the
        // width is fixed by the sort of the arguments themselves, so a width
        // literal would be ill-sorted.
        return SList(
            SmtType.BOOL, null,
            listOf(term.symbol(fn), a, b)
        )
    }

    private fun checkIndex(e: ArrayAccessExpr, array: SExpr, index: SExpr, guard: SExpr, mode: Mode) {
        if (!options.checkIndex) return
        addObligation(
            "array bound ${describe(e)}",
            term.impl(
                guard,
                term.and(
                    term.greaterOrEquals(index, translator.makeInt(java.math.BigInteger.ZERO), true),
                    term.lessThan(index, translator.arrayLength(array))
                )
            ),
            mode
        )
    }
    //endregion

    private fun tryExprType(e: Expression): ResolvedType? = try {
        e.calculateResolvedType()
    } catch (ex: Exception) {
        null
    }
    //endregion

    //region method calls
    private fun handleCall(call: MethodCallExpr, guard: SExpr, mode: Mode): SExpr {
        val strategy = options.callStrategy(call)
        // Prefer the declaration in the same compilation unit: it carries the JML
        // contracts. (The symbol solver may re-parse the file without JML processing.)
        val decl = findDeclarationInCu(call)
            ?: try {
                call.resolve().toAst().orElse(null) as? com.github.javaparser.ast.body.MethodDeclaration
            } catch (e: Exception) {
                System.err.println("VCG: could not resolve call $call: ${e.javaClass.simpleName}: ${e.message}")
                null
            }
        if (decl == null || decl.body.isEmpty) {
            return contractCall(call, guard, mode, null)
        }
        return when (strategy) {
            CallStrategy.INLINE -> {
                if (inlineDepth < options.maxInlineDepth) inlineCall(call, guard, mode, decl) else contractCall(call, guard, mode, decl)
            }

            CallStrategy.CONTRACT -> contractCall(call, guard, mode, decl)
        }
    }

    /** Finds a method declaration with matching name and arity in the same CU. */
    private fun findDeclarationInCu(call: MethodCallExpr): com.github.javaparser.ast.body.MethodDeclaration? {
        if (call.scope.isPresent) return null
        val cu = ctx.callable.findAncestor(com.github.javaparser.ast.CompilationUnit::class.java)
            .orElse(null) ?: return null
        return cu.findAll(com.github.javaparser.ast.body.MethodDeclaration::class.java)
            .firstOrNull {
                it.nameAsString == call.nameAsString &&
                    it.parameters.size == call.arguments.size &&
                    it !== ctx.callable
            }
    }

    /**
     * Assume the precondition (against the pre-call state), havoc the assignable
     * locations, then assume the postcondition against the *post-havoc* state — the
     * `ensures` clauses of a callee see the freshly havoced fields, exactly like the
     * callee body would. For calls `o.m(...)` on an explicit receiver, the callee's
     * `this` is bound to `o`, so contract clauses referring to `this` (and the
     * `assignable this.f` clause) act on the receiver's fields.
     */
    private fun contractCall(
        call: MethodCallExpr,
        guard: SExpr,
        mode: Mode,
        decl: com.github.javaparser.ast.body.MethodDeclaration?
    ): SExpr {
        val args = call.arguments.map { atom(it, guard, mode) }
        // result variable
        val jRet = decl?.let { try { it.type.resolve() } catch (e: Exception) { null } }
        val sRet = safeType(jRet ?: tryExprType(call))
        val result = freshTemp(sRet, jRet)

        if (decl == null) {
            // unknown callee: unconstrained result, nothing havoced (unsound but best effort)
            return result
        }
        val contracts = decl.contracts
        JMLUtils.unroll(contracts)
        // merge all clauses of all (unrolled) contracts of the callee
        val joint = JmlContract()
        for (c in contracts) joint.clauses.addAll(c.clauses)

        // For `o.m(...)` the callee's `this` denotes the receiver `o`; contract names
        // `this.f` then map to the caller's `o.f` keys via [bindReceiver].
        val scope = call.scope.orElse(null)
        val receiver = if (scope != null && scope !is ThisExpr && !decl.isStatic) {
            atom(scope, guard, mode)
        } else null
        val receiverPrefix = if (receiver != null) "$scope." else null

        /** Rebinds the callee's `this` (and tracked `this.*` entries) to the receiver. */
        fun bindReceiver(map: HashMap<String, SExpr>) {
            val recv = receiver ?: return
            val prefix = receiverPrefix ?: return
            map["this"] = recv
            for ((k, v) in env) {
                if (k.startsWith(prefix)) {
                    map["this." + k.removePrefix(prefix)] = v
                }
            }
        }

        /** Maps a callee location key to the caller's key for receiver-bound calls. */
        fun callerKey(key: String): String =
            if (receiverPrefix != null && key.startsWith("this.")) {
                receiverPrefix + key.removePrefix("this.")
            } else {
                key
            }

        fun havocKey(locKey: String) {
            val target = callerKey(locKey)
            val type = envTypes[target] ?: return
            val name = "\$havoc_${++uid}"
            if (type === SmtType.JAVA_OBJECT) {
                query.addCommand(term.command("declare-const", term.symbol(name), term.symbol("U")))
            } else {
                query.declareConst(name, type)
            }
            env[target] = term.variable(type, envJavaTypes[target], name)
        }

        /** The callee view of the environment: current state + params + result + receiver. */
        fun calleeView(): HashMap<String, SExpr> {
            val m = HashMap(env)
            decl.parameters.forEachIndexed { i, p ->
                if (i < args.size) {
                    // Reference-typed parameters (arrays/objects) are passed by
                    // reference and may alias an `assignable` location that was
                    // havoced in (b): bind them to the *post-havoc* env value so the
                    // callee's `ensures` is assumed against the state the caller will
                    // observe after the call. Scalar parameters stay by value.
                    val pj = try { p.type.resolve() } catch (e: Exception) { null }
                    val byRef = pj?.isArray == true ||
                        (pj?.isReferenceType == true && envTypes[p.nameAsString] !== SmtType.INT)
                    val post = if (byRef) env[p.nameAsString] else null
                    m[p.nameAsString] = post ?: args[i]
                }
            }
            m[ExprTranslator.RESULT] = result
            bindReceiver(m)
            return m
        }

        // (a) assert the precondition against the pre-call state
        val trPre = translatorOf(envOverride = calleeView())
        for (pre in clauseExprs(joint, REQUIRES)) {
            addObligation("precondition of ${decl.nameAsString}", term.impl(guard, trPre.tr(pre)), mode)
        }
        // (b) havoc the assignable locations (in the caller's environment)
        val assignable = clauseExprs(joint, ASSIGNABLE)
        if (assignable.isEmpty() || assignable.any { it.toString().contains("everything") }) {
            // \everything or no assignable clause: havoc all fields and arrays
            for (key in env.keys.toList()) {
                if (key.startsWith("this.") || envTypes[key] is SmtType.Array) {
                    havocKey(key)
                }
            }
        } else {
            for (loc in assignable) {
                if (loc.toString().contains("nothing")) continue
                val key = when (loc) {
                    is NameExpr -> contractLocationKey(decl, loc.nameAsString)
                    is FieldAccessExpr -> Normalizer().key(loc)
                    is ArrayAccessExpr -> localKey(Normalizer().key(loc.name))
                    else -> loc.toString()
                }
                havocKey(key)
            }
        }
        // (c) assume the postcondition against the post-havoc state
        val trPost = translatorOf(envOverride = calleeView())
        for (post in clauseExprs(joint, ENSURES)) {
            emit(mode, guard, trPost.tr(post))
        }
        return result
    }

    /**
     * Inline the callee body; the result is the callee's return value. After the body
     * has run in a private copy of the environment, the callee's effects on *aliased*
     * mutable state are propagated back to the caller: field writes (`this.f`, `o.f`,
     * and receiver fields for `o.m(...)` calls, remapped to the receiver's keys) and
     * array contents. Callee-local variables and scalar parameter reassignments stay
     * local to the callee (parameters are passed by value, arrays by reference).
     */
    private fun inlineCall(
        call: MethodCallExpr,
        guard: SExpr,
        mode: Mode,
        decl: com.github.javaparser.ast.body.MethodDeclaration
    ): SExpr {
        val args = call.arguments.map { atom(it, guard, mode) }
        val calleeParams = decl.parameters.map { it.nameAsString }.toSet()
        val calleeEnv = LinkedHashMap(env)
        calleeEnv.remove(ExprTranslator.RET)
        calleeEnv.remove(ExprTranslator.RESULT)
        decl.parameters.forEachIndexed { i, p ->
            if (i < args.size) calleeEnv[p.nameAsString] = args[i]
        }
        // local declarations of the callee shadow nothing; declare its result variable
        val jRet = try { decl.type.resolve() } catch (e: Exception) { null }
        val sRet = safeType(jRet)
        val name = "\$res_${++uid}"
        query.declareConst(name, sRet)
        calleeEnv[ExprTranslator.RESULT] = term.variable(sRet, jRet, name)
        calleeEnv[ExprTranslator.RET] = term.makeFalse()

        // For `o.m(...)` the callee's `this` denotes the receiver `o`; alias tracked
        // receiver fields (`o.f`) so reads of `this.f` inside the callee see them.
        val scope = call.scope.orElse(null)
        val receiver = if (scope != null && scope !is ThisExpr && !decl.isStatic) {
            atom(scope, guard, mode)
        } else null
        val receiverPrefix = if (receiver != null) "$scope." else null
        if (receiver != null) {
            calleeEnv["this"] = receiver
            val prefix = receiverPrefix!!
            for ((k, v) in env) {
                if (k.startsWith(prefix)) calleeEnv["this." + k.removePrefix(prefix)] = v
            }
        }

        // Where the caller keeps the pre-inline state of each callee key, so effects
        // can be propagated back to the originating caller location.
        val origin = HashMap<String, String>()
        for (k in calleeEnv.keys) if (k !in calleeParams) origin[k] = k
        val aliasedParams = HashSet<String>()
        decl.parameters.forEachIndexed { i, p ->
            if (i < args.size) {
                val ck = callerKeyOf(call.arguments[i]) ?: return@forEachIndexed
                aliasedParams.add(p.nameAsString)
                origin[p.nameAsString] = ck
            }
        }

        val saved = env
        env = calleeEnv
        inlineDepth++
        val nf = Normalizer().normalize(decl.body.get())
        // keys introduced by callee-local declarations are not aliased caller state
        val calleeDeclared = HashSet<String>()
        collectCalleeDeclaredKeys(nf, calleeDeclared)
        exec(nf, guard, mode)
        inlineDepth--
        val result = env[ExprTranslator.RESULT] ?: term.variable(sRet, jRet, name)
        val retFlag = env[ExprTranslator.RET] ?: term.makeFalse()
        env = saved
        env[ExprTranslator.RESULT] = result
        envTypes[ExprTranslator.RESULT] = sRet
        env["\$callee_ret_${++uid}"] = retFlag

        // Propagate the callee's effects on aliased mutable state back to the caller.
        // The SSA constants were already declared while inlining; the caller adopts
        // the new versions under the originating caller keys.
        for ((key, value) in calleeEnv) {
            if (key == ExprTranslator.RESULT || key == ExprTranslator.RET || key.startsWith("\$")) continue
            if (key == "this" || key in calleeDeclared) continue
            if (key in calleeParams) {
                // scalars/objects are passed by value; only arrays alias the caller,
                // and only when the argument was a known caller location
                if (envTypes[key] !is SmtType.Array || key !in aliasedParams) continue
            }
            val target = when {
                receiverPrefix != null && key.startsWith("this.") -> receiverPrefix + key.removePrefix("this.")
                else -> origin[key] ?: key
            }
            val old = saved[target]
            if (old != null) {
                if (old !== value) env[target] = value
            } else if (target.contains('.')) {
                // the callee wrote a field of an object the caller also references
                if (envTypes.containsKey(key)) env[target] = value
            }
        }
        return result
    }

    /** The caller's location key an argument expression resolves to, or `null`. */
    private fun callerKeyOf(e: Expression): String? = when (e) {
        is NameExpr -> localKey(e.nameAsString)
        is FieldAccessExpr -> Normalizer().key(e)
        is ArrayAccessExpr -> localKey(Normalizer().key(e.name))
        else -> null
    }

    /**
     * Resolves a bare name in a callee's contract to a location key. A name that is a
     * field of the callee's declaring class denotes `this.<name>` (and is remapped to
     * the receiver's keys for `o.m(...)` calls); parameters stay plain names.
     */
    private fun contractLocationKey(
        decl: com.github.javaparser.ast.body.MethodDeclaration,
        name: String,
    ): String {
        val cls = decl.findAncestor(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration::class.java).orElse(null)
        val calleeField = cls?.fields?.any { f -> f.variables.any { it.nameAsString == name } } == true
        return when {
            calleeField -> "this.$name"
            decl.parameters.any { it.nameAsString == name } -> name
            else -> localKey(name)
        }
    }

    /** Collects the keys of local variables declared inside [stmts] (callee scope). */
    private fun collectCalleeDeclaredKeys(stmts: List<NfStmt>, out: MutableSet<String>) {
        for (s in stmts) {
            when (s) {
                is NfAssign -> {
                    val t = s.target
                    if (t is NfLocal && t.declaredType != null) out.add(localKey(t.key))
                }
                is NfIf -> {
                    collectCalleeDeclaredKeys(s.thenStmts, out)
                    collectCalleeDeclaredKeys(s.elseStmts, out)
                }
                is NfLoop -> collectCalleeDeclaredKeys(s.body, out)
                is NfBlock -> collectCalleeDeclaredKeys(s.stmts, out)
                is NfTryCatch -> {
                    collectCalleeDeclaredKeys(s.tryBody, out)
                    s.catches.forEach { collectCalleeDeclaredKeys(it.body, out) }
                    collectCalleeDeclaredKeys(s.finallyBody, out)
                }
                is NfSwitch -> s.cases.forEach { collectCalleeDeclaredKeys(it.body, out) }
                else -> {}
            }
        }
    }
    //endregion
}

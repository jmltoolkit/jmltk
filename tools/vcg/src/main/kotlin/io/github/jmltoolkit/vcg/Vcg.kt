/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.ArrayCreationExpr
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.CharLiteralExpr
import com.github.javaparser.ast.expr.ClassExpr
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
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
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedReferenceType
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
import java.math.BigInteger
import io.github.jmltoolkit.utils.JMLUtils
import io.github.jmltoolkit.vcg.ir.NfArray
import io.github.jmltoolkit.vcg.ir.NfAssert
import io.github.jmltoolkit.vcg.ir.NfAssign
import io.github.jmltoolkit.vcg.ir.NfAssume
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

    /** Qualified type names whose `T`-sort constant has been declared (mirrors the
     *  hierarchy declared in [setupTypeHierarchy]; extended lazily for library types). */
    private val declaredTypeSorts = LinkedHashSet<String>()
    /** Source values of string literals -> the declared reference-constant name
     *  (keyed by value so equal literals alias within one query). */
    private val stringLiteralConsts = HashMap<String, String>()
    private var stringLengthDeclared = false

    /**
     * Human-readable prefix shared by all verification-condition ids, e.g.
     * `org.example.Counter#increment(int)` (constructors use `init`). The id of a
     * single condition is `<prefix>#<kind>-<n>@<line>:<column>`.
     */
    private val vcPrefix: String by lazy {
        val classPart = try {
            ctx.enclosingType.resolve().qualifiedName
        } catch (e: Exception) {
            ctx.enclosingType.fullyQualifiedName.orElse(ctx.enclosingType.nameAsString)
        }
        val callablePart = when (val c = ctx.callable) {
            is ConstructorDeclaration -> "init"
            else -> c.nameAsString
        }
        val params = ctx.callable.parameters.joinToString(",") { p ->
            try {
                p.type.resolve().describe()
            } catch (e: Exception) {
                p.type.toString()
            }
        }
        "$classPart#$callablePart($params)"
    }

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
                "java.lang.String",
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
        declaredTypeSorts.addAll(allTypes)
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

    /**
     * Declares a `T`-sort constant for a type not declared in [setupTypeHierarchy]
     * (e.g. a library class used only as a cast/creation target) and records its
     * subtype edge to `java.lang.Object`.
     */
    private fun ensureTypeSort(qualified: String) {
        if (!declaredTypeSorts.add(qualified)) return
        val sort = typeSortName(qualified)
        query.addCommand(term.command("declare-const", term.symbol(sort), term.symbol("T")))
        query.addAssert(
            term.list(
                null, SmtType.BOOL, term.symbol("subtype"),
                term.symbol(sort), term.symbol(typeSortName("java.lang.Object"))
            )
        )
    }

    /** The arithmetic sort used for a Java `int` in the current mode. */
    private val intSort: SmtType by lazy {
        if (options.mode == VerificationMode.UNBOUNDED) {
            SmtType.INT
        } else {
            SmtType.BV32
        }
    }

    /** Declares `stringLength : U -> int` once; the model read behind `String.length()`. */
    private fun ensureStringLengthFn() {
        if (stringLengthDeclared) return
        stringLengthDeclared = true
        query.addCommand(
            term.command(
                "declare-fun", term.symbol("stringLength"),
                SList(null, null, listOf(term.symbol("U"))),
                term.type(intSort)
            )
        )
    }

    /** `(stringLength s)`, the runtime length of a `String`-like reference. The
     *  function is declared lazily at first use, so queries that never touch the
     *  string model (P3.3: `checkStringIndex` is part of the default policy) stay
     *  free of the declaration. */
    private fun stringLengthOf(s: SExpr): SExpr {
        ensureStringLengthFn()
        return term.list(null, intSort, term.symbol("stringLength"), s)
    }

    /**
     * The declared reference constant for a distinct string literal. A literal is a
     * *non-null* object (so `--check-null` on literal receivers proves) and, under
     * `--check-string-index`, its runtime length is bound. The constant is declared
     * once per literal value — repeated literals alias it instead of re-declaring.
     */
    private fun literalConst(value: String): SExpr {
        val name = stringLiteralConsts.getOrPut(value) {
            val n = "strlit_${stringLiteralConsts.size}"
            query.addCommand(term.command("declare-const", term.symbol(n), term.symbol("U")))
            val v = term.variable(SmtType.JAVA_OBJECT, null, n)
            query.addAssert(term.nonNull(v))
            if (options.checkStringIndex) {
                query.addAssert(equality(stringLengthOf(v), translator.makeInt(value.length.toLong())))
            }
            n
        }
        return term.variable(SmtType.JAVA_OBJECT, null, name)
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
        // P2.2: constant field initializers. `foldFieldConstants` folds the
        // enclosing type's initializers in declaration order (JLS 8.3.3: an
        // initializer sees only *earlier*-declared fields); the folded values are
        // then pinned to the slots at method entry where the language guarantees
        // them (`bindFieldConstant`), closing the gap where a field with a constant
        // initializer was previously unconstrained (havoced).
        val fieldConsts = foldFieldConstants()
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
                val c = fieldConsts[v.nameAsString]
                if (c != null && bindFieldConstant(field, sType)) {
                    // pin the slot's entry value: `static final` constants for every
                    // callable; for constructors the instance initializer runs before
                    // the body, and a `final` instance constant can never be rewritten
                    val valTerm = when (c) {
                        is FieldConst.Bool ->
                            if (sType === SmtType.BOOL) term.makeBoolean(c.value) else null

                        is FieldConst.Num -> fieldNumTerm(c, jType, sType)
                    }
                    if (valTerm != null) query.addAssert(equality(env[key]!!, valTerm))
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
            if (isBoxedType(returnType)) {
                throw RuntimeException(
                    "Cannot model boxed return type '${describeType(returnType)}' (auto-boxing is not supported)"
                )
            }
            val sType = safeType(returnType)
            declareSmtConst(ExprTranslator.RESULT, sType)
            env[ExprTranslator.RESULT] = term.variable(sType, returnType, ExprTranslator.RESULT)
            envTypes[ExprTranslator.RESULT] = sType
        }
        oldEnv.putAll(env)
    }

    //region constant field initializers (P2.2)
    /** A constant-folded value of a field initializer (P2.2): only JLS-style
     *  constant expressions and same-class references into earlier-declared
     *  constant fields are folded; everything else (method calls, non-constant
     *  reads, casts, forward references) stays unconstrained — the folder never
     *  approximates an unmodeled value. */
    private sealed interface FieldConst {
        /** A numeric constant. [asLong] marks an expression of JLS `long` type
         *  (operands promote to 64-bit arithmetic, JLS 5.6.2); 32-bit semantics
         *  apply otherwise. The value always lies in the signed range of its type:
         *  every intermediate operation is checked, so the folded value never
         *  wraps — it is the exact Java value (also in BOUNDED mode, where the
         *  bit-vector encoding would wrap differently). */
        data class Num(val value: BigInteger, val asLong: Boolean) : FieldConst

        data class Bool(val value: Boolean) : FieldConst
    }

    /** Folds the enclosing type's field initializers in textual declaration order.
     *  Only *earlier*-declared fields are visible to the fold (JLS 8.3.3 runs
     *  initializers textually), so a forward reference never leaks a value that
     *  Java would still see as the default. */
    private fun foldFieldConstants(): Map<String, FieldConst> {
        val consts = LinkedHashMap<String, FieldConst>()
        for (field in ctx.enclosingType.fields) {
            for (v in field.variables) {
                val init = v.initializer.orElse(null) ?: continue
                val c = foldFieldConst(init) { name -> consts[name] }
                if (c != null) consts[v.nameAsString] = c
            }
        }
        return consts
    }

    private fun foldFieldConst(e: Expression, lookup: (String) -> FieldConst?): FieldConst? =
        when (e) {
            is IntegerLiteralExpr -> intLiteralConst(e)

            is LongLiteralExpr -> longLiteralConst(e)

            is CharLiteralExpr -> FieldConst.Num(BigInteger.valueOf(e.asChar().code.toLong()), false)

            is BooleanLiteralExpr -> FieldConst.Bool(e.value)

            is UnaryExpr -> foldUnaryConst(e, lookup)

            is BinaryExpr -> foldBinaryConst(e, lookup)

            is NameExpr -> lookup(e.nameAsString)

            is EnclosedExpr -> foldFieldConst(e.inner, lookup)

            // `this.f` in an instance initializer names the same field as `f`
            is FieldAccessExpr ->
                if (e.scope is ThisExpr) lookup(e.nameAsString) else null

            else -> null
        }

    /** Parses an `int` literal with JLS 3.10.1 typing: decimal literals are
     *  non-negative and `long` when the value exceeds 2^31-1; hex/octal/binary
     *  literals fit 32 bits (two's complement) as `int`, else 64 bits as `long`. */
    private fun intLiteralConst(n: IntegerLiteralExpr): FieldConst.Num? {
        val text = n.value.replace("_", "")
        val decimal: Boolean
        val radix: Int
        val digits: String
        when {
            text.startsWith("0x", ignoreCase = true) -> {
                decimal = false; radix = 16; digits = text.substring(2)
            }

            text.startsWith("0b", ignoreCase = true) -> {
                decimal = false; radix = 2; digits = text.substring(2)
            }

            text.length > 1 && text.startsWith("0") -> {
                decimal = false; radix = 8; digits = text.substring(1)
            }

            else -> {
                decimal = true; radix = 10; digits = text
            }
        }
        val v = try { BigInteger(digits, radix) } catch (e: NumberFormatException) { return null }
        return if (decimal) {
            when {
                v <= I32_MAX -> FieldConst.Num(v, false)
                v <= I64_MAX -> FieldConst.Num(v, true)
                else -> null
            }
        } else {
            when {
                v.bitLength() <= 32 -> FieldConst.Num(twosComplement(v, 32), false)
                v.bitLength() <= 64 -> FieldConst.Num(twosComplement(v, 64), true)
                else -> null
            }
        }
    }

    /** Parses a `long` literal: the digits fit 64 bits and are interpreted in
     *  two's complement (so `0xffffffffffffffffL` is `-1L`). */
    private fun longLiteralConst(n: LongLiteralExpr): FieldConst.Num? {
        var text = n.value.replace("_", "")
        if (text.endsWith("l", ignoreCase = true)) text = text.dropLast(1)
        val radix: Int
        val digits: String
        when {
            text.startsWith("0x", ignoreCase = true) -> {
                radix = 16; digits = text.substring(2)
            }

            text.startsWith("0b", ignoreCase = true) -> {
                radix = 2; digits = text.substring(2)
            }

            text.length > 1 && text.startsWith("0") -> {
                radix = 8; digits = text.substring(1)
            }

            else -> {
                radix = 10; digits = text
            }
        }
        val v = try { BigInteger(digits, radix) } catch (e: NumberFormatException) { return null }
        if (v.bitLength() > 64) return null
        return FieldConst.Num(twosComplement(v, 64), true)
    }

    private fun foldUnaryConst(e: UnaryExpr, lookup: (String) -> FieldConst?): FieldConst? {
        val inner = foldFieldConst(e.expression, lookup) ?: return null
        return when (e.operator) {
            UnaryExpr.Operator.PLUS -> inner as? FieldConst.Num

            UnaryExpr.Operator.MINUS -> {
                val n = inner as? FieldConst.Num ?: return null
                val v = n.value.negate()
                if (fitsKind(n.asLong, v)) FieldConst.Num(v, n.asLong) else null
            }

            UnaryExpr.Operator.LOGICAL_COMPLEMENT ->
                (inner as? FieldConst.Bool)?.let { FieldConst.Bool(!it.value) }

            UnaryExpr.Operator.BITWISE_COMPLEMENT -> {
                val n = inner as? FieldConst.Num ?: return null
                val v = n.value.not()
                if (fitsKind(n.asLong, v)) FieldConst.Num(v, n.asLong) else null
            }

            else -> null
        }
    }

    private fun foldBinaryConst(e: BinaryExpr, lookup: (String) -> FieldConst?): FieldConst? {
        // `>>>` is refused: its result differs between the untouched mathematical
        // encoding (UNBOUNDED) and Java's 32-bit pattern (BOUNDED)
        if (e.operator == BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT) return null
        val l = foldFieldConst(e.left, lookup) ?: return null
        val r = foldFieldConst(e.right, lookup) ?: return null
        if (l is FieldConst.Bool && r is FieldConst.Bool) {
            return when (e.operator) {
                BinaryExpr.Operator.AND -> FieldConst.Bool(l.value && r.value)
                BinaryExpr.Operator.OR -> FieldConst.Bool(l.value || r.value)
                BinaryExpr.Operator.EQUALS -> FieldConst.Bool(l.value == r.value)
                BinaryExpr.Operator.NOT_EQUALS -> FieldConst.Bool(l.value != r.value)
                else -> null
            }
        }
        if (l !is FieldConst.Num || r !is FieldConst.Num) return null
        // binary numeric promotion (JLS 5.6.2): long wins over int/narrow
        val asLong = l.asLong || r.asLong
        val x = l.value
        val y = r.value
        fun fits(v: BigInteger) = fitsKind(asLong, v)
        fun num(v: BigInteger) = if (fits(v)) FieldConst.Num(v, asLong) else null
        return when (e.operator) {
            BinaryExpr.Operator.PLUS -> num(x.add(y))
            BinaryExpr.Operator.MINUS -> num(x.subtract(y))
            BinaryExpr.Operator.MULTIPLY -> num(x.multiply(y))
            BinaryExpr.Operator.DIVIDE -> if (y.signum() == 0) null else num(x.divide(y))
            BinaryExpr.Operator.REMAINDER -> if (y.signum() == 0) null else num(x.remainder(y))
            BinaryExpr.Operator.LEFT_SHIFT -> shiftConst(x, y, asLong, left = true)
            BinaryExpr.Operator.SIGNED_RIGHT_SHIFT -> shiftConst(x, y, asLong, left = false)
            BinaryExpr.Operator.BINARY_AND -> num(x.and(y))
            BinaryExpr.Operator.BINARY_OR -> num(x.or(y))
            BinaryExpr.Operator.XOR -> num(x.xor(y))
            BinaryExpr.Operator.LESS -> FieldConst.Bool(x < y)
            BinaryExpr.Operator.LESS_EQUALS -> FieldConst.Bool(x <= y)
            BinaryExpr.Operator.GREATER -> FieldConst.Bool(x > y)
            BinaryExpr.Operator.GREATER_EQUALS -> FieldConst.Bool(x >= y)
            BinaryExpr.Operator.EQUALS -> FieldConst.Bool(x == y)
            BinaryExpr.Operator.NOT_EQUALS -> FieldConst.Bool(x != y)
            else -> null
        }
    }

    /** Java `<<` / `>>` on a constant: the shift distance is masked to 31 (int) or
     *  63 (long, JLS 15.19), and the exact mathematical result is kept only when
     *  it still fits the operand's signed range — a wrapping shift like `1 << 31`
     *  is left unconstrained instead of being pinned to a mode-dependent value. */
    private fun shiftConst(x: BigInteger, y: BigInteger, asLong: Boolean, left: Boolean): FieldConst.Num? {
        val mask = if (asLong) 63 else 31
        val n = (y.longValueExact() and mask.toLong()).toInt()
        val v = if (left) x.shiftLeft(n) else x.shiftRight(n)
        return if (fitsKind(asLong, v)) FieldConst.Num(v, asLong) else null
    }

    private fun fitsKind(asLong: Boolean, v: BigInteger): Boolean =
        if (asLong) v in I64_MIN..I64_MAX else v in I32_MIN..I32_MAX

    /** Two's-complement interpretation of a [bits]-bit pattern ([v] is non-negative). */
    private fun twosComplement(v: BigInteger, bits: Int): BigInteger {
        val width = BigInteger.ONE.shiftLeft(bits)
        return if (v.testBit(bits - 1)) v.subtract(width) else v
    }

    /**
     * Whether a folded field constant is bound at method entry. The entry value
     * is only pinned where *every* legal execution reaches the entry state with
     * that value:
     *  - `static final` constants hold for any callable (the class initializer
     *    runs before every use and `final` prevents later writes);
     *  - an instance constant initializer holds inside a constructor body (JLS
     *    12.5: initializers run before the body), but *not* in an arbitrary
     *    instance method, which may run on an object built by a constructor that
     *    overwrote the field — except a `final` field, which no constructor can
     *    rewrite, so its initializer value holds for every instance;
     *  - mutable static fields are never bound (another method may have written
     *    them before entry), and neither are nested non-`final`-instance cases.
     */
    private fun bindFieldConstant(field: com.github.javaparser.ast.body.FieldDeclaration, sType: SmtType): Boolean {
        if (sType === SmtType.JAVA_OBJECT) return false // reference slots stay unconstrained
        return when {
            field.isStatic -> field.isFinal
            ctx.callable is ConstructorDeclaration -> true
            field.isFinal -> !ctx.callable.isStatic
            else -> false
        }
    }

    /** The SMT literal for a folded numeric constant, matching the slot's sort;
     *  `null` when the value cannot be represented in the field's declared type
     *  (defensive: valid Java never produces such a constant initializer). */
    private fun fieldNumTerm(c: FieldConst.Num, jType: ResolvedType?, sType: SmtType?): SExpr? {
        val p = jType as? ResolvedPrimitiveType ?: return null
        val inRange = when (p) {
            ResolvedPrimitiveType.BYTE -> c.value in I8_MIN..I8_MAX
            ResolvedPrimitiveType.SHORT -> c.value in I16_MIN..I16_MAX
            ResolvedPrimitiveType.CHAR -> c.value >= BigInteger.ZERO && c.value <= U16_MAX
            ResolvedPrimitiveType.INT -> c.value in I32_MIN..I32_MAX
            ResolvedPrimitiveType.LONG -> c.value in I64_MIN..I64_MAX
            else -> false
        }
        if (!inRange) return null
        return when (sType) {
            is SmtType.BitVec -> term.makeBitvector(sType.width, c.value)
            SmtType.INT -> term.makeInt(c.value.toString())
            else -> null
        }
    }
    //endregion

    private fun isBoxedType(t: ResolvedType): Boolean {
        val id = try {
            (t as? com.github.javaparser.resolution.types.ResolvedReferenceType)?.qualifiedName
        } catch (e: Exception) {
            null
        }
        return id in setOf(
            "java.lang.Integer", "java.lang.Long", "java.lang.Boolean", "java.lang.Character",
            "java.lang.Short", "java.lang.Byte", "java.lang.Float", "java.lang.Double"
        )
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

    private fun describeType(t: ResolvedType): String = try {
        t.describe()
    } catch (e: Exception) {
        t.toString()
    }

    private fun resolveType(t: com.github.javaparser.ast.type.Type): ResolvedType? = try {
        t.resolve()
    } catch (e: Exception) {
        null
    }

    private fun safeType(jType: ResolvedType?): SmtType {
        val t = try {
            // fall back to the mode's `int` encoding: BV32 in BOUNDED, Int in
            // UNBOUNDED — the raw SmtType.INT sort is ill-sorted next to BV32
            if (jType != null) translator.getType(jType) else translator.getType(ResolvedPrimitiveType.INT)
        } catch (e: Exception) {
            translator.getType(ResolvedPrimitiveType.INT)
        }
        if (t is SmtType.Array) ensureLengthFn(t)
        return t
    }

    /**
     * Java numeric conversion of [v] to the bit-vector width of [target]
     * (JLS 5.1.2 widening / 5.1.3 narrowing): widening sign-extends — `char`
     * zero-extends, it is unsigned — and narrowing keeps the low-order bits, the
     * Java wrap-around (`(byte) 300 == 44`). Non-bit-vector targets (booleans,
     * references, arrays) and already-matching widths pass through unchanged.
     */
    private fun convertTo(v: SExpr, target: SmtType?): SExpr {
        if (target !is SmtType.BitVec) return v
        val vw = (v.smtType as? SmtType.BitVec)?.width ?: return v
        if (vw == target.width) return v
        return if (vw > target.width) {
            term.extractLow(v, target.width)
        } else {
            val isChar = (v.javaType as? ResolvedPrimitiveType) === ResolvedPrimitiveType.CHAR
            if (isChar) term.zeroExtend(v, target.width) else term.signExtend(v, target.width)
        }
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

    /** Per-case requires terms, translated against the *entry* (pre-state)
     *  environment in [assumeRequires] and reused as the per-case guards in
     *  [checkPostcondition], so the guards always speak about the values that held
     *  at method entry (never the post-body, post-havoc state). */
    private var entryCaseRequires: List<List<SExpr>> = emptyList()

    private fun assumeRequires() {
        // Multiple spec cases are disjunctive (JML): the method is only obliged to be
        // well-behaved when the caller picks some case, so the entry assumption is the
        // disjunction of the per-case requires (`R_1 \/ R_2 \/ ...`); a spec case
        // without a requires clause is `true` and makes the disjunction trivial. The
        // per-case postcondition obligations are then guarded by their own requires
        // (see [checkPostcondition]).
        entryCaseRequires = ctx.contracts.map { case ->
            clauseExprs(case, REQUIRES).map { translatorOf(it).tr(it) }
        }
        // a spec case without a requires clause is `true` and makes the disjunction
        // trivial (no legal caller can violate it), so only assert when some case
        // actually constrains the entry state.
        val constrained = entryCaseRequires
            .mapNotNull { r -> if (r.isEmpty()) null else r.reduce { a, b -> term.and(a, b) } }
        if (constrained.isNotEmpty()) {
            query.addAssert(constrained.reduce { a, b -> term.or(a, b) })
        }
        assumeClassInvariant()
    }

    /**
     * JML entry semantics: a non-helper instance method may assume the
     * statically-visible class invariants of the enclosing instance
     * (`\invariant_for(this)`). Constructors assume nothing (fields are not yet
     * initialized) and static methods have no enclosing instance.
     */
    private fun assumeClassInvariant() {
        if (isHelper || ctx.callable.isStatic || ctx.callable is ConstructorDeclaration) return
        invariantForThisTerm()?.let { query.addAssert(it) }
    }

    private fun checkPostcondition(guard: SExpr) {
        // JML visible-state semantics: a non-helper instance method (like every
        // constructor, which must *establish* the invariants) preserves the class
        // invariants — `\invariant_for(this)` holds at method exit — so the folded
        // invariant joins the postcondition it must satisfy.
        val invariant =
            if (!isHelper && !ctx.callable.isStatic) invariantForThisTerm() else null
        val callable = ctx.callable
        val m = callable as? MethodDeclaration
        val isVoid = m == null || m.type.toString() == "void"
        val ret = flag(ExprTranslator.RET)
        // Multiple spec cases are verified *separately*: for case i the obligation is
        // `guard /\ requires_i (/\ ret) ==> ensures_i`, i.e. the callee must satisfy
        // every case whenever that case's own requires held at entry. A case without
        // requires must always hold.
        for ((i, case) in ctx.contracts.withIndex()) {
            var post = term.makeTrue()
            var hasPost = false
            for (e in clauseExprs(case, ENSURES)) {
                post = term.and(post, translatorOf(e).tr(e))
                hasPost = true
            }
            if (invariant != null) {
                post = term.and(post, invariant)
                hasPost = true
            }
            if (!hasPost) continue
            var ante = guard
            for (r in entryCaseRequires.getOrElse(i) { emptyList() }) {
                ante = term.and(ante, r)
            }
            if (!isVoid) ante = term.and(ante, ret)
            addObligation("postcondition", "postcondition", term.impl(ante, post), Mode.Real, callable)
        }
    }

    /** `helper` methods neither assume nor preserve the class invariants. */
    private val isHelper: Boolean by lazy {
        try {
            ctx.callable.hasModifier(Modifier.DefaultKeyword.JML_HELPER)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * The folded, translated `\invariant_for(this)` of the enclosing instance —
     * the statically-visible class invariants, joined by [InvariantFold.fold] —
     * or `null` when no invariant is in scope.
     */
    private fun invariantForThisTerm(): SExpr? {
        val invs = InvariantFold.invariantsOf(ctx.enclosingType)
        if (invs.isEmpty()) return null
        return translatorOf().tr(InvariantFold.fold(invs))
    }

    /**
     * Expands a literal `\invariant_for(...)` / `\static_invariant_for(...)`
     * expression: the statically-visible invariants of the argument's class are
     * gathered and folded, then translated against the current state. A non-`this`
     * receiver is bound as the invariants' `this` (`\invariant_for(o)` evaluates
     * `o`'s invariants about `o`). Recognized names always expand — an unresolved
     * target folds to `true` (vacuously) rather than being misread as a call.
     */
    private fun invariantForTerm(receiver: SExpr?, receiverExpr: Expression?, isStatic: Boolean): SExpr {
        val type = targetTypeForInvariant(receiver, receiverExpr) ?: return term.makeTrue()
        // `\static_invariant_for` folds only the static invariants: the instance
        // clauses read `this`, which is not declared inside a static method.
        val fold = InvariantFold.fold(InvariantFold.invariantsOf(type, staticOnly = isStatic))
        val bound =
            if (receiverExpr is ThisExpr || (receiver != null && receiver === env["this"])) {
                null
            } else if (receiver != null) {
                bindInvariantReceiver(receiver, type)
            } else {
                null
            }
        return (if (bound == null) translatorOf() else translatorOf(envOverride = bound)).tr(fold)
    }

    /** The type whose invariants a `\invariant_for`/`\static_invariant_for`
     *  argument denotes, or `null` when it cannot be established. The receiver
     *  term's resolved type wins (a callee-view `this` may be a bound argument);
     *  a literal `this` falls back to the enclosing type; a `ClassExpr`
     *  (`\static_invariant_for(T.class)`) resolves its class type. */
    private fun targetTypeForInvariant(receiver: SExpr?, receiverExpr: Expression?): TypeDeclaration<*>? {
        val qn = try {
            (receiver?.javaType as? ResolvedReferenceType)?.qualifiedName
        } catch (e: Exception) {
            null
        }
        if (qn != null) return findTypeInCu(qn)
        if (receiverExpr is ThisExpr) return ctx.enclosingType
        val qn2 = when (receiverExpr) {
            is ClassExpr -> try {
                receiverExpr.type.resolve().asReferenceType().qualifiedName
            } catch (e: Exception) {
                null
            }

            else -> try {
                receiverExpr?.calculateResolvedType()?.asReferenceType()?.qualifiedName
            } catch (e: Exception) {
                null
            }
        }
        if (qn2 != null) findTypeInCu(qn2)?.let { return it }
        return if (receiver != null && receiver === env["this"]) ctx.enclosingType else null
    }

    /** Finds the class declaration of qualified name [qn] in the verification CU. */
    private fun findTypeInCu(qn: String): TypeDeclaration<*>? {
        val cu = ctx.callable.findAncestor(CompilationUnit::class.java).orElse(null) ?: return null
        return cu.findAll(ClassOrInterfaceDeclaration::class.java).firstOrNull {
            try {
                it.resolve().qualifiedName == qn
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Receiver-bound view of the environment for translating `\invariant_for(o)`:
     * `this` denotes `o`, and every scalar/object field of `o`'s class that lives
     * in an identity-keyed field map is exposed again as `this.<field>` (select of
     * the map at `o`), so bare-field and `this.f` reads in the invariants observe
     * the receiver's map entry. Fields without a live map stay unconstrained —
     * sound, since no fact is then assumed.
     */
    private fun bindInvariantReceiver(receiver: SExpr, type: TypeDeclaration<*>): Map<String, SExpr> {
        val m = HashMap(env)
        m["this"] = receiver
        m.keys.removeAll { it.startsWith("this.") }
        val cls = type as? ClassOrInterfaceDeclaration ?: return m
        for (fd in cls.fields) {
            for (v in fd.variables) {
                val fname = v.nameAsString
                val sType = try {
                    envTypes["this.$fname"] ?: safeType(resolveType(v.type))
                } catch (e: Exception) {
                    continue
                }
                if (sType is SmtType.Array) continue
                val mv = env[fieldMapKey(fname, sType)] ?: continue
                m["this.$fname"] = term.select(sType, null, mv, receiver)
            }
        }
        return m
    }

    private fun emitVcs() {
        for ((i, vc) in obligations.withIndex()) {
            query.push()
            val named = SList(
                SmtType.COMMAND, null,
                listOf(
                    term.symbol("!"), term.not(vc.obligation),
                    // the `:named` symbol is SMT-safe and positional: the human-readable
                    // condition id lives on VerificationCondition and must not leak into
                    // the query (it would both break SMT-LIB symbol syntax and make the
                    // solver-result cache depend on names instead of content)
                    term.symbol(":named"), term.symbol("v${i + 1}")
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
        // P2.2: primitive value ranges for the constant-field folder
        val I8_MIN = BigInteger.valueOf(Byte.MIN_VALUE.toLong())
        val I8_MAX = BigInteger.valueOf(Byte.MAX_VALUE.toLong())
        val I16_MIN = BigInteger.valueOf(Short.MIN_VALUE.toLong())
        val I16_MAX = BigInteger.valueOf(Short.MAX_VALUE.toLong())
        val U16_MAX = BigInteger.valueOf(0xFFFFL)
        val I32_MIN = BigInteger.valueOf(Int.MIN_VALUE.toLong())
        val I32_MAX = BigInteger.valueOf(Int.MAX_VALUE.toLong())
        val I64_MIN = BigInteger.valueOf(Long.MIN_VALUE)
        val I64_MAX = BigInteger.valueOf(Long.MAX_VALUE)

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
            fieldAccessHandler = { receiver, field, scopeText -> fieldReadFor(e1, receiver, field, scopeText) },
            thisFieldRead = { m, thisVal, field -> fieldReadFor(m, thisVal, field, "this") },
            arrayRefHandler = { v -> arrayRef(v) },
            invariantForHandler = { recv, recvExpr, isStatic -> invariantForTerm(recv, recvExpr, isStatic) }
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
            // `this`-receiver reads go through the identity-keyed field map whenever
            // it is live, so a write through an alias of `this` (`Box b = this;
            // b.value = 5`) is observable as `this.value`; otherwise the flat
            // per-path slot (also the `\old` view, which never carries map keys).
            val (jType, sType) = fieldSelectorType(thisRepr, "this", field)
            val activeMap = activeEnv[fieldMapKey(field, sType)]
            if (activeMap != null) return term.select(sType, jType, activeMap, thisRepr)
            return activeEnv["this.$field"] ?: declareUnknownField(field)
        }
        // The field's SMT sort must come from the *receiver's* type (not the
        // enclosing class): an `int` field of a referenced object is a bit-vector
        // in BOUNDED mode, so a selector typed as `Int` would be ill-sorted.
        val (jType, sType) = fieldSelectorType(receiver, scopeText, field)
        if (sType is SmtType.Array) {
            // array-typed fields keep the per-path/selector model (whole-array SSA)
            val versioned = activeEnv["$scopeText.$field"]
            if (versioned != null) return versioned
            val fn = fieldSelector(field, sType, jType)
            return term.list(jType, sType, term.symbol(fn), receiver)
        }
        // scalar/object fields of arbitrary receivers: read through the versioned
        // identity-keyed field map (alias-correct). A per-receiver slot (created by
        // contract-call assumptions or, historically, per-path writes) is preferred
        // only when the map is not live in the active view — e.g. `\old(o.f)` over
        // a flat-written field.
        val mapKey = fieldMapKey(field, sType)
        val legacy = activeEnv["$scopeText.$field"]
        val activeMap = activeEnv[mapKey]
        if (activeMap != null && legacy == null) {
            return term.select(sType, jType, activeMap, receiver)
        }
        if (legacy != null) return legacy
        if (env[mapKey] == null) declareFieldMap(mapKey, sType)
        return term.select(sType, jType, env[mapKey]!!, receiver)
    }

    /** Field-read term for `scope.name` given an already-translated [scopeTerm]; mirrors
     *  [io.github.jmltoolkit.vcg.ExprTranslator.visit] for `FieldAccessExpr`. */
    private fun fieldRead(scope: Expression, e: FieldAccessExpr, scopeTerm: SExpr): SExpr {
        if (e.nameAsString == "length") return translator.arrayLength(scopeTerm)
        fieldReadFor(env, scopeTerm, e.nameAsString, scope.toString()).let { return it }
    }

    /** Resolves the Java/SMT type of `scope.field` from the resolved receiver type. */
    private fun fieldSelectorType(
        receiver: SExpr, scopeText: String, field: String
    ): Pair<ResolvedType?, SmtType> {
        val thisJ = if (scopeText == "this") envJavaTypes["this.$field"] else null
        if (thisJ != null) return thisJ to (envTypes["this.$field"] ?: safeType(thisJ))
        val resolved = receiver.javaType
        val fieldJType = (resolved as? com.github.javaparser.resolution.types.ResolvedReferenceType)
            ?.getFieldType(field)?.orElse(null)
        if (fieldJType != null) return fieldJType to safeType(fieldJType)
        // last resort: mode-appropriate scalar default
        val default = if (options.mode == VerificationMode.BOUNDED) SmtType.BV32 else SmtType.INT
        return null to (if (scopeText == "this") envTypes["this.$field"] ?: default else default)
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
        val sType = envTypes[key] ?: if (options.mode == VerificationMode.BOUNDED) SmtType.BV32 else SmtType.INT
        query.declareConst(key, sType)
        val v = term.variable(sType, envJavaTypes[key], key)
        env[key] = v
        envTypes[key] = sType
        return v
    }

    private fun declareUnknown(name: String): SExpr {
        val key = unknownKey(name)
        val sType = envTypes[key] ?: if (options.mode == VerificationMode.BOUNDED) SmtType.BV32 else SmtType.INT
        query.declareConst(key, sType)
        val v = term.variable(sType, envJavaTypes[key], key)
        env[key] = v
        envTypes[key] = sType
        return v
    }

    private fun unknownKey(name: String): String =
        if (env.containsKey("this.$name")) "this.$name" else "unk_" + name
    //endregion

    //region reference identity (must-alias support)

    /**
     * Versioned map-typed locations whose SSA values are `(Array U <elem>)` heap
     * maps (instance fields of arbitrary receivers). Keys start with `fld$` so the
     * array-store frame loop ([execAssign]) never mistakes a field map for a Java
     * array value.
     */
    private fun isFieldMapKey(key: String): Boolean = key.startsWith("fld$")

    private val arrayRefFns = HashMap<String, String>()

    /**
     * Declares (once per element sort) and returns the `$arrayref_…` function whose
     * application to an array *value* yields the *reference identity* of that array
     * (a `U` constant). Java `==` on arrays compares identities — never contents —
     * and every element store to an array value `v1` is visible through any other
     * array value `v2` whose identity is provably equal. Identity is a plain
     * equality-theory sort, so the alias frames below are decided by the solver
     * without extensional (content) reasoning.
     */
    private fun arrayRefFn(contentType: SmtType.Array): String {
        val tag = contentType.toString().filter { ch: Char -> ch.isLetterOrDigit() }
        return arrayRefFns.getOrPut(tag) {
            val fn = "\$arrayref_$tag"
            query.addCommand(
                term.command(
                    "declare-fun", term.symbol(fn),
                    SList(null, null, listOf(term.type(contentType))),
                    term.symbol("U")
                )
            )
            fn
        }
    }

    /** The reference identity of an array-valued [SExpr], or `null` for non-arrays. */
    private fun arrayRef(v: SExpr): SExpr? {
        val arr = v.smtType as? SmtType.Array ?: return null
        if (v.toString().startsWith("fld$")) return null
        return term.list(null, SmtType.JAVA_OBJECT, term.symbol(arrayRefFn(arr)), v)
    }

    //region versioned identity-keyed field maps

    /**
     * Identity-keyed (versioned) field maps make writes to a scalar/object field of
     * an *arbitrary* receiver visible through every other reference that provably
     * denotes the same object (`b2 = b; b2.value = 5` ⇒ `b.value == 5`). For a field
     * `name` of SMT sort `elem`, the map value is a `(Array U <elem>)` heap map,
     * SSA-versioned under the key `fld$<tag>.<name>` exactly like a local variable:
     * a write is `map' = ite(g, store(map, receiver, v), map)`, a read is
     * `(select map receiver)`. Distinct objects (distinct `U` constants) never
     * observe each other's stores; the alias decision is left to the solver on `U`.
     *
     * `this`-receiver fields, array-typed fields and static fields keep the flat
     * per-path model — this model complements it for receivers that resolve to an
     * environment term (`params`, locals, `this.<field>` objects).
     */
    private fun fieldMapKey(field: String, elem: SmtType): String {
        val tag = when {
            elem === SmtType.JAVA_OBJECT -> "obj"
            elem === SmtType.INT -> "int"
            elem === SmtType.BOOL -> "bool"
            elem === SmtType.BV32 -> "bv32"
            else -> "t_" + elem.toString().filter { it.isLetterOrDigit() }
        }
        return "fld$$tag.$field"
    }

    /** The declared SMT sort `(Array U <elem>)` of a field map. */
    private fun fieldMapSort(elem: SmtType): SExpr = SList(
        null, null,
        listOf(
            term.symbol("Array"), term.symbol("U"),
            if (elem === SmtType.JAVA_OBJECT) term.symbol("U") else term.type(elem)
        )
    )

    private fun fieldMapType(elem: SmtType): SmtType = SmtType.Array(SmtType.JAVA_OBJECT, elem)

    /** Declares the base (unconstrained) version of a field map into the current env. */
    private fun declareFieldMap(mapKey: String, elem: SmtType) {
        query.addCommand(term.command("declare-const", term.symbol(mapKey), fieldMapSort(elem)))
        val t = fieldMapType(elem)
        env[mapKey] = term.variable(t, null, mapKey)
        envTypes[mapKey] = t
        // Anchor the base map's entry at `this` to the flat `this.<field>` slot so a
        // field read on an alias of `this` that issued before the map became live
        // (requires clauses, entry values) keeps its value once reads switch to the
        // map: `select(base, this)` equals the flat entry until a write through a
        // provably aliasing receiver actually stores into `this`'s slot.
        val field = mapKey.substringAfterLast('.')
        val thisRepr = env["this"]
        val flat = env["this.$field"]
        if (thisRepr != null && flat != null && thisRepr.smtType === SmtType.JAVA_OBJECT) {
            query.addAssert(equality(term.select(elem, null, env[mapKey]!!, thisRepr), flat))
        }
    }

    /** Next SSA version of a field map, declared with the printable `(Array U …)` sort. */
    private fun freshFieldMapVersion(mapKey: String, elem: SmtType): SExpr {
        if (env[mapKey] == null) declareFieldMap(mapKey, elem)
        val name = nextName(mapKey)
        query.addCommand(term.command("declare-const", term.symbol(name), fieldMapSort(elem)))
        val t = fieldMapType(elem)
        val v = term.variable(t, null, name)
        env[mapKey] = v
        envTypes[mapKey] = t
        return v
    }

    /** [freshVersion] that knows the field-map keys (their sort cannot be printed by
     *  the generic `SmtType.Array` printer, which would emit the `Object` placeholder
     *  for the reference index). */
    private fun freshVersionFor(key: String, type: SmtType): SExpr =
        if (isFieldMapKey(key) && type is SmtType.Array) freshFieldMapVersion(key, type.to) else freshVersion(key, type)

    /** Declares a fresh havoc constant of [type] for [key] using the printable sort. */
    private fun declareHavocConst(key: String, type: SmtType): SExpr {
        val name = "\$havoc_${++uid}"
        if (isFieldMapKey(key) && type is SmtType.Array) {
            query.addCommand(term.command("declare-const", term.symbol(name), fieldMapSort(type.to)))
        } else if (type === SmtType.JAVA_OBJECT) {
            query.addCommand(term.command("declare-const", term.symbol(name), term.symbol("U")))
        } else {
            query.declareConst(name, type)
        }
        val v = term.variable(type, envJavaTypes[key], name)
        env[key] = v
        return v
    }

    /** Strong update `map[receiver] := v` with phi-style framing by [g]. */
    private fun fieldMapStore(field: String, elem: SmtType, recvTerm: SExpr, v: SExpr, g: SExpr, mode: Mode) {
        val mapKey = fieldMapKey(field, elem)
        if (env[mapKey] == null) declareFieldMap(mapKey, elem)
        val prevMap = env[mapKey]!!
        val nextMap = freshFieldMapVersion(mapKey, elem)
        emitPhi(mode, equality(nextMap, iteTerm(g, term.store(prevMap, recvTerm, v), prevMap)))
    }
    //endregion

    /** Declares a constant of [type] using the encoding's `U` sort for references
     *  (a bare `SmtType.JAVA_OBJECT` would print the placeholder sort `Object`). */
    private fun declareSmtConst(name: String, type: SmtType) {
        if (type === SmtType.JAVA_OBJECT) {
            query.addCommand(term.command("declare-const", term.symbol(name), term.symbol("U")))
        } else {
            query.declareConst(name, type)
        }
    }

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

    private fun addObligation(kind: String, description: String, prop: SExpr, mode: Mode, node: com.github.javaparser.ast.Node? = null) {
        val ob =
            when (mode) {
                is Mode.Real -> prop

                is Mode.Record -> {
                    var premise = mode.rec.premise
                    for (c in mode.rec.conjuncts) premise = term.and(premise, c)
                    term.impl(premise, prop)
                }
            }
        val number = obligations.size + 1
        val loc = node?.range?.orElse(null)
        val id = "$vcPrefix#$kind-$number@${loc?.begin?.line ?: "-"}:${loc?.begin?.column ?: "-"}"
        obligations.add(VerificationCondition(id, description, ob, loc, kind))
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
                return exitGuards[s.loopId] ?: guard
            }

            is NfThrow -> execThrow(s, guard, mode)

            is NfTryCatch -> return execTryCatch(s, guard, mode)

            is NfSwitch -> execSwitch(s, guard, mode)

            is NfReturn -> execReturn(s, guard, mode)

            is NfBreak -> {
                val g = effectiveGuard(guard, s)
                setFlag(ExprTranslator.BREAK + s.loopId, g, mode)
                // loop contract: the breaks clause holds when the loop is left via break
                val f = loopFrames.lastOrNull { it.loopId == s.loopId }
                if (f != null && f.strategy != LoopStrategy.UNROLL && f.breaks.isNotEmpty()) {
                    var clause = term.makeTrue()
                    for (b in f.breaks) clause = term.and(clause, translatorOf().tr(b))
                    addObligation("break-clause", "loop breaks clause ${describe(s.origin!!)}", term.impl(g, clause), mode)
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
                        addObligation("continue-clause", "loop continues clause ${describe(s.origin!!)}", term.impl(g, clause), mode)
                    }
                    // a continue reaches the loop head, so the invariant must hold here
                    if (f.invariants.isNotEmpty()) {
                        var inv = term.makeTrue()
                        for (e in f.invariants) inv = term.and(inv, translatorOf().tr(e))
                        addObligation("loop-invariant-continue", "loop invariant at continue ${describe(s.origin!!)}", term.impl(g, inv), mode)
                    }
                }
            }

            is NfAssume -> emit(mode, effectiveGuard(guard, s), translatorOf().tr(s.expr))

            is NfAssert -> addObligation(
                "assert",
                "assert ${s.origin?.let { describe(it) } ?: ""}",
                term.impl(effectiveGuard(guard, s), translatorOf().tr(s.expr)), mode,
                s.origin
            )

            is NfHavoc -> {
                // A local declared without initializer must get the sort of its
                // *declared* type (e.g. BV32 in BOUNDED mode), not the Int fallback —
                // otherwise later assignments mix Int and bit-vector sorts.
                val key = locationKey(s.location)
                val declared = (s.location as? NfLocal)?.declaredType?.let { tryResolve(it) }
                val type = envTypes[key] ?: declared?.let { safeType(it) } ?: locType(s.location)
                freshVersion(key, type)
            }
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

    private val loopContext = ArrayDeque<Pair<Int, Boolean>>() // (loopId, isSwitch?)

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
        // a `continue` never targets a switch (the normalizer only pushes loop ids on
        // the continue stack); skip switch-like entries pushed by execSwitch
        loopContext.lastOrNull { !it.second }?.first

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
        // exception value (fresh) and thrown flag, bound phi-style so a throw under a
        // dead guard keeps previous values instead of leaving fresh constants unconstrained
        val prevEv = env[ExprTranslator.EXCVAL] ?: term.makeNull()
        val ev = freshVersion(ExprTranslator.EXCVAL, SmtType.JAVA_OBJECT)
        emitPhi(mode, equality(ev, iteTerm(g, v, prevEv)))
        val prevExc = env[ExprTranslator.EXC] ?: term.makeFalse()
        val exc = freshVersion(ExprTranslator.EXC, SmtType.BOOL)
        emitPhi(mode, equality(exc, iteTerm(g, term.makeTrue(), prevExc)))
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
        val preTry = LinkedHashMap(env)

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
                    // phi-style merge: `phi = ite(resumeGuard, ite(handled, handledV, normalV), preTry)`
                    // so a dead/unresumed path (unhandled exception, or a try that is
                    // itself under a false guard) keeps the pre-try value instead of
                    // leaving `phi` as an unconstrained constant.
                    val pre = preTry[key] ?: normalV
                    emitPhi(mode, equality(phi, iteTerm(resumeGuard, iteTerm(handled, handledV, normalV), pre)))
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
        val prevExc = env[ExprTranslator.EXC] ?: term.makeFalse()
        val excFresh = freshVersion(ExprTranslator.EXC, SmtType.BOOL)
        emitPhi(mode, equality(excFresh, iteTerm(resumeGuard, term.makeFalse(), prevExc)))
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
            "String",
            -> "java.lang.$name"

            else -> name
        }
    }

    /**
     * Lowers a `switch` statement with Java fall-through semantics. The selector is
     * evaluated exactly once, then each case group is executed under the guard
     * "the switch was already entered (fall through) OR this group's label matches
     * the selector" (the `default` group enters iff no label matched anywhere).
     * A `break` inside a case is encoded as a switch-scoped completion flag that
     * makes the remaining groups unreachable; a `return`/`throw` behaves as usual.
     * After the switch the break flag is reset so an *enclosing* unrolled loop can
     * re-enter the same switch node in its next iteration.
     */
    private fun execSwitch(s: NfSwitch, guard: SExpr, mode: Mode) {
        val g = effectiveGuard(guard, s)
        val sel = atom(s.selector, g, mode)
        val selfMatches = s.cases.map { c ->
            c.labels.fold(term.makeFalse()) { acc, label ->
                term.or(acc, equality(sel, translatorOf().tr(label)))
            }
        }
        // The selector matched *some* labelled group of this switch. Constant for the
        // whole chain (the selector is evaluated once and labels are compile-time
        // constants), so the default group can decide up front whether to fire.
        val anyMatch = selfMatches.fold(term.makeFalse()) { acc, m -> term.or(acc, m) }
        loopContext.addLast(s.switchId to true)
        var entered: SExpr = term.makeFalse()
        for ((i, c) in s.cases.withIndex()) {
            // labelled group: enters when the selector equals one of its labels or a
            // previous group already started (fall through); default: enters when no
            // label matched anywhere
            val enterNow = if (c.labels.isEmpty()) term.not(anyMatch) else selfMatches[i]
            val cond = term.or(entered, enterNow)
            val condG = term.and(g, cond)
            exec(c.body, condG, mode)
            // phi-style: `enteredNext = ite(condG, true, entered)`; folding the guard
            // in keeps the flag false on paths where the whole switch is dead
            val enteredNext = freshTemp(SmtType.BOOL)
            emitPhi(mode, equality(enteredNext, iteTerm(condG, term.makeTrue(), entered)))
            entered = enteredNext
        }
        loopContext.removeLast()
        // reset the switch-scoped break flag so it does not leak into the next
        // iteration of an enclosing unrolled loop (this switch node runs unchanged)
        val prevBrk = env[ExprTranslator.BREAK + s.switchId] ?: term.makeFalse()
        val brkFresh = freshVersion(ExprTranslator.BREAK + s.switchId, SmtType.BOOL)
        emitPhi(mode, equality(brkFresh, iteTerm(g, term.makeFalse(), prevBrk)))
    }

    private fun execReturn(s: NfReturn, guard: SExpr, mode: Mode) {
        val g = effectiveGuard(guard, s)
        if (s.value != null && envTypes.containsKey(ExprTranslator.RESULT)) {
            // capture the previous value *before* freshVersion renames the env slot
            val prev = env[ExprTranslator.RESULT]
            // a `byte`/`short`/`char` value returned from an int-typed method is
            // converted (widened) to the declared return type; a value returned
            // from a narrow-typed method is truncated to it.
            val v = convertTo(atom(s.value, g, mode), envTypes[ExprTranslator.RESULT])
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
        // the stored value is shared by every target kind below (a scalar/object
        // field store and the flat per-path write must observe one translation of
        // the right-hand side — no duplicated side effects/obligations)
        val v = atom(s.rhs, g, mode)
        when (val t = s.target) {
            is NfArray -> {
                val key = localKey(t.key)
                val arrType = envTypes[key] ?: SmtType.Array(SmtType.INT, SmtType.INT)
                val arr = env[key] ?: run {
                    val slot = freshVersion(key, arrType)
                    // First touch of an array-typed *receiver* field (e.g. `h.vals`):
                    // its current value is the field-selector read, not an
                    // unconstrained fresh array. Without this tie the flat slot and the
                    // selector term never meet — the store's index check (default-on
                    // since P3.3) cannot be discharged from a `requires h.vals.length
                    // >= n`, and the stored element ignores the previous contents.
                    val dot = key.lastIndexOf('.')
                    val recvName = if (dot > 0 && key.substring(0, dot) != "this") key.substring(0, dot) else null
                    if (recvName != null) {
                        val ra = env[recvName] ?: env["this.$recvName"]
                        if (ra != null && ra.smtType === SmtType.JAVA_OBJECT) {
                            val fname = key.substring(dot + 1)
                            val (jType, sType) = fieldSelectorType(ra, recvName, fname)
                            if (sType is SmtType.Array) {
                                val fn = fieldSelector(fname, sType, jType)
                                emit(mode, g, equality(slot, term.list(jType, sType, term.symbol(fn), ra)))
                            }
                        }
                    }
                    slot
                }
                // Java stores wrap the value to the element type: `((_ extract 7 0) v)`
                // for a byte[] element that was computed at promoted int width.
                val vv = convertTo(v, (arrType as? SmtType.Array)?.to)
                val idx = atom(t.index, g, mode)
                if (options.checkIndex) {
                    addObligation(
                        "array-store",
                        "array store bound ${describe(s.origin!!)}",
                        term.impl(
                            g,
                            term.and(
                                term.greaterOrEquals(idx, translator.makeInt(java.math.BigInteger.ZERO), true),
                                term.lessThan(idx, translator.arrayLength(arr))
                            )
                        ),
                        mode,
                        s.origin
                    )
                }
                val next = freshVersion(key, arrType)
                // phi-style store: on paths where the store does not execute (dead
                // guard, e.g. statements after a `break`), the array keeps its old
                // value instead of becoming a fresh unconstrained constant.
                emitPhi(mode, equality(next, iteTerm(g, term.store(arr, idx, vv), arr)))
                // frame the array length across the store: len(a[i := v]) == len(a).
                // Without this, a fresh version introduced by the store has an
                // unconstrained length and bounds checks on a read *afterwards*
                // can no longer be discharged from the `a.length` constraints.
                emit(mode, g, equality(translator.arrayLength(next), translator.arrayLength(arr)))
                // An element store does not change the array's *reference identity*;
                // preserving it keeps later frames and `==` comparisons sound.
                arrayRef(arr)?.let { refOld ->
                    arrayRef(next)?.let { refNew ->
                        emit(mode, g, equality(refNew, refOld))
                    }
                }
                // The store to `arr` must be visible through every other live array
                // value that provably denotes the same array (reference identity
                // equal). Each other array key is bumped to `ite(refEq, store, old)`,
                // framing by identity so distinct arrays stay untouched and the
                // alias decision is left to the solver. This is what makes
                // `b2 = a; a[i] = v; b2[i] == v` and the "last write wins when the
                // references are equal" pattern provable.
                val arrayKeys = env.keys.filter {
                    it != key && !isFieldMapKey(it) && env[it]?.smtType is SmtType.Array
                }
                for (k in arrayKeys) {
                    val kOld = env[k] ?: continue
                    val kArr = kOld.smtType as? SmtType.Array ?: continue
                    val refK = arrayRef(kOld) ?: continue
                    val refA = arrayRef(arr) ?: continue
                    val kNext = freshVersion(k, kArr)
                    emitPhi(
                        mode,
                        equality(kNext, iteTerm(g, iteTerm(equality(refK, refA), term.store(kOld, idx, vv), kOld), kOld))
                    )
                    emit(mode, g, equality(translator.arrayLength(kNext), translator.arrayLength(kOld)))
                    arrayRef(kNext)?.let { newRef -> emit(mode, g, equality(newRef, refK)) }
                }
            }

            is NfField -> {
                val fieldName = t.key.substringAfterLast('.')
                val recvTerm = if (t.receiver != "this") {
                    env[t.receiver] ?: env["this.${t.receiver}"]
                } else {
                    env["this"]
                }
                // identity-keyed map store for scalar/object fields of receivers
                // that resolve to an environment term (parameters, locals, or
                // `this.<field>` objects). `this`-receiver writes update the map as
                // well so object aliases of the enclosing instance (and inlined /
                // contracted callee receivers) observe the write; array-typed fields
                // and unresolvable receivers keep the flat per-path model below.
                if (recvTerm != null && recvTerm.smtType === SmtType.JAVA_OBJECT) {
                    val sType = fieldSelectorType(recvTerm, if (t.receiver == "this") "this" else t.receiver, fieldName).second
                    if (sType !is SmtType.Array) {
                        fieldMapStore(fieldName, sType, recvTerm, convertTo(v, sType), g, mode)
                    }
                }
                if (t.receiver != "this") {
                    // only the map keeps the write; the flat slot would go stale
                    return
                }
                // `this`-receiver writes also keep the historical flat per-path slot
                flatWrite(s, t, g, mode, v)
                return
            }

            else -> {
                val key = locationKey(t)
                // Bare writes to a field of the enclosing instance (`counter = 0`
                // normalizes to NfLocal but resolves to `this.counter`) update the
                // identity-keyed map too, so aliases of `this` observe the write
                // exactly like an explicit `this.f = v`.
                if (t is NfLocal && key.startsWith("this.")) {
                    val recv = env["this"]
                    if (recv != null && recv.smtType === SmtType.JAVA_OBJECT) {
                        val fieldName = key.substringAfterLast('.')
                        val sType = fieldSelectorType(recv, "this", fieldName).second
                        if (sType !is SmtType.Array) fieldMapStore(fieldName, sType, recv, convertTo(v, sType), g, mode)
                    }
                }
                flatWrite(s, t, g, mode, v)
            }
        }
    }

    /** The historical flat per-path SSA write for locals and `this`-receiver fields. */
    private fun flatWrite(s: NfAssign, t: NfLocation, g: SExpr, mode: Mode, v: SExpr) {
        val key = locationKey(t)
        val declaredJType = (t as? NfLocal)?.declaredType?.let { dt -> tryResolve(dt) }
        val type = when (t) {
            is NfLocal -> t.declaredType?.let { dt -> safeType(tryResolve(dt)) } ?: locType(t)
            is NfField -> fieldWriteType(t, key)
            else -> locType(t)
        }
        // a local of a narrow primitive type (`byte b = 127 + 1`) truncates the
        // promoted right-hand side to its declared width (Java wrap-around).
        val vv = convertTo(v, type)
        val prev = env[key]
        // the local keeps its resolved declared type as meta-information on the
        // SExpr: receiver-bound views (contract calls on `o.m()` with `o` a local,
        // invariant folding) consult `receiver.javaType` to expose `o`'s fields
        val next = freshVersion(key, type, declaredJType)
        // phi-style assignment: when the guard is false (dead path — e.g.
        // statements after a `break`/`continue`/`return`) the variable keeps
        // its previous value. Without this, `next` is an unconstrained fresh
        // constant that leaks into loop merges and postconditions.
        if (prev != null) {
            emitPhi(mode, equality(next, iteTerm(g, vv, prev)))
        } else {
            emit(mode, g, equality(next, vv))
        }
        // copying an array reference (`y = x`) copies the identity too:
        // reads and writes through `y` afterwards alias `x`.
        val rn = arrayRef(next)
        val rv = arrayRef(vv)
        if (rn != null && rv != null) emit(mode, g, equality(rn, rv))
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
                // read the pre-if value *before* freshVersion: when the else-branch is
                // empty, `env` aliases `snapshot` (env = snapshot above), so freshVersion
                // would otherwise write the new phi into snapshot and make the fallback
                // self-referential (`phi = ite(g, ..., phi)`), leaving `phi` unconstrained
                // on paths where the if-guard is false.
                val pre = snapshot[key] ?: ev
                val phi = freshVersionFor(key, type)
                // phi-style merge: `phi = ite(g, ite(c, tv, ev), pre)`. The if-guard
                // `g` is folded into the binding so a dead branch (guard false — e.g.
                // a `continue`/`break`/`return` earlier in an unrolled loop body)
                // keeps the pre-if value instead of leaving `phi` unconstrained;
                // an unconstrained phi referenced by loop-exit conditions and the
                // post-loop merge lets the solver invent values for it (spurious
                // counterexamples for loops combining continue/break/return).
                emitPhi(mode, equality(phi, iteTerm(g, iteTerm(c, tv, ev), pre)))
                env[key] = phi
                // alias frames must survive a phi: the merged array's reference
                // identity is whichever branch was taken.
                val rp = arrayRef(phi)
                val rt = arrayRef(tv)
                val re = arrayRef(ev)
                val rpre = arrayRef(pre)
                if (rp != null && rt != null && re != null && rpre != null) {
                    emitPhi(mode, equality(rp, iteTerm(g, iteTerm(c, rt, re), rpre)))
                }
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
        val loopId = s.loopId
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
        loopContext.addLast(loopId to false)
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
                    val phi = freshVersionFor(key, type)
                    // phi-style merge: `phi = ite(prefix, ite(c, bv, sv), sv)`. The
                    // iteration-entry guard is folded in so that a dead iteration
                    // (loop already exited via break/return earlier) keeps the
                    // pre-iteration value instead of leaving `phi` as an unconstrained
                    // constant that the exit-merge chain could later select.
                    emitPhi(mode, equality(phi, iteTerm(prefix, iteTerm(c, bv, sv), sv)))
                    env[key] = phi
                }
            }
            // reset continue flag of this loop for the next iteration
            val prevCont = env[ExprTranslator.CONTINUE + loopId] ?: term.makeFalse()
            val cont = freshVersion(ExprTranslator.CONTINUE + loopId, SmtType.BOOL)
            // phi-style reset: `cont = ite(and(prefix, c), false, prevCont)`. Without
            // folding the guard in, `cont` is an unconstrained fresh constant on paths
            // where the iteration is not entered (loop already exited), and the enclosed
            // statements of later iterations would be spuriously gated by `not cont`.
            emitPhi(mode, equality(cont, iteTerm(term.and(prefix, c), term.makeFalse(), prevCont)))
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
                    if (isFieldMapKey(key) && type is SmtType.Array) {
                        query.addCommand(term.command("declare-const", term.symbol(name), fieldMapSort(type.to)))
                    } else if (type === SmtType.JAVA_OBJECT) {
                        query.addCommand(term.command("declare-const", term.symbol(name), term.symbol("U")))
                    } else {
                        query.declareConst(name, type)
                    }
                    term.variable(type, envJavaTypes[key], name).also { undef = it }
                }
                acc = if (acc == null) v else iteTerm(e.cond, v, acc)
            }
            if (acc != null) {
                val fin = freshVersionFor(key, type)
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
        addObligation(
            "loop-invariant-init",
            "loop invariant initially valid ${describe(s.loopNode)}",
            term.impl(g, inv),
            mode
        )

        // (2) one arbitrary iteration preserves the invariant (and decreases the variant)
        val snapshot = LinkedHashMap(env)
        val havocEnv = LinkedHashMap(env)
        val modified = modifiedLocations(s.body) + ExprTranslator.RET +
            (ExprTranslator.BREAK + loopId) + (ExprTranslator.CONTINUE + loopId)
        for (key in modified) {
            if (envTypes.containsKey(key)) {
                val type = envTypes[key]!!
                val name = "\$havoc_${++uid}"
                if (isFieldMapKey(key) && type is SmtType.Array) {
                    query.addCommand(term.command("declare-const", term.symbol(name), fieldMapSort(type.to)))
                } else if (type === SmtType.JAVA_OBJECT) {
                    query.addCommand(term.command("declare-const", term.symbol(name), term.symbol("U")))
                } else {
                    query.declareConst(name, type)
                }
                havocEnv[key] = term.variable(type, envJavaTypes[key], name)
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
        loopContext.addLast(loopId to false)
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
            "loop-invariant-preserved",
            "loop invariant preserved ${describe(s.loopNode)}",
            term.impl(normal, invAfter),
            Mode.Record(rec)
        )
        for (v in variants) {
            val d0 = translatorOf(envOverride = havocBefore).tr(v)
            val d1 = translatorOf(envOverride = envAfter).tr(v)
            val zero = translator.makeInt(java.math.BigInteger.ZERO)
            addObligation(
                "variant",
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
                val type = envTypes[key]!!
                val name = "\$havoc_${++uid}"
                if (isFieldMapKey(key) && type is SmtType.Array) {
                    query.addCommand(term.command("declare-const", term.symbol(name), fieldMapSort(type.to)))
                } else if (type === SmtType.JAVA_OBJECT) {
                    query.addCommand(term.command("declare-const", term.symbol(name), term.symbol("U")))
                } else {
                    query.declareConst(name, type)
                }
                env[key] = term.variable(type, envJavaTypes[key], name)
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
        // A normal exit from the loop means its body was completed without
        // returning, breaking, or continuing: the method's `$ret` and the loop's
        // `$break`/`$continue` flags were just havoced, and if they stay
        // unconstrained the solver may treat the loop as "returned" with `$result`
        // still at its pre-loop value — silently disabling the code after the loop
        // and producing spurious postcondition failures (cf. a `return` inside the
        // loop followed by a post-loop `return -1`).
        emit(mode, g, term.not(flag(ExprTranslator.RET)))
        emit(mode, g, term.not(flag(ExprTranslator.CONTINUE + loopId)))
        if (strategy == LoopStrategy.LOOP_CONTRACT) {
            // under the loop contract a `break` is a second, legitimate exit: the
            // fall-through branch is the one on which no break occurred
            emit(mode, term.and(g, term.not(condMain)), term.not(brkMain))
        } else {
            emit(mode, g, term.not(brkMain))
        }
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
        fun fieldLocationKey(l: NfLocation) {
            val key = locationKey(l)
            res.add(key)
            // field-map key for scalar/object fields of receivers that resolve to
            // an object term (`this`-receiver and explicit receiver writes update
            // the map; a bare `counter = 0` normalizes to NfLocal but resolves to
            // `this.counter`) — the loop merges must version it like any
            // modified slot
            val isBareThisField = l is NfLocal && key.startsWith("this.")
            if (l is NfField || isBareThisField) {
                val recvTerm = if (!isBareThisField && (l as NfField).receiver != "this") {
                    env[l.receiver] ?: env["this.${l.receiver}"]
                } else {
                    env["this"]
                }
                val scopeText = if (!isBareThisField && (l as NfField).receiver != "this") l.receiver else "this"
                if (recvTerm != null && recvTerm.smtType === SmtType.JAVA_OBJECT) {
                    val sType = fieldSelectorType(recvTerm, scopeText, key.substringAfterLast('.')).second
                    if (sType !is SmtType.Array) res.add(fieldMapKey(key.substringAfterLast('.'), sType))
                }
            }
        }

        fun go(l: List<NfStmt>) {
            for (s in l) {
                when (s) {
                    is NfAssign -> fieldLocationKey(s.target)

                    is NfHavoc -> fieldLocationKey(s.location)

                    is NfIf -> {
                        go(s.thenStmts); go(s.elseStmts)
                    }

                    is NfLoop -> go(s.body)

                    is NfTryCatch -> {
                        go(s.tryBody)
                        s.catches.forEach { go(it.body) }
                        go(s.finallyBody)
                    }

                    is NfSwitch -> s.cases.forEach { go(it.body) }

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
            // Java binary numeric promotion (JLS 5.6): byte/short/char operands are
            // widened to int before the operation. `checkDivision` must test the
            // *promoted* divisor too (an 8-bit `bv` zero test against a 32-bit
            // constant would be ill-sorted); `binary` promotes internally for every
            // other consumer.
            val (lp, rp) = translator.promote(e.operator, l, r)
            checkDivision(e, rp, guard, mode)
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
                // `==`/`!=` on two arrays is *reference* equality (Java semantics):
                // comparing identities, never (extensional) contents. An element store
                // through one alias is exactly a store through the other, so when the
                // identities match the last write wins.
                if (l.smtType is SmtType.Array && r.smtType is SmtType.Array) {
                    val lr = arrayRef(l)
                    val rr = arrayRef(r)
                    if (lr != null && rr != null) {
                        val res = translator.binary(e.operator, lr, rr)
                        return if (res.smtType == SmtType.BOOL) res else tempAssign(res, guard, mode, e)
                    }
                }
            }
            val res = translator.binary(e.operator, lp, rp)
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
            // JLS 15.25 numeric promotion on the branches: byte/short/char met with
            // int (or long) promote to int/long, so a `b > 0 ? b : 300` ternary over
            // a BV8 variable and a BV32 literal stays well-sorted.
            val tb = t.smtType as? SmtType.BitVec
            val eb = el.smtType as? SmtType.BitVec
            val (t2, el2) = if (tb != null && eb != null && tb.width != eb.width) {
                val w = if (tb.width >= 64 || eb.width >= 64) 64 else 32
                convertTo(t, SmtType.getBitVec(w)) to convertTo(el, SmtType.getBitVec(w))
            } else {
                t to el
            }
            SList(t2.smtType ?: el2.smtType, t2.javaType, listOf(term.symbol("ite"), c, t2, el2))
        }

        is EnclosedExpr -> atom(e.inner, guard, mode)

        is CastExpr -> {
            val v = atom(e.expression, guard, mode)
            if (options.checkCast &&
                e.expression !is com.github.javaparser.ast.expr.NullLiteralExpr &&
                e.type.isReferenceType && v.smtType === SmtType.JAVA_OBJECT
            ) {
                val name = qualifyType(e.type.toString())
                ensureTypeSort(name)
                addObligation(
                    "castcheck", "cast ${describe(e)}",
                    term.impl(guard, typeMatch(v, e.type)), mode, e
                )
            }
            // Java primitive casting (JLS 5.1.2/5.1.3): narrowing keeps the low
            // bits (wrap-around), widening sign-extends (`char` zero-extends). This
            // was previously a silent no-op, so `(byte)300` stayed 300 and mixing a
            // narrowed BV8 with a BV32 literal produced ill-sorted SMT.
            if (e.type.isPrimitiveType) {
                val jType = try {
                    e.type.resolve() as? ResolvedPrimitiveType
                } catch (ex: Exception) {
                    null
                }
                val converted = convertTo(v, jType?.let { translator.getType(it) })
                if (jType != null) converted.javaType = jType
                converted
            } else {
                v
            }
        }

        is MethodCallExpr -> handleCall(e, guard, mode)

        is ObjectCreationExpr -> {
            // `new T(...)`: a fresh object of the (reference) sort. Constructor
            // arguments are evaluated (and captured, so the constructor's contract
            // can bind them to the parameters) for their side effects.
            val args = e.arguments.map { atom(it, guard, mode) }
            val obj = freshTemp(SmtType.JAVA_OBJECT, tryExprType(e))
            // Java `new` never returns null: axiomatize the fresh object as non-null
            // so the default-on `nullcheck` on a `new T().m(...)` receiver proves.
            emit(mode, guard, term.nonNull(obj))
            if (options.checkCast && obj.smtType === SmtType.JAVA_OBJECT) {
                // Bind typeof/instanceof so that `(T) new T(...)` casts are provable.
                val jt = try { e.calculateResolvedType() } catch (ex: Exception) { null }
                val name = (jt as? ResolvedReferenceType)?.qualifiedName
                    ?: if (e.type.isReferenceType) qualifyType(e.type.toString()) else null
                if (name != null && name.isNotEmpty()) {
                    ensureTypeSort(name)
                    val sort = term.symbol(typeSortName(name))
                    val typeofExpr = SList(null, null, listOf(term.symbol("typeof"), obj))
                    emit(
                        mode, guard,
                        term.list(null, SmtType.BOOL, term.symbol("="), typeofExpr, sort)
                    )
                    emit(
                        mode, guard,
                        term.list(null, SmtType.BOOL, term.symbol("instanceof"), obj, sort)
                    )
                }
            }
            // P2.1: a construction whose constructor is in this compilation unit and
            // carries a JML contract is a *contract call site*: the caller must
            // establish the constructor's requires, may assume its ensures, and may
            // assume the constructed class's invariant. Uncontracted constructors
            // (library classes, implicit constructors, contractless in-CU bodies —
            // mirroring the P1.3 "contractless-with-body keeps old behavior" carve-out)
            // keep the fresh-constant model: the object's fields are unconstrained,
            // which is a sound over-approximation of an unrefined construction.
            val ctor = try {
                findConstructor(e)
            } catch (ex: Exception) {
                null
            }
            if (ctor != null && ctor.contracts.isNotEmpty()) {
                constructorContract(e, args, obj, guard, mode, ctor)
            }
            obj
        }

        is ArrayCreationExpr -> {
            // P3.1: a multi-dimensional array (`new int[n][m]`) has one level per
            // dimension, but the model below only encodes the *first* one — the
            // element sort silently collapses to a 1-D array of the base type.
            // Reject instead of mis-modeling.
            if (e.levels.size > 1) {
                throw UnsupportedFeatureException(
                    "unsupported: multi-dimensional array creation ${describe(e)}: $e",
                    e.range.orElse(null)
                )
            }
            // A fresh anonymous array constant whose length is bound to the first
            // (possibly symbolic) dimension; a negative size is a runtime error.
            val dimension = if (e.levels.isNotEmpty() && e.levels[0].dimension.isPresent) {
                e.levels[0].dimension.get()
            } else {
                null
            }
            val dimTerm = dimension?.let { atom(it, guard, mode) }
            if (options.checkNegativeArraySize && dimTerm != null) {
                addObligation(
                    "negative-array-size", "negative array size ${describe(e)}",
                    term.impl(
                        guard,
                        term.greaterOrEquals(dimTerm, translator.makeInt(java.math.BigInteger.ZERO), true)
                    ),
                    mode, e
                )
            }
            translatorOf().createArray(
                tryExprType(e), arraySmtType(e), dimTerm
            )
        }

        is ArrayAccessExpr -> {
            val arrayS: SExpr = atom((e as ArrayAccessExpr).name, guard, mode)
            val indexS: SExpr = atom(e.index, guard, mode)
            checkIndex(e, arrayS, indexS, guard, mode)
            val elementType = (arrayS.smtType as? SmtType.Array)?.to ?: SmtType.INT
            term.select(elementType, null, arrayS, indexS)
        }

        is FieldAccessExpr -> {
            // Dereferencing a null receiver is a runtime error (arrays are value
            // maps and modelled never-null; `this` is already asserted non-null).
            val scopeExpr = e.scope
            if (options.checkNull && scopeExpr != null && e.nameAsString != "length") {
                if (scopeExpr is ThisExpr) {
                    translatorOf().tr(e)
                } else {
                    val scopeTerm = atom(scopeExpr, guard, mode)
                    if (scopeTerm.smtType === SmtType.JAVA_OBJECT) {
                        addObligation(
                            "nullcheck", "dereference ${describe(e)}",
                            term.impl(guard, term.nonNull(scopeTerm)), mode, e
                        )
                    }
                    fieldRead(scopeExpr, e, scopeTerm)
                }
            } else {
                translatorOf().tr(e)
            }
        }

        is StringLiteralExpr -> {
            // Literal receivers can flow into the receiver-sensitive checks: declare
            // a non-null reference constant (so `--check-null` on literal receivers
            // proves) and, under `--check-string-index`, bind its runtime length so
            // bounds checks on literal receivers are concrete.
            if (options.checkStringIndex || options.checkNull) {
                literalConst(e.value)
            } else {
                translatorOf().tr(e)
            }
        }

        is NameExpr, is ThisExpr, is LiteralExpr, is JmlExpression ->
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
            "divbyzero",
            "division by zero ${describe(e)}",
            term.impl(guard, term.not(equality(divisor, translator.makeInt(java.math.BigInteger.ZERO)))),
            mode,
            e
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
        addObligation(
            "overflow",
            "arithmetic overflow ${describe(e)}",
            term.impl(guard, term.not(fnApplyOverflow(e))),
            mode,
            e
        )
    }

    /**
     * Encodes "the operation [e] overflows its signed bit-vector type" using only
     * portable SMT-LIB bit-vector operators. The previous encoding used Z3's
     * overflow predicates `bvsaddo`/`bvssubo`/`bvsmulo`, which do not exist in
     * older Z3 releases (Ubuntu noble ships 4.8.12, where the query fails with
     * `unknown constant`). Instead, both operands are sign-extended to a width in
     * which the exact result cannot wrap (w+1 bits for +/- and 2w bits for *),
     * the operation is applied on the widened operands (the exact result, no
     * wrap-around) and natively at the original width (the Java wrap-around
     * result), and overflow is exactly the case where the sign-extension of the
     * wrapped result differs from the exact one.
     */
    private fun fnApplyOverflow(e: BinaryExpr): SExpr {
        val tr = translatorOf()
        val a0 = e.left.accept(tr, null)!!
        val b0 = e.right.accept(tr, null)!!
        // args arrive unpromoted (`e.left.accept` translates the raw operand); apply
        // Java's binary numeric promotion first so the width being checked is the
        // promoted one — a byte+byte sum can never overflow an int, so its overflow
        // check must not wrap at 8 bits.
        val (a, b) = translator.promote(e.operator, a0, b0)
        val width = (a.smtType as? SmtType.BitVec)?.width
            ?: (b.smtType as? SmtType.BitVec)?.width
            ?: throw RuntimeException(
                "overflow check requires bit-vector operands, got ${a.smtType}, ${b.smtType}"
            )
        // a wider width in which the exact signed operation cannot wrap: w+1 bits
        // for +/- and 2w bits for * (the signed product of two w-bit values always
        // fits in 2w bits).
        val exactWidth = if (e.operator == BinaryExpr.Operator.MULTIPLY) 2 * width else width + 1
        val wrapped = tr.translator.binary(e.operator, a, b)
        val exact = tr.translator.binary(
            e.operator,
            term.signExtend(a, exactWidth),
            term.signExtend(b, exactWidth),
        )
        return term.not(term.equality(exact, term.signExtend(wrapped, exactWidth)))
    }

    private fun checkIndex(e: ArrayAccessExpr, array: SExpr, index: SExpr, guard: SExpr, mode: Mode) {
        if (!options.checkIndex) return
        addObligation(
            "array-bound",
            "array bound ${describe(e)}",
            term.impl(
                guard,
                term.and(
                    term.greaterOrEquals(index, translator.makeInt(java.math.BigInteger.ZERO), true),
                    term.lessThan(index, translator.arrayLength(array))
                )
            ),
            mode,
            e
        )
    }
    //endregion

    private fun tryExprType(e: Expression): ResolvedType? = try {
        e.calculateResolvedType()
    } catch (ex: Exception) {
        null
    }

    /** The SMT array type of a `new T[..]` when the expression's resolved type is
     *  unavailable (the node is a detached copy, e.g. from a re-parented parsed
     *  fragment). Mirrors `translator.getType` on the array's resolved type: the
     *  element sort follows the mode (BV32..BV64 / FP in BOUNDED, Int / Real in
     *  UNBOUNDED) and so does the index sort (BV32 in BOUNDED — `bv$length` is
     *  declared over `(Array (_ BitVec 32) _)` — and Int in UNBOUNDED). */
    private fun arraySmtType(e: ArrayCreationExpr): SmtType {
        val t = e.elementType
        val element = when {
            !t.isPrimitiveType -> SmtType.JAVA_OBJECT

            t.asString() == "boolean" -> SmtType.BOOL

            t.asString() == "float" || t.asString() == "double" ->
                if (options.mode == VerificationMode.BOUNDED) {
                    if (t.asString() == "float") SmtType.FP32 else SmtType.FP64
                } else {
                    SmtType.REAL
                }

            options.mode != VerificationMode.BOUNDED -> SmtType.INT

            else -> when (t.asString()) {
                "byte" -> SmtType.BV8
                "short" -> SmtType.BV16
                "char" -> SmtType.BV16
                "long" -> SmtType.BV64
                else -> SmtType.BV32
            }
        }
        val index = if (options.mode == VerificationMode.BOUNDED) SmtType.BV32 else SmtType.INT
        return SmtType.Array(index, element)
    }
    //endregion

    //region method calls
    private fun handleCall(call: MethodCallExpr, guard: SExpr, mode: Mode): SExpr {
        // `\invariant_for` / `\static_invariant_for` in a statement position (e.g.
        // `//@ assume \invariant_for(this);`) are folded invariant expressions, not
        // Java method calls; expand them before any call resolution.
        val callName = call.nameAsString
        if (callName == "\\invariant_for" || callName == "\\static_invariant_for") {
            val arg = call.arguments.firstOrNull()
            val recv = if (arg != null && arg !is ClassExpr) {
                try {
                    atom(arg, guard, mode)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            return invariantForTerm(recv, arg, callName == "\\static_invariant_for") ?: term.makeTrue()
        }
        // `String.length()`-style model read under the string-index feature: the
        // runtime length is the uninterpreted `(stringLength s)` function instead of
        // an opaque contract-call result.
        if (options.checkStringIndex && call.arguments.isEmpty() && call.nameAsString == "length") {
            val scope = call.scope.orElse(null)
            if (isStringLike(scope)) {
                val recv = if (scope == null || scope is ThisExpr) {
                    term.makeThis()
                } else {
                    atom(scope, guard, mode)
                }
                if (options.checkNull && recv.smtType === SmtType.JAVA_OBJECT) {
                    addObligation(
                        "nullcheck", "dereference ${describe(call)}",
                        term.impl(guard, term.nonNull(recv)), mode, call
                    )
                }
                return stringLengthOf(recv)
            }
        }
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
            return contractCall(call, guard, mode, decl)
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

    /** True if the receiver expression denotes a `java.lang.String`-like reference
     *  (a `String`, `StringBuilder` or `StringBuffer`, or `this` in such a class). */
    private fun isStringLike(scope: Expression?): Boolean =
        scope != null && (
            scope is ThisExpr || try {
            scope.calculateResolvedType().describe().startsWith("java.lang.String")
        } catch (e: Exception) {
            false
        }
        )

    /**
     * Emits the implicit bounds obligation for known `String`/`StringBuilder` index
     * methods (`charAt`, `codePointAt`, `substring(...)`) against the receiver's
     * model length `(stringLength recv)`. Bounded-mode comparisons stay on the
     * mode's `int` sort via [term.lessOrEquals]/[term.lessThan] sort dispatch.
     */
    private fun stringIndexChecker(
        call: MethodCallExpr, args: List<SExpr>, recv: SExpr, guard: SExpr, mode: Mode
    ) {
        val name = call.nameAsString
        val len = stringLengthOf(recv)
        val zero = translator.makeInt(java.math.BigInteger.ZERO)
        val bounds: SExpr? = when (name) {
            "charAt", "codePointAt" -> if (args.size == 1) {
                term.and(term.lessOrEquals(zero, args[0], true), term.lessThan(args[0], len))
            } else {
                null
            }

            "substring" -> when (args.size) {
                1 -> term.and(
                    term.lessOrEquals(zero, args[0], true),
                    term.lessOrEquals(args[0], len, true)
                )

                2 -> term.and(
                    term.and(
                        term.lessOrEquals(zero, args[0], true),
                        term.lessOrEquals(args[0], args[1], true)
                    ),
                    term.lessOrEquals(args[1], len, true)
                )

                else -> null
            }

            else -> null
        }
        if (bounds != null) {
            addObligation(
                "string-index", "string index bound ${describe(call)}",
                term.impl(guard, bounds), mode, call
            )
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

        // For `o.m(...)` the callee's `this` denotes the receiver `o`. When any
        // receiver-sensitive check is enabled the receiver is evaluated up front so
        // the null / string-index checks and the contract-view binding share a
        // single translation (no duplicated side effects).
        val scope = call.scope.orElse(null)
        val needsEarlyReceiver = options.checkNull || options.checkStringIndex
        var receiver: SExpr? =
            if (needsEarlyReceiver && scope != null && scope !is ThisExpr && (decl?.isStatic != true)) {
                atom(scope, guard, mode)
            } else {
                null
            }
        var receiverPrefix: String? = if (receiver != null) "$scope." else null

        if (options.checkNull && receiver != null && receiver.smtType === SmtType.JAVA_OBJECT) {
            addObligation(
                "nullcheck", "dereference ${describe(call)}",
                term.impl(guard, term.nonNull(receiver)), mode, call
            )
        }
        if (options.checkStringIndex) {
            val recvForString = receiver
                ?: if (scope != null && isStringLike(scope)) term.makeThis() else null
            if (recvForString != null) stringIndexChecker(call, args, recvForString, guard, mode)
        }

        // bind the receiver for the callee view (and the unrefined-call havoc) even
        // when no receiver-sensitive check forced it up front
        if (receiver == null && scope != null && scope !is ThisExpr && decl?.isStatic != true) {
            receiver = atom(scope, guard, mode)
            receiverPrefix = "$scope."
        }

        val contracts = decl?.contracts ?: NodeList<JmlContract>()
        JMLUtils.unroll(contracts)
        // The callee's spec cases stay separate (JML disjunction): each case i gives
        // `requires_i -> ensures_i`, the caller must establish the *union* of the
        // assignable clauses and may assume each case's ensures guarded by its own
        // requires. Merging clauses conjunctively would demand all requires and
        // assume all ensures — unsound for cases with distinct preconditions.
        val cases = contracts.toList()

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
            // Identity-keyed field maps are shared with the callee; each scalar/object
            // field of the receiver that lives only in such a map is exposed to the
            // callee as `select(map, receiver)`, so `requires`/`ensures` clauses act
            // on the caller-visible map entry.
            val rt = try {
                recv.javaType as? com.github.javaparser.resolution.types.ResolvedReferenceType
            } catch (e: Exception) {
                null
            }
            if (rt != null) {
                val declared = try { rt.typeDeclaration.orElse(null)?.declaredFields ?: emptyList() } catch (e: Exception) {
                    emptyList()
                }
                for (fd in declared) {
                    val fname = fd.name
                    if (map.containsKey("this.$fname")) continue
                    if (fd.isStatic) continue
                    val sType = try { fieldSelectorType(recv, prefix.removeSuffix("."), fname).second } catch (e: Exception) {
                        continue
                    }
                    if (sType is SmtType.Array) continue
                    val mkey = fieldMapKey(fname, sType)
                    val mv = env[mkey] ?: continue
                    map[mkey] = mv
                    map["this.$fname"] = term.select(sType, null, mv, recv)
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
            // `assignable this.f` on a field with a live identity map refreshes the
            // map entry as well: for a receiver-bound call the callee's assignable
            // clause acts on the receiver object, and for a plain `assignable this.f`
            // / `assignable f` on the enclosing instance it refreshes `this`'s slot —
            // so the callee's `ensures` clauses and the caller's post-call reads
            // agree on the map value.
            if (locKey.startsWith("this.") && !isFieldMapKey(locKey)) {
                val fieldName = target.substringAfterLast('.')
                val recv = receiver ?: env["this"]
                if (recv != null && recv.smtType === SmtType.JAVA_OBJECT) {
                    val sType = fieldSelectorType(recv, "this", fieldName).second
                    if (sType !is SmtType.Array && env.containsKey(fieldMapKey(fieldName, sType))) {
                        val fresh = freshTemp(sType, null)
                        fieldMapStore(fieldName, sType, recv, fresh, term.makeTrue(), mode)
                    }
                }
            }
            val type = envTypes[target] ?: return
            declareHavocConst(target, type)
        }

        // (P1.3) unrefined callee: a call the caller cannot refine with — no
        // declaration at all (hence no body and no JML contract), or a bodyless
        // native/abstract method without a contract. Default: fail loudly instead of
        // silently treating the result as an unconstrained constant while assuming
        // the call touched nothing (unsound). With --allow-unrefined-calls, havoc
        // the caller-visible frame (this.*, arrays, field maps — the \everything
        // path) so the call is over-approximated instead of guessed.
        fun havocEverything() {
            for (key in env.keys.toList()) {
                if (key.startsWith("this.") || envTypes[key] is SmtType.Array || isFieldMapKey(key)) {
                    havocKey(key)
                }
            }
        }
        val unrefinedCallee =
            "its callee has no JML contract and no body to inline; failing loudly instead of " +
                "guessing its behavior. Refine the call with a JML contract (or a body) or rerun " +
                "with --allow-unrefined-calls to havoc the caller-visible frame"
        if (decl == null) {
            if (!options.allowUnrefinedCalls) {
                throw IllegalStateException(
                    "VCG: call to ${describe(call)} could not be resolved and $unrefinedCallee"
                )
            }
            havocEverything()
            return result
        }
        if (cases.isEmpty() && decl.body.isEmpty) {
            if (!options.allowUnrefinedCalls) {
                throw IllegalStateException(
                    "VCG: call to ${describe(call)} (${decl.nameAsString}): $unrefinedCallee"
                )
            }
            havocEverything()
            return result
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

        // (a) precondition against the pre-call state: the caller must establish the
        // disjunction of the per-case requires (at least one spec case applies). A
        // case without a requires clause always applies, making the disjunction `true`.
        val trPre = translatorOf(envOverride = calleeView())
        val caseRequires = mutableListOf<List<SExpr>>()
        for (c in cases) {
            caseRequires += clauseExprs(c, REQUIRES).map { trPre.tr(it) }
        }
        val forcedCases = cases.filter { clauseExprs(it, REQUIRES).isNotEmpty() }
        if (cases.isNotEmpty() && forcedCases.size == cases.size) {
            val disjunction = caseRequires
                .map { r -> r.reduce { a, b -> term.and(a, b) } }
                .reduce { a, b -> term.or(a, b) }
            addObligation(
                "precondition",
                "precondition of ${decl.nameAsString}",
                term.impl(guard, disjunction),
                mode
            )
        }
        // (b) havoc the assignable locations (union over all cases, in the caller's
        // environment).
        val assignable = cases.flatMap { clauseExprs(it, ASSIGNABLE) }
        if (assignable.isEmpty() || assignable.any { it.toString().contains("everything") }) {
            // \everything in any case, or no assignable clause at all: havoc all
            // fields and arrays.
            for (key in env.keys.toList()) {
                if (key.startsWith("this.") || envTypes[key] is SmtType.Array || isFieldMapKey(key)) {
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
        // (c) assume the postcondition against the post-havoc state: for each spec
        // case i, assume `requires_i (pre-state) -> ensures_i (post-state)`. The
        // guard anchors on the *pre-call* values of the requires, so a case whose
        // precondition failed before the call commits the callee to nothing.
        val trPost = translatorOf(envOverride = calleeView())
        for ((i, c) in cases.withIndex()) {
            val posts = clauseExprs(c, ENSURES)
            if (posts.isEmpty()) continue
            val post = posts.map { trPost.tr(it) }.reduce { a, b -> term.and(a, b) }
            val req = caseRequires[i]
            val assume = if (req.isEmpty()) {
                post
            } else {
                term.impl(req.reduce { a, b -> term.and(a, b) }, post)
            }
            emit(mode, guard, assume)
        }
        return result
    }

    /**
     * The in-CU `ConstructorDeclaration` behind `new T(...)`, or `null` for library
     * constructors and implicit default constructors (which cannot carry a JML
     * contract). Prefers the symbol solver's resolution (exact overload); falls back
     * to a same-CU search by resolved qualified type name and arity.
     */
    private fun findConstructor(e: ObjectCreationExpr): ConstructorDeclaration? {
        try {
            val ast = e.resolve().toAst().orElse(null) as? ConstructorDeclaration
            if (ast != null) return ast
        } catch (ex: Exception) {
            // fall through to the same-CU search
        }
        val qn = try {
            (e.calculateResolvedType() as? ResolvedReferenceType)?.qualifiedName
        } catch (ex: Exception) {
            null
        } ?: return null
        val cu = ctx.callable.findAncestor(CompilationUnit::class.java).orElse(null) ?: return null
        return cu.findAll(ConstructorDeclaration::class.java).firstOrNull { cd ->
            cd.parameters.size == e.arguments.size && try {
                cd.findAncestor(ClassOrInterfaceDeclaration::class.java)
                    .orElse(null)?.resolve()?.qualifiedName == qn
            } catch (ex: Exception) {
                false
            }
        }
    }

    /**
     * Applies a contracted constructor at its creation site (P2.1). The fresh object
     * is the callee view's `this` (constructor contracts speak about the object being
     * built); the caller must establish the *disjunction* of the per-case `requires`,
     * may assume each case's `ensures` guarded by its own (pre-state) `requires`, and
     * may assume the constructed class's class invariant — a constructor contract
     * establishes it, exactly as `checkPostcondition` folds it into the constructor's
     * own verification obligation.
     */
    private fun constructorContract(
        creation: ObjectCreationExpr,
        args: List<SExpr>,
        obj: SExpr,
        guard: SExpr,
        mode: Mode,
        ctor: ConstructorDeclaration,
    ) {
        JMLUtils.unroll(ctor.contracts)
        val cases = ctor.contracts.toList()
        if (cases.isEmpty()) return

        /** The callee view of the creation: current state + params + the fresh `this`. */
        fun ctorView(): HashMap<String, SExpr> {
            val m = HashMap(env)
            // drop the enclosing method's own `this.*` flat slots so the clauses'
            // field reads refer to the constructed object
            m.keys.removeAll { it.startsWith("this.") }
            m["this"] = obj
            // expose the constructed object's fields: every scalar/object field of
            // the constructor's class reads as `select(map, obj)` in the contract
            // clauses. The maps are declared eagerly, so the clauses' reads and the
            // caller's post-construction reads share the same select terms
            // regardless of translation order (a bare/`this.` field otherwise falls
            // back to the flat-`this` slot of the *enclosing* method, or to an
            // unknown constant).
            val cls = ctor.findAncestor(ClassOrInterfaceDeclaration::class.java).orElse(null)
            if (cls != null) {
                for (fd in cls.fields) {
                    if (fd.isStatic) continue
                    for (v in fd.variables) {
                        val fname = v.nameAsString
                        val sType = try {
                            envTypes["this.$fname"] ?: safeType(resolveType(v.type))
                        } catch (e: Exception) {
                            continue
                        }
                        if (sType is SmtType.Array) continue
                        val mkey = fieldMapKey(fname, sType)
                        if (env[mkey] == null) declareFieldMap(mkey, sType)
                        val mv = env[mkey] ?: continue
                        m["this.$fname"] = term.select(sType, null, mv, obj)
                    }
                }
            }
            ctor.parameters.forEachIndexed { i, p ->
                if (i < args.size) m[p.nameAsString] = args[i]
            }
            return m
        }

        // (a) precondition against the pre-state: the caller must establish the
        // disjunction of the per-case requires (at least one spec case applies).
        val trPre = translatorOf(envOverride = ctorView())
        val caseRequires = cases.map { c -> clauseExprs(c, REQUIRES).map { trPre.tr(it) } }
        val forcedCases = cases.filterIndexed { i, _ -> caseRequires[i].isNotEmpty() }
        if (cases.isNotEmpty() && forcedCases.size == cases.size) {
            val disjunction = caseRequires
                .map { r -> r.reduce { a, b -> term.and(a, b) } }
                .reduce { a, b -> term.or(a, b) }
            addObligation(
                "precondition",
                "precondition of constructor ${describe(creation)}",
                term.impl(guard, disjunction),
                mode,
                creation
            )
        }
        // (b) havoc the assignable locations (union over all cases). The fresh
        // object's own state (`this.f`, bare fields) is already unconstrained, so
        // only *aliasable* caller-visible state named in the assignable clauses is
        // refreshed. No assignable clause / `\everything` (the JML default) means the
        // constructor may touch anything reachable — mirror contractCall.
        val assignable = cases.flatMap { clauseExprs(it, ASSIGNABLE) }
        if (assignable.isEmpty() || assignable.any { it.toString().contains("everything") }) {
            for (key in env.keys.toList()) {
                if (key.startsWith("this.") || envTypes[key] is SmtType.Array || isFieldMapKey(key)) {
                    havocCallerKey(key, mode = mode)
                }
            }
        } else {
            for (loc in assignable) {
                if (loc.toString().contains("nothing")) continue
                havocCtorLocation(loc, ctor, mode)
            }
        }
        // (c) assume each case's ensures (same pre-state-requires guard discipline as
        // contractCall), then the constructed class's invariant, which a valid
        // constructor contract guarantees and its verification re-establishes.
        val trPost = translatorOf(envOverride = ctorView())
        for ((i, c) in cases.withIndex()) {
            val posts = clauseExprs(c, ENSURES)
            if (posts.isEmpty()) continue
            val post = posts.map { trPost.tr(it) }.reduce { a, b -> term.and(a, b) }
            val req = caseRequires[i]
            val assume = if (req.isEmpty()) {
                post
            } else {
                term.impl(req.reduce { a, b -> term.and(a, b) }, post)
            }
            emit(mode, guard, assume)
        }
        invariantForTerm(obj, creation, isStatic = false)?.let { emit(mode, guard, it) }
    }

    /**
     * Havocs one assignable *location* of a constructor contract in the caller's
     * environment. `this.f`-style locations denote the fresh object's own state and
     * are already unconstrained; only aliasable caller-visible state is refreshed —
     * a named receiver field (`o.f`) through its map entry, an array slot through a
     * fresh version, a scalar parameter through a fresh constant.
     */
    private fun havocCtorLocation(loc: Expression, ctor: ConstructorDeclaration, mode: Mode) {
        when (loc) {
            is NameExpr -> {
                // a bare field of the constructed class is fresh-object state; a
                // parameter name is the caller's argument (by-reference for objects)
                val cls = ctor.findAncestor(ClassOrInterfaceDeclaration::class.java).orElse(null)
                val isCtorField = cls?.fields?.any { f ->
                    f.variables.any { it.nameAsString == loc.nameAsString }
                } == true
                if (isCtorField) return
                havocCallerKey(loc.nameAsString, mode = mode)
            }

            is FieldAccessExpr -> {
                val key = try {
                    Normalizer().key(loc)
                } catch (ex: Exception) {
                    return
                }
                // `this.f` and bare fields of the constructed class are fresh state
                if (key.startsWith("this.")) return
                val recvName = key.substringBeforeLast('.')
                val fieldName = key.substringAfterLast('.')
                val recv = env[recvName]
                val sType = if (recv != null) {
                    try {
                        fieldSelectorType(recv, recvName, fieldName).second
                    } catch (ex: Exception) {
                        null
                    }
                } else {
                    null
                }
                if (sType is SmtType.Array) {
                    havocCallerKey(key, mode = mode)
                    return
                }
                if (recv != null && recv.smtType === SmtType.JAVA_OBJECT &&
                    sType != null && env.containsKey(fieldMapKey(fieldName, sType))
                ) {
                    val fresh = freshTemp(sType, null)
                    fieldMapStore(fieldName, sType, recv, fresh, term.makeTrue(), mode)
                }
            }

            is ArrayAccessExpr -> {
                val key = try {
                    localKey(Normalizer().key(loc.name))
                } catch (ex: Exception) {
                    return
                }
                havocCallerKey(key, mode = mode)
            }

            else -> {}
        }
    }

    /** Havocs one caller-environment key by declaring a fresh constant; `this.f`
     *  field keys also refresh the identity-keyed map entry for [receiver]
     *  (defaults to the caller's `this`), so subsequent map reads observe the new
     *  value. Mirrors the caller-state half of `contractCall`'s local rule. */
    private fun havocCallerKey(locKey: String, receiver: SExpr? = null, mode: Mode = Mode.Real) {
        if (locKey.startsWith("this.") && !isFieldMapKey(locKey)) {
            val fieldName = locKey.substringAfterLast('.')
            val recv = receiver ?: env["this"]
            if (recv != null && recv.smtType === SmtType.JAVA_OBJECT) {
                val sType = try {
                    fieldSelectorType(recv, "this", fieldName).second
                } catch (e: Exception) {
                    null
                }
                if (sType != null && sType !is SmtType.Array && env.containsKey(fieldMapKey(fieldName, sType))) {
                    val fresh = freshTemp(sType, null)
                    fieldMapStore(fieldName, sType, recv, fresh, term.makeTrue(), mode)
                }
            }
        }
        val type = envTypes[locKey] ?: return
        declareHavocConst(locKey, type)
    }

    /** Inline the callee body; the result is the callee's return value. After the body
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
        declareSmtConst(name, sRet)
        calleeEnv[ExprTranslator.RESULT] = term.variable(sRet, jRet, name)
        calleeEnv[ExprTranslator.RET] = term.makeFalse()

        // For `o.m(...)` the callee's `this` denotes the receiver `o`; alias tracked
        // receiver fields (`o.f`) so reads of `this.f` inside the callee see them.
        val scope = call.scope.orElse(null)
        val receiver = if (scope != null && scope !is ThisExpr && !decl.isStatic) {
            atom(scope, guard, mode)
        } else {
            null
        }
        if (options.checkNull && receiver != null && receiver.smtType === SmtType.JAVA_OBJECT) {
            addObligation(
                "nullcheck", "dereference ${describe(call)}",
                term.impl(guard, term.nonNull(receiver)), mode, call
            )
        }
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

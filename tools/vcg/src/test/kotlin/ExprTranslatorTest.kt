/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.jml.expr.JmlBinaryInfixExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr.JmlDefaultBinder
import com.github.javaparser.ast.type.ArrayType
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import io.github.jmltoolkit.smt.ArithmeticTranslator
import io.github.jmltoolkit.smt.BitVectorArithmeticTranslator
import io.github.jmltoolkit.smt.IntArithmeticTranslator
import io.github.jmltoolkit.smt.SmtQuery
import io.github.jmltoolkit.smt.SmtTermFactory
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.model.SmtType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Unit tests for [ExprTranslator], the second stage of the VCG pipeline (Java/JML
 * expressions -> SMT terms). The translator is exercised directly against hand-built
 * environments: no symbol resolution is needed, which makes it possible to assert on
 * the exact SMT term that is produced for every supported expression form.
 *
 * The parameterized tables cover all binary operators in both the integer
 * ([IntArithmeticTranslator]) and bit-vector ([BitVectorArithmeticTranslator]) modes,
 * all literal nodes, and a range of compound expressions. The dedicated tests cover
 * the special-cases: `\result`, `\old`/`old`, array-length, handler dispatching,
 * JML quantifiers, JML infix operators, chained comparisons, the array/null
 * comparison fix, and array creation.
 */
@Timeout(120)
class ExprTranslatorTest {

    private val term = SmtTermFactory
    private val parser: JavaParser = JavaParser(ParserConfiguration().setProcessJml(true))

    //region helpers
    private fun parseExpr(src: String): Expression =
        parser.parseExpression<Expression>(src).result.orElseThrow { AssertionError("could not parse: $src") }

    private fun iVar(name: String) = term.variable(SmtType.INT, ResolvedPrimitiveType.INT, name)
    private fun bVar(name: String) = term.variable(SmtType.BOOL, ResolvedPrimitiveType.BOOLEAN, name)
    private fun aVar(name: String) = term.variable(SmtType.Array(SmtType.INT, SmtType.INT), null, name)
    private fun bvVar(name: String) = term.variable(SmtType.BV32, null, name)
    private fun oVar(name: String) = term.variable(SmtType.JAVA_OBJECT, null, name)

    private fun tr(
        src: String,
        env: Map<String, SExpr> = emptyMap(),
        oldEnv: Map<String, SExpr>? = null,
        translator: ArithmeticTranslator? = null,
        callHandler: (MethodCallExpr) -> SExpr = { term.symbol("call") },
        unknownHandler: (NameExpr) -> SExpr = { term.symbol("unk_" + it.nameAsString) },
        fieldHandler: ((SExpr, String, String) -> SExpr?)? = null,
    ): SExpr {
        val q = SmtQuery()
        val t = translator ?: IntArithmeticTranslator(q)
        return ExprTranslator(
            q, t, env, oldEnv ?: emptyMap(), callHandler, unknownHandler,
            fieldHandler ?: { _, _, _ -> null }
        ).tr(parseExpr(src))
    }

    private fun trExpr(
        e: Expression,
        env: Map<String, SExpr> = emptyMap(),
        oldEnv: Map<String, SExpr>? = null,
    ): SExpr {
        val q = SmtQuery()
        return ExprTranslator(q, IntArithmeticTranslator(q), env, oldEnv ?: emptyMap(), { term.symbol("call") }, { term.symbol("unk") }).tr(e)
    }
    //endregion

    //region parameterized: integer binary operators
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("intBinaryOps")
    fun testIntBinaryOperators(src: String, expected: String) {
        assertEquals(expected, tr(src, mapOf("a" to iVar("a"), "b" to iVar("b"))).toString(), src)
    }

    //endregion
    companion object {
        @JvmStatic
        fun intBinaryOps(): Stream<Arguments> = Stream.of(
            Arguments.of("a + b", "(+ a b)"),
            Arguments.of("a - b", "(- a b)"),
            Arguments.of("a * b", "(* a b)"),
            Arguments.of("a / b", "(/ a b)"),
            Arguments.of("a % b", "(mod a b)"),
            Arguments.of("a < b", "(< a b)"),
            Arguments.of("a > b", "(> a b)"),
            Arguments.of("a <= b", "(<= a b)"),
            Arguments.of("a >= b", "(>= a b)"),
            Arguments.of("a == b", "(= a b)"),
            Arguments.of("a != b", "(not (= a b))"),
        )

        @JvmStatic
        fun logicalOps(): Stream<Arguments> = Stream.of(
            Arguments.of("b1 && b2", "(and b1 b2)"),
            Arguments.of("b1 || b2", "(or b1 b2)"),
            Arguments.of("b1 == b2", "(= b1 b2)"),
            Arguments.of("b1 != b2", "(not (= b1 b2))"),
            Arguments.of("!b1", "(not b1)"),
        )

        @JvmStatic
        fun bvBinaryOps(): Stream<Arguments> = Stream.of(
            Arguments.of("a + b", "(bvadd a b)"),
            Arguments.of("a - b", "(bvsub a b)"),
            Arguments.of("a * b", "(bvmul a b)"),
            Arguments.of("a / b", "(bvsdiv a b)"),
            Arguments.of("a % b", "(bvsrem a b)"),
            Arguments.of("a < b", "(bvslt a b)"),
            Arguments.of("a <= b", "(bvsle a b)"),
            Arguments.of("a > b", "(bvsgt a b)"),
            Arguments.of("a >= b", "(bvsge a b)"),
            Arguments.of("a == b", "(= a b)"),
            Arguments.of("a != b", "(not (= a b))"),
            Arguments.of("a & b", "(bvand a b)"),
            Arguments.of("a | b", "(bvor a b)"),
            Arguments.of("a << b", "(bvshl a b)"),
            Arguments.of("a >> b", "(bvashr a b)"),
            Arguments.of("a >>> b", "(bvashr a b)"),
            Arguments.of("a ^ b", "(xor a b)"),
        )

        @JvmStatic
        fun literals(): Stream<Arguments> = Stream.of(
            Arguments.of("true", "true"),
            Arguments.of("false", "false"),
            Arguments.of("0", "0"),
            Arguments.of("42", "42"),
            Arguments.of("2147483648", "2147483648"),
            Arguments.of("1L", "1L"),
            Arguments.of("'a'", "97"),
            Arguments.of("'\\n'", "10"),
            Arguments.of("'\\\\'", "92"),
            Arguments.of("\"hi\"", "hi"),
            Arguments.of("null", "null"),
            Arguments.of("-1", "(- 0 1)"),
        )

        @JvmStatic
        fun compoundExprs(): Stream<Arguments> = Stream.of(
            Arguments.of("x + 1", "(+ x 1)"),
            Arguments.of("x - 1", "(- x 1)"),
            Arguments.of("x * 2", "(* x 2)"),
            Arguments.of("x / 2", "(/ x 2)"),
            Arguments.of("x % 2", "(mod x 2)"),
            Arguments.of("x + y - x", "(- (+ x y) x)"),
            Arguments.of("(x + 1) * 2", "(* (+ x 1) 2)"),
            Arguments.of("2 + 3 * 4", "(+ 2 (* 3 4))"),
            Arguments.of("x == 0 || y > 1", "(or (= x 0) (> y 1))"),
            Arguments.of("x >= 0 && x < 10", "(and (>= x 0) (< x 10))"),
            Arguments.of("x != y && x > y", "(and (not (= x y)) (> x y))"),
            Arguments.of("b && (x > 0 || y > 0)", "(and b (or (> x 0) (> y 0)))"),
            Arguments.of("x % 2 == 0", "(= (mod x 2) 0)"),
            Arguments.of("x / 2 == 0", "(= (/ x 2) 0)"),
            Arguments.of("x * x", "(* x x)"),
            Arguments.of("a[0]", "(select a 0)"),
            Arguments.of("a[i]", "(select a i)"),
            Arguments.of("x + a[i]", "(+ x (select a i))"),
            Arguments.of("a[0] + a[1]", "(+ (select a 0) (select a 1))"),
            Arguments.of("a[x] == 0", "(= (select a x) 0)"),
            Arguments.of("a.length > 0", "(> (int\$length a) 0)"),
            Arguments.of("a.length - 1", "(- (int\$length a) 1)"),
            Arguments.of("x < a.length && x >= 0", "(and (< x (int\$length a)) (>= x 0))"),
            Arguments.of("b && (a.length > 0)", "(and b (> (int\$length a) 0))"),
            Arguments.of("c ? x : y", "(ite c x y)"),
            Arguments.of("c ? 1 : 2", "(ite c 1 2)"),
            Arguments.of("c ? a[i] : x", "(ite c (select a i) x)"),
            Arguments.of("(int) x", "x"),
            Arguments.of("-x", "(- 0 x)"),
            Arguments.of("+x", "x"),
            Arguments.of("(x)", "x"),
            Arguments.of("this", "this"),
            Arguments.of("this == null", "(= this null)"),
            Arguments.of("b == true", "(= b true)"),
            Arguments.of("true && b", "(and true b)"),
            Arguments.of("b || false", "(or b false)"),
        )

    }

    //region parameterized: boolean / logical operators
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("logicalOps")
    fun testLogicalOperators(src: String, expected: String) {
        assertEquals(expected, tr(src, mapOf("b1" to bVar("b1"), "b2" to bVar("b2"))).toString(), src)
    }

    //endregion

    //region parameterized: bit-vector binary operators
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("bvBinaryOps")
    fun testBitVectorOperators(src: String, expected: String) {
        val q = SmtQuery()
        val t = BitVectorArithmeticTranslator(q)
        val env = mapOf("a" to bvVar("a"), "b" to bvVar("b"))
        val e = ExprTranslator(q, t, env, emptyMap(), { term.symbol("call") }, { term.symbol("unk") })
        assertEquals(expected, e.tr(parseExpr(src)).toString(), src)
    }

    //endregion

    //region parameterized: literals
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("literals")
    fun testLiterals(src: String, expected: String) {
        assertEquals(expected, tr(src).toString(), src)
    }

    //endregion

    //region parameterized: compound expressions
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("compoundExprs")
    fun testCompoundExpressions(src: String, expected: String) {
        val env = mapOf(
            "x" to iVar("x"), "y" to iVar("y"), "i" to iVar("i"),
            "b" to bVar("b"), "c" to bVar("c"),
            "a" to aVar("a"),
        )
        assertEquals(expected, tr(src, env).toString(), src)
    }

    //endregion

    //region casts and instanceof
    @Test
    fun testReferenceCastProducesCastTerm() {
        assertEquals("(cast x sort_foo)", tr("(Foo) x", mapOf("x" to iVar("x"))).toString())
    }

    @Test
    fun testCastQualifiesJavaLangException() {
        assertEquals("(cast x sort_java_lang_exception)", tr("(Exception) x", mapOf("x" to iVar("x"))).toString())
    }

    @Test
    fun testCastPreservesQualifiedName() {
        assertEquals("(cast x sort_pkg_foo)", tr("(pkg.Foo) x", mapOf("x" to iVar("x"))).toString())
    }

    @Test
    fun testInstanceOfProducesInstanceofTerm() {
        assertEquals("(instanceof x sort_foo)", tr("x instanceof Foo", mapOf("x" to iVar("x"))).toString())
    }

    @Test
    fun testInstanceOfQualifiesObject() {
        assertEquals("(instanceof x sort_java_lang_object)", tr("x instanceof Object", mapOf("x" to iVar("x"))).toString())
    }

    @Test
    fun testPrimitiveCastIsElided() {
        // numeric casts are width-agnostic in the SMT encoding and must be dropped
        assertEquals("a", tr("(int) a", mapOf("a" to iVar("a"))).toString())
    }

    @Test
    fun testReferenceCastEmitsCastTerm() {
        // a reference cast must be kept: `(cast value sort_C)`
        assertEquals("(cast x sort_box)", tr("(Box) x", mapOf("x" to oVar("x"))).toString())
    }
    //endregion

    //region array / null special comparison
    @Test
    fun testArrayEqualsNullIsFalse() {
        assertEquals("false", tr("a == null", mapOf("a" to aVar("a"))).toString())
    }

    @Test
    fun testArrayNotEqualsNullIsTrue() {
        assertEquals("true", tr("a != null", mapOf("a" to aVar("a"))).toString())
    }

    @Test
    fun testNullEqualsArrayIsFalse() {
        // null on the left-hand side must behave identically
        assertEquals("false", tr("null == a", mapOf("a" to aVar("a"))).toString())
    }

    @Test
    fun testNullNotEqualsArrayIsTrue() {
        assertEquals("true", tr("null != a", mapOf("a" to aVar("a"))).toString())
    }

    @Test
    fun testArrayComparisonWithArrayIsEquality() {
        // two *arrays* compare via store equality, no null special-casing
        val env = mapOf("a" to aVar("a"), "b" to aVar("b"))
        assertEquals("(= a b)", tr("a == b", env).toString())
    }
    //endregion

    //region names: \result, old env, unknown handler
    @Test
    fun testResultResolvesFromEnv() {
        val env = mapOf(ExprTranslator.RESULT to iVar("res"))
        assertEquals("res", tr("\\result", env).toString())
    }

    @Test
    fun testResultWithoutEnvFallsBackToSymbol() {
        assertEquals("\$result", tr("\\result").toString())
    }

    @Test
    fun testNameResolvesThroughEnv() {
        assertEquals("x", tr("x", mapOf("x" to iVar("x"))).toString())
    }

    @Test
    fun testNameFallsBackToThisField() {
        // bare field `f` of the enclosing class resolves via the `this.f` key
        assertEquals("tf", tr("f", mapOf("this.f" to iVar("tf"))).toString())
    }

    @Test
    fun testNameFallsBackToOldEnvironment() {
        // `x` is not in the current env but is in the pre-state env
        assertEquals("x_pre", tr("x", oldEnv = mapOf("x" to iVar("x_pre"))).toString())
    }

    @Test
    fun testUnknownNameGoesToHandler() {
        var invoked = false
        val r = tr("zzz", unknownHandler = { invoked = true; term.symbol("unresolved") })
        assertEquals("unresolved", r.toString())
        assertTrue(invoked, "unknown-handler must be invoked for an unbound name")
    }

    @Test
    fun testCurrentEnvWinsOverOldEnvironment() {
        val env = mapOf("x" to iVar("x_cur"))
        val old = mapOf("x" to iVar("x_pre"))
        assertEquals("x_cur", tr("x", env, old).toString())
    }
    //endregion

    //region field access
    @Test
    fun testFieldLengthUsesArrayLength() {
        assertEquals("(int\$length a)", tr("a.length", mapOf("a" to aVar("a"))).toString())
    }

    @Test
    fun testFieldAccessHandlerIsConsultedFirst() {
        var receiverSeen: SExpr? = null
        var fieldSeen: String? = null
        var scopeSeen: String? = null
        val env = mapOf("o" to iVar("o"))
        val r = tr(
            "o.f", env,
            fieldHandler = { recv, field, scope ->
                receiverSeen = recv; fieldSeen = field; scopeSeen = scope
                term.symbol("handled")
            }
        )
        assertEquals("handled", r.toString())
        assertSame(env["o"], receiverSeen, "handler receives the receiver value")
        assertEquals("f", fieldSeen)
        assertEquals("o", scopeSeen)
    }

    @Test
    fun testFieldAccessFallsBackToFullTextEnvKey() {
        assertEquals("of", tr("o.f", mapOf("o" to iVar("o"), "o.f" to iVar("of"))).toString())
    }

    @Test
    fun testFieldAccessThisFieldEnvKey() {
        assertEquals("tf", tr("this.f", mapOf("this.f" to iVar("tf"))).toString())
    }

    @Test
    fun testFieldAccessFallbackToSelector() {
        assertEquals("(f o)", tr("o.f", mapOf("o" to iVar("o"))).toString())
    }

    @Test
    fun testFieldAccessParenthesizedThisUsesSelector() {
        // `(this).f` is not a ThisExpr scope: the flat this.f constant must NOT win
        val env = mapOf("this.f" to iVar("tf"))
        assertEquals("(f this)", tr("(this).f", env).toString())
    }

    @Test
    fun testFieldAccessChainedFallback() {
        // `p.q.f`: the inner scope itself falls back to a selector, so the outer does too
        val env = mapOf("p" to term.symbol("p"))
        assertEquals("(f (q p))", tr("p.q.f", env).toString())
    }

    @Test
    fun testFieldAccessUsesDefaultHandlerWhenOmitted() {
        // constructing the translator without an explicit field-access handler uses the
        // built-in default (which performs no heap modelling and returns null)
        val q = SmtQuery()
        val e = ExprTranslator(
            q, IntArithmeticTranslator(q),
            mapOf("o" to iVar("o")), emptyMap(),
            { term.symbol("call") }, { term.symbol("unk") }
        )
        assertEquals("(f o)", e.tr(parseExpr("o.f")).toString())
    }

    @Test
    fun testConstructorDefaultsKickedInViaNamedArguments() {
        // omitting oldEnv and the field handler through named arguments must leave the
        // defaults in place: oldEnv == env, and the handler does no heap modelling
        val q = SmtQuery()
        val e = ExprTranslator(
            q, IntArithmeticTranslator(q), mapOf("o" to iVar("o")),
            callHandler = { term.symbol("call") },
            unknownHandler = { term.symbol("unk") },
        )
        assertEquals("(f o)", e.tr(parseExpr("o.f")).toString())
        // \old falls back to the current environment when oldEnv is not supplied
        assertEquals("o", e.tr(parseExpr("\\old(o)")).toString())
    }
    //endregion

    //region method calls: \old / old and call handler
    @Test
    fun testOldCallResolvesPreState() {
        val env = mapOf("x" to iVar("x_cur"))
        val old = mapOf("x" to iVar("x_pre"), "x_cur" to iVar("x_cur"))
        assertEquals("x_pre", tr("old(x)", env, old).toString())
    }

    @Test
    fun testBackslashOldCallResolvesPreState() {
        // the JML keyword `\old` is lexed with the backslash in the name
        val env = mapOf("x" to iVar("x_cur"))
        val old = mapOf("x" to iVar("x_pre"))
        assertEquals("x_pre", tr("\\old(x)", env, old).toString())
    }

    @Test
    fun testOldWithNestedExpression() {
        val env = mapOf("x" to iVar("x_cur"), "y" to iVar("y_cur"))
        val old = mapOf("x" to iVar("x_pre"), "y" to iVar("y_pre"))
        assertEquals("(+ x_pre y_pre)", tr("old(x + y)", env, old).toString())
    }

    @Test
    fun testOrdinaryCallGoesToCallHandler() {
        var seen: MethodCallExpr? = null
        val r = tr("foo(x)", mapOf("x" to iVar("x")), callHandler = { seen = it; term.symbol("result") })
        assertEquals("result", r.toString())
        assertEquals("foo", seen?.nameAsString)
    }

    @Test
    fun testOldWithTwoArgumentsIsOrdinaryCall() {
        // only single-argument `old(x)` is the pre-state form
        val r = tr("old(a, b)", mapOf("a" to iVar("a"), "b" to iVar("b")))
        assertEquals("call", r.toString())
    }
    //endregion

    //region JML infix and chained comparisons
    @Test
    fun testJmlInfixImplication() {
        val e = JmlBinaryInfixExpr(NameExpr("a"), NameExpr("b"), SimpleName("==>"))
        assertEquals("(==> a b)", trExpr(e, mapOf("a" to iVar("a"), "b" to iVar("b"))).toString())
    }

    @Test
    fun testJmlInfixEquivalence() {
        val e = JmlBinaryInfixExpr(NameExpr("a"), NameExpr("b"), SimpleName("<==>"))
        assertEquals("(<==> a b)", trExpr(e, mapOf("a" to iVar("a"), "b" to iVar("b"))).toString())
    }

    @Test
    fun testMultiCompareUnrollsToConjunction() {
        val env = mapOf("a" to iVar("a"), "b" to iVar("b"), "c" to iVar("c"))
        assertEquals("(and (< a b) (< b c))", tr("a < b < c", env).toString())
    }

    @Test
    fun testMultiCompareLeq() {
        val env = mapOf("a" to iVar("a"), "b" to iVar("b"), "c" to iVar("c"))
        assertEquals("(and (<= a b) (<= b c))", tr("a <= b <= c", env).toString())
    }
    //endregion

    //region JML quantifiers
    private fun binder(name: String, type: com.github.javaparser.ast.type.Type) = VariableDeclarator(type, name)

    private fun quantified(
        binderKind: JmlDefaultBinder,
        variables: List<VariableDeclarator>,
        expressions: List<Expression>,
    ): JmlQuantifiedExpr = JmlQuantifiedExpr(
        null, binderKind,
        com.github.javaparser.ast.NodeList(variables),
        com.github.javaparser.ast.NodeList(expressions),
    )

    @Test
    fun testForallRangeGuardsBody() {
        // (\forall int i; 0 <= i && i < 10; a[i] > 0)
        val range = parseExpr("0 <= i && i < 10")
        val body = parseExpr("a[i] > 0")
        val q = quantified(JmlDefaultBinder.FORALL, listOf(binder("i", PrimitiveType.intType())), listOf(range, body))
        val env = mapOf("a" to aVar("a"))
        assertEquals(
            "(forall ((i Int)) (=> (and (<= 0 i) (< i 10)) (and true (> (select a i) 0))))",
            trExpr(q, env).toString()
        )
    }

    @Test
    fun testForallWithoutRange() {
        // (\forall int i; a[i] > 0): the empty range is implied by the (single) body
        val body = parseExpr("a[i] > 0")
        val q = quantified(JmlDefaultBinder.FORALL, listOf(binder("i", PrimitiveType.intType())), listOf(body))
        assertEquals("(forall ((i Int)) (and true (> (select a i) 0)))", trExpr(q, mapOf("a" to aVar("a"))).toString())
    }

    @Test
    fun testExistsConjoinsRangeAndBody() {
        val range = parseExpr("0 <= i && i < 10")
        val body = parseExpr("a[i] == 0")
        val q = quantified(JmlDefaultBinder.EXISTS, listOf(binder("i", PrimitiveType.intType())), listOf(range, body))
        assertEquals(
            "(exists ((i Int)) (and (and true (and (<= 0 i) (< i 10))) (= (select a i) 0)))",
            trExpr(q, mapOf("a" to aVar("a"))).toString()
        )
    }

    @Test
    fun testQuantifiedArrayBinder() {
        // (\forall int[] arr; arr[0] > 0)
        val body = parseExpr("arr[0] > 0")
        val q = quantified(JmlDefaultBinder.FORALL, listOf(binder("arr", ArrayType(PrimitiveType.intType()))), listOf(body))
        assertEquals("(forall ((arr (Array Int Int))) (and true (> (select arr 0) 0)))", trExpr(q).toString())
    }

    @Test
    fun testQuantifiedNonPrimitiveBinderFallsBackToInt() {
        // a non-primitive, non-array binder type defaults to Int
        val body = parseExpr("f > 0")
        val q = quantified(JmlDefaultBinder.FORALL, listOf(binder("f", ClassOrInterfaceType("Foo"))), listOf(body))
        assertEquals("(forall ((f Int)) (and true (> f 0)))", trExpr(q).toString())
    }

    @Test
    fun testQuantifiedExpressionSeesBoundVariables() {
        // the bound variable must shadow any outer environment entry
        val range = parseExpr("0 <= j && j < 5")
        val body = parseExpr("a[j] == j")
        val q = quantified(JmlDefaultBinder.FORALL, listOf(binder("j", ClassOrInterfaceType("Foo"))), listOf(range, body))
        val env = mapOf("a" to aVar("a"), "j" to iVar("outer_j"))
        assertEquals(
            "(forall ((j Int)) (=> (and (<= 0 j) (< j 5)) (and true (= (select a j) j))))",
            trExpr(q, env).toString()
        )
    }

    @Test
    fun testConditionalWithQuantifiedBranchFallsBackToElseType() {
        // a quantifier SExpr carries a null smtType: the conditional must then pick up
        // the else-branch type when assembling the ite node
        val quantifiedThen = quantified(
            JmlDefaultBinder.FORALL,
            listOf(binder("i", PrimitiveType.intType())),
            listOf(parseExpr("i > 0"))
        )
        val cond = com.github.javaparser.ast.expr.ConditionalExpr(parseExpr("true"), quantifiedThen, parseExpr("false"))
        assertEquals(
            "(ite true (forall ((i Int)) (and true (> i 0))) false)",
            trExpr(cond).toString()
        )
    }
    //endregion

    //region array creation
    private fun resolverParser(): JavaParser = JavaParser(
        ParserConfiguration().setProcessJml(true)
            .setSymbolResolver(JavaSymbolSolver(CombinedTypeSolver(ReflectionTypeSolver())))
    )

    @Test
    fun testArrayCreationDeclaresLengthConstraint() {
        val q = SmtQuery()
        val e = ExprTranslator(q, IntArithmeticTranslator(q), emptyMap(), emptyMap(), { term.symbol("call") }, { term.symbol("unk") })
        val node = arrayCreation(resolverParser(), "int[] a = new int[5];")
        val v = e.tr(node)
        assertTrue(v.asSymbolValue().startsWith("anon_array_"), "expected anonymous array constant, got $v")
        val text = q.toString()
        assertTrue(text.contains("(declare-const ${v.asSymbolValue()} (Array Int Int))"), "missing declaration in:\n$text")
        assertTrue(text.contains("(= (int\$length ${v.asSymbolValue()}) 5)"), "missing length assertion in:\n$text")
    }

    @Test
    fun testArrayCreationWithInitializerOmitsLength() {
        val q = SmtQuery()
        val e = ExprTranslator(q, IntArithmeticTranslator(q), emptyMap(), emptyMap(), { term.symbol("call") }, { term.symbol("unk") })
        val node = arrayCreation(resolverParser(), "int[] a = new int[] { 1, 2 };")
        val v = e.tr(node)
        assertTrue(v.asSymbolValue().startsWith("anon_array_"))
        assertTrue(!q.toString().contains("int\$length"), "array initializer must not emit a length assertion")
    }

    @Test
    fun testArrayCreationConstructedWithoutLevelsSkipsLengthGuard() {
        // an ArrayCreationExpr with *no* levels at all (only constructible programmatically)
        // must skip the length assertion but still declare the anonymous array constant
        val q = SmtQuery()
        val e = ExprTranslator(q, IntArithmeticTranslator(q), emptyMap(), emptyMap(), { term.symbol("call") }, { term.symbol("unk") })
        val node = arrayCreation(resolverParser(), "int[] a = new int[5];")
        node.levels = com.github.javaparser.ast.NodeList<com.github.javaparser.ast.ArrayCreationLevel>()
        val v = e.tr(node)
        assertTrue(v.asSymbolValue().startsWith("anon_array_"))
        assertTrue(!q.toString().contains("int\$length"), "empty levels must not emit a length assertion")
    }
    //endregion

    /** Parses a snippet inside a method body so resolution works (node is CU-attached). */
    private fun arrayCreation(parser: JavaParser, body: String): com.github.javaparser.ast.expr.ArrayCreationExpr {
        val cu = parser.parse("class C { void m() { $body } }")
        assertTrue(cu.isSuccessful, cu.problems.toString())
        return cu.result.get()
            .findAll(com.github.javaparser.ast.expr.ArrayCreationExpr::class.java)
            .first()
    }

    //region unsupported expression kinds
    @Test
    fun testDoubleLiteralIsUnsupported() {
        // float/double literals have no representation in the integer SMT sort
        assertThrows(RuntimeException::class.java) { tr("3.14") }
    }

    @Test
    fun testUnaryBitwiseComplementOnIntsIsUnsupportedViaTypeMismatch() {
        // `~x` on an Int sort has no encoding in the integer arithmetic translator
        assertThrows(RuntimeException::class.java) { tr("~x", mapOf("x" to iVar("x"))) }
    }
    //endregion

    //region structural invariants
    @Test
    fun testBooleanTermsCarryBoolType() {
        val r = tr("x > 0", mapOf("x" to iVar("x")))
        assertEquals(SmtType.BOOL, r.smtType)
    }

    @Test
    fun testIntTermsCarryIntType() {
        val r = tr("x + 1", mapOf("x" to iVar("x")))
        assertEquals(SmtType.INT, r.smtType)
    }
    //endregion
}

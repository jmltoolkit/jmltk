/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.WhileStmt
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import io.github.jmltoolkit.smt.Z3
import io.github.jmltoolkit.smt.solver.Solver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.util.stream.Stream

/**
 * Unit tests for the verification-condition *generation* stage (Vcg.kt). Each case
 * runs the full generation pipeline on a small annotated method and either proves
 * the resulting conditions with Z3, expects a falsifiable condition, or expects the
 * generation itself to fail in a specific way (unsupported constructs, missing
 * invariants, bodyless methods). Most cases are exercised in both the UNBOUNDED
 * (mathematical integers) and BOUNDED (bit-vector) arithmetic modes.
 */
@Timeout(180)
class VcgSemanticsTest {
    private val parser: JavaParser
    private val cu by lazy {
        val r = parser.parse(javaClass.getResourceAsStream("/VcgExamples.java")!!)
        assertTrue(r.isSuccessful, r.problems.toString())
        r.result.get()
    }

    init {
        val config = ParserConfiguration()
        config.setProcessJml(true)
        config.setSymbolResolver(
            JavaSymbolSolver(
                CombinedTypeSolver(
                    JavaParserTypeSolver(Path.of("src/test/resources")),
                    ReflectionTypeSolver()
                )
            )
        )
        parser = JavaParser(config)
    }

    private fun method(name: String): MethodDeclaration {
        val cls = cu.types[0].asClassOrInterfaceDeclaration()
        return cls.methods.first { it.nameAsString == name }
    }

    private fun ctor(): ConstructorDeclaration =
        cu.types[0].asClassOrInterfaceDeclaration().constructors.first()

    private fun vcgFor(name: String, options: VcgOptions = VcgOptions()): VcgResult {
        val res = Vcg(VcgContext.of(method(name)), options).verify()
        assertTrue(res.conditions.isNotEmpty(), "no VCs generated for $name")
        return res
    }

    /** Runs Z3 on the query; every VC must be unsat (proven). */
    private fun expectAllProven(result: VcgResult) {
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 not installed")
        val answer = Solver().run(result.query)
        val errors = try {
            answer.consumeErrors()
        } catch (e: Exception) {
            listOf("solver answer not parseable: $answer")
        }
        assertTrue(errors.isEmpty(), "solver errors: $errors\n${result.query}")
        for (vc in result.conditions) {
            assertTrue(answer.isSymbol("unsat"), "VC ${vc.id} (${vc.description}) not proven\n${result.query}")
            answer.consume()
        }
    }

    /** Runs Z3; expects at least one VC to be falsifiable (sat). */
    private fun expectSomeFalsifiable(result: VcgResult) {
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 not installed")
        val answer = Solver().run(result.query)
        var sat = 0
        for (vc in result.conditions) {
            try {
                if (answer.isSymbol("sat")) sat++
            } catch (e: Exception) {
                throw AssertionError("solver answer not parseable: $answer\n${result.query}")
            }
            answer.consume()
        }
        assertTrue(sat > 0, "expected at least one falsifiable VC\n${result.query}")
    }

    private companion object {
        val u = VcgOptions(mode = VerificationMode.UNBOUNDED)
        val b = VcgOptions(mode = VerificationMode.BOUNDED)
        val uInline = u.copy(defaultCallStrategy = CallStrategy.INLINE)
        val uContract = u.copy(defaultCallStrategy = CallStrategy.CONTRACT)
        val RUNTIME_KINDS = setOf("nullcheck", "castcheck", "negative-array-size", "string-index")

        @JvmStatic
        fun proven(): Stream<Arguments> = Stream.of(
            // type resolution fallbacks and unknown members
            Arguments.of("usesUnknownType", u),
            Arguments.of("probeGhostField", u),
            Arguments.of("unknownContractName", u),
            Arguments.of("unknownBodyName", u),
            // instance-field reads/writes through this. and arbitrary receivers
            Arguments.of("setThisField", u),
            Arguments.of("setThisField", b),
            Arguments.of("readCounter", u),
            Arguments.of("readCounter", b),
            Arguments.of("writeReadCounter", u),
            Arguments.of("writeReadCounter", b),
            // static members
            Arguments.of("bumpStatic", u),
            Arguments.of("bumpStatic", b),
            // statements: havoc, nested loops, break/continue loop contracts
            Arguments.of("havocAndNestedLoops", u.copy(defaultUnrollDepth = 3)),
            Arguments.of("breakAndContinue", u),
            Arguments.of("tryFinallyOnly", u),
            Arguments.of("tryFinallyOnly", b),
            Arguments.of("multiCatchFinally", u),
            Arguments.of("multiCatchFinally", b),
            // JML statements in the body
            Arguments.of("bodyAssert", u),
            Arguments.of("bodyAssert", b),
            // expression forms inside statements
            Arguments.of("arrayNullInStmt", u),
            Arguments.of("arrayNullInStmt", b),
            Arguments.of("miscExprs", u),
            Arguments.of("miscExprs", b),
            Arguments.of("declareInThen", u),
            Arguments.of("ifThenPhi", u),
            Arguments.of("doubleReturn", u),
            // object allocation and exceptions
            Arguments.of("newObject", u),
            Arguments.of("newObject", b),
            Arguments.of("throwNew", u),
            Arguments.of("throwNew", b),
            // calls: unresolved, inlined, contract-based, recursion depth limit
            Arguments.of("callUnresolved", u),
            Arguments.of("callInlineMixedArgs", uInline),
            Arguments.of("level0", uInline.copy(maxInlineDepth = 1)),
            Arguments.of("callBumpEverything", uContract),
            Arguments.of("callAssignableField", uContract),
            Arguments.of("callAssignableArray", uContract),
            Arguments.of("callAssignableCast", uContract),
            Arguments.of("callAssignableNothing", uContract),
            // loops in detail
            Arguments.of("doWhileCount", u),
            Arguments.of("doWhileCount", b),
            Arguments.of("unrolledContinue", b),
            Arguments.of("unrolledLoopWithBreak", b.copy(defaultUnrollDepth = 10)),
            Arguments.of("linearCount", u),
            Arguments.of("linearCount", b),
            // bounded (bit-vector) arithmetic and runtime checks
            Arguments.of("boundedMul", b.copy(checkOverflow = true)),
            Arguments.of("boundedAdd", b.copy(checkOverflow = true, checkDivision = true)),
            Arguments.of("boundedDiv", b.copy(checkDivision = true)),
            Arguments.of("boundedStore", b.copy(checkIndex = true)),
            Arguments.of("storeFirst", b.copy(checkIndex = true)),
            // regression against the classic end-to-end examples, in either mode
            Arguments.of("abs", u),
            Arguments.of("sumInvariant", u),
            // sumInvariant accumulates n*(n-1)/2, which overflows a bit-vector for unbounded n:
            // in bounded mode the invariant s >= 0 is only provable for the bounded unrolled variant
            Arguments.of("sumBounded", b.copy(defaultUnrollDepth = 6)),
            Arguments.of("countUp", u),
            Arguments.of("countUp", b.copy(defaultUnrollDepth = 6)),
            Arguments.of("storeFirst", u.copy(checkIndex = true)),
            Arguments.of("sumArray", u),
            // like sumInvariant, unbounded accumulation overflows a bit-vector; use the bounded variant
            Arguments.of("sumBoundedArr", b.copy(defaultUnrollDepth = 6)),
            Arguments.of("binarySearch", u.copy(defaultUnrollDepth = 4)),
            Arguments.of("binarySearch", b.copy(defaultUnrollDepth = 4)),
            Arguments.of("divideByItselfWithTry", u),
            Arguments.of("setBox", u),
            Arguments.of("sumAndMax", u),
            Arguments.of("loopContractBreak", u),
            Arguments.of("callBumpBoxValue", uContract),
            Arguments.of("callBumpBoxValueInline", uInline),
            // corner cases: MIN/MAX literals and boundary arithmetic
            Arguments.of("intMinLiteral", u),
            Arguments.of("intMinLiteral", b),
            Arguments.of("intMaxLiteral", u),
            Arguments.of("intMaxLiteral", b),
            Arguments.of("nearMax", u),
            Arguments.of("nearMax", b),
            Arguments.of("boundedIncrement", u),
            Arguments.of("boundedIncrement", b),
            Arguments.of("maxOverflow", u),
            Arguments.of("maxOverflow", b),
            // bounded arithmetic wraps MIN_VALUE - 1 to MAX_VALUE (falsifiable in
            // unbounded mode, where subtraction does not wrap)
            Arguments.of("minUnderflowWrap", b),
            // instanceof as a statement (stored in a local and read in a branch)
            Arguments.of("stmtInstanceof", u),
            Arguments.of("stmtInstanceof", b),
            Arguments.of("stmtInstanceofCount", u),
            Arguments.of("stmtInstanceofCount", b),
            // returns/continues/breaks combined with loops and try-catch-finally
            Arguments.of("returnInsideLoop", u),
            Arguments.of("returnInsideLoop", b),
            Arguments.of("plainReturnInLoop", u),
            Arguments.of("plainReturnInLoop", b),
            Arguments.of("tryBreakContinue", u),
            Arguments.of("tryBreakContinue", b),
            Arguments.of("continueCountOnly", u),
            Arguments.of("continueCountOnly", b),
            Arguments.of("nonNormalLoopExit", u),
            Arguments.of("nonNormalLoopExit", b),
            Arguments.of("nestedTry", u),
            Arguments.of("nestedTry", b),
            Arguments.of("tryReturnFinally", u),
            Arguments.of("tryReturnFinally", b),
            // coverage batch: targeted engine branches
            Arguments.of("customExceptionCatch", u),
            Arguments.of("customExceptionCatch", b),
            Arguments.of("qualifiedCatch", u),
            Arguments.of("arrayStoreInLoop", u.copy(defaultUnrollDepth = 4)),
            Arguments.of("arrayNullInStmt2", u),
            Arguments.of("arrayNullInStmt2", b),
            // runtime-checks parity: subtraction overflow, remainder division
            Arguments.of("boundedSub", b.copy(checkOverflow = true)),
            Arguments.of("boundedRem", b.copy(checkDivision = true)),
            Arguments.of("boundedDiv", b.copy(checkOverflow = true, checkDivision = true)),
            // loop contracts with only one abrupt-clause family present
            Arguments.of("loopContractContinueOnly", u),
            Arguments.of("loopContractContinueNoClause", u),
            Arguments.of("loopContractBreakNoClause", u),
            // contract calls: name/local assignable clauses, \everything arrays, receivers
            Arguments.of("callAssignableName", uContract),
            Arguments.of("callAssignableLocal", uContract),
            Arguments.of("callBumpEverythingArr", uContract),
            Arguments.of("callStaticContract", uContract),
            Arguments.of("callThisShorthand", uContract),
            Arguments.of("callObjParam", uContract),
            // switches: matching, fall-through, break scoping, in a loop, continue
            Arguments.of("switchBasic", u),
            Arguments.of("switchBasic", b),
            Arguments.of("switchFallThrough", u),
            Arguments.of("switchFallThrough", b),
            Arguments.of("switchBreakStops", u),
            Arguments.of("switchBreakStops", b),
            Arguments.of("switchBreakStaysInLoop", u),
            Arguments.of("switchBreakStaysInLoop", b),
            Arguments.of("switchContinueInLoop", u),
            Arguments.of("switchContinueInLoop", b),
            Arguments.of("callInlineSwitch", uInline),
            // inlined calls: nested bodies, aliased array params, fresh receivers, static
            Arguments.of("callStaticInline", uInline),
            Arguments.of("inlineNested", uInline),
            Arguments.of("inlineArrayParam", uInline),
            Arguments.of("callNewBox", uInline),
            Arguments.of("selfRecInline", uInline.copy(maxInlineDepth = 1)),
            // heap field selection on arbitrary receivers and unresolved types
            Arguments.of("writeHolderArray", u),
            Arguments.of("readHolderArray", u),
            Arguments.of("readBoxField", u),
            Arguments.of("readUnknownField", u),
            // array creation with symbolic length, unresolved return/catch types
            Arguments.of("makeArray", u),
            Arguments.of("returnsUnknown", u),
            Arguments.of("catchUnknownType", u),
            // overload resolution in contract calls
            Arguments.of("overloadCaller", u),
        )

        @JvmStatic
        fun falsifiable(): Stream<Arguments> = Stream.of(
            Arguments.of("overflowDetected", b.copy(checkOverflow = true)),
            Arguments.of("boundedOverflow", b.copy(checkOverflow = true)),
            Arguments.of("boundedOob", b.copy(checkIndex = true)),
            // falsifiable-by-design: the specification is wrong (the engine is right):
            // returning early from a loop leaves a lower accumulator than the spec claims
            Arguments.of("returnEarlyNoContinue", u),
            Arguments.of("returnEarlyNoContinue", b),
            // break inside try: for n >= 2 the accumulator reaches 2, not 1
            Arguments.of("tryBreakOnly", u),
            Arguments.of("tryBreakOnly", b),
            // continue + return: for n < 4 the loop never returns and yields 0
            Arguments.of("continueThenReturn", u),
            Arguments.of("continueThenReturn", b),
            // two continues: for n >= 4 the accumulator reaches 2, not 1
            Arguments.of("loopContinueNoTry", u),
            Arguments.of("loopContinueNoTry", b),
            // break without try: for n = 2 the accumulator reaches 2, not 1
            Arguments.of("loopBreakNoTry", u),
            Arguments.of("loopBreakNoTry", b),
            // nested catch/finally around an expression: final value is 10/(x+1)+2, not 0
            Arguments.of("nestedCatchFinally", u),
            Arguments.of("nestedCatchFinally", b),
            // unbounded MIN_VALUE - 1 does not wrap: result is -2147483649, not -2147483647
            Arguments.of("minUnderflow", u),
            Arguments.of("minUnderflow", b),
            // a wrong switch postcondition exposes the non-matching branch
            Arguments.of("switchWrong", u),
            Arguments.of("switchWrong", b),
        )
    }

    @ParameterizedTest(name = "[{index}] {0} {1}")
    @MethodSource("proven")
    fun testGenerationProven(name: String, options: VcgOptions) {
        expectAllProven(vcgFor(name, options))
    }

    @ParameterizedTest(name = "[{index}] {0} {1}")
    @MethodSource("falsifiable")
    fun testGenerationFalsifiable(name: String, options: VcgOptions) {
        expectSomeFalsifiable(vcgFor(name, options))
    }

    //region dedicated generation cases

    @Test
    fun testConstructorVerified() {
        val res = Vcg(VcgContext.of(ctor()), u).verify()
        assertEquals(1, res.conditions.size, "constructor should emit a single postcondition VC")
        expectAllProven(res)
    }

    @Test
    fun testSwitchBasicProven() {
        // a plain (non-loop) switch with a break and a default
        expectAllProven(vcgFor("switchBasic", u))
        expectAllProven(vcgFor("switchBasic", b))
    }

    @Test
    fun testSwitchContinueTargetsLoopNotSwitch() {
        // a `continue` inside a switch case must jump to the enclosing loop head; the
        // loop contract/invariant machinery and the unroll merge must both be sound
        expectAllProven(vcgFor("switchContinueInLoop", u))
    }

    @Test
    fun testSwitchInsideInlinedCallee() {
        // inlining a callee whose body contains a switch now works end to end
        expectAllProven(vcgFor("callInlineSwitch", uInline))
    }

    @Test
    fun testAutoboxingRejectedByGeneration() {
        // auto- (un-)boxing of a boxed parameter is not modeled; the engine must
        // fail with an explicit "Could not handle types" error instead of producing
        // a silently wrong condition
        val ex = assertThrows(RuntimeException::class.java) { vcgFor("unboxedIncrement", u) }
        assertTrue(ex.message!!.contains("Could not handle types"), ex.message)
    }

    @Test
    fun testBoxedReturnTypeRejectedByGeneration() {
        // a boxed (reference) return type such as Integer is not modeled soundly;
        // generation must reject it up front instead of emitting an ill-sorted query
        val ex = assertThrows(RuntimeException::class.java) { vcgFor("boxedIdentity", u) }
        assertTrue(ex.message!!.contains("boxed return type"), ex.message)
    }

    @Test
    fun testLoopWithoutInvariantRejectedWhenAbstracted() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            vcgFor("loopWithoutInvariant", VcgOptions(mode = VerificationMode.UNBOUNDED, defaultLoopStrategy = LoopStrategy.INVARIANT))
        }
        assertTrue(ex.message!!.contains("invariant"), ex.message)
    }

    @Test
    fun testBodylessMethodRejected() {
        val r = parser.parse(
            """
            interface WithAbstract {
                //@ requires x >= 0;
                //@ ensures \result >= 0;
                public int m(int x);
            }
            """.trimIndent()
        )
        assertTrue(r.isSuccessful, r.problems.toString())
        val md = r.result.get().findAll(MethodDeclaration::class.java).first()
        val ex = assertThrows(IllegalArgumentException::class.java) {
            Vcg(VcgContext.of(md), u).verify()
        }
        assertTrue(ex.message!!.contains("no body"), ex.message)
    }

    @Test
    fun testMethodWithoutContractHasNoObligations() {
        val res = Vcg(VcgContext.of(method("incScalar")), u).verify()
        assertTrue(res.conditions.isEmpty(), "no contract means no obligations")
    }

    @Test
    fun testObligationIdsAreReadableAndSequential() {
        // ids follow <class>#<method>(<params>)#<kind>-<n>@<line>:<column>
        val res = vcgFor("bodyAssert", u)
        val idRe = Regex("^VcgExamples#bodyAssert\\(int\\)#[a-z-]+-\\d+@\\d+:\\d+$")
        res.conditions.forEachIndexed { i, vc ->
            assertTrue(idRe.matches(vc.id), "unexpected id format: ${vc.id}")
            assertEquals("VcgExamples#bodyAssert(int)", vc.id.substringBeforeLast('#'))
            assertTrue(vc.id.contains("#${vc.kind}-${i + 1}@"), "running number must increment: ${vc.id}")
        }
        assertEquals("postcondition", res.conditions.last().description)
        assertEquals("postcondition", res.conditions.last().kind)
    }

    @Test
    fun testArrayStoreBoundObligationIsEmitted() {
        val res = vcgFor("boundedStore", b.copy(checkIndex = true))
        assertTrue(res.conditions.any { it.description.contains("array store bound") })
        assertTrue(res.conditions.any { it.description.contains("postcondition") })
    }

    @Test
    fun testDivisionCheckObligationIsEmitted() {
        val res = vcgFor("boundedDiv", b.copy(checkDivision = true))
        assertTrue(res.conditions.any { it.description.contains("division by zero") })
    }

    @Test
    fun testOverflowCheckObligationIsEmitted() {
        val res = vcgFor("boundedAdd", b.copy(checkOverflow = true))
        assertTrue(res.conditions.any { it.description.contains("arithmetic overflow") })
    }

    //region implicit runtime-exception checks (opt-in flags)

    @Test
    fun testRuntimeChecksOffByDefault() {
        // the new checks must be silent unless explicitly requested
        for (name in listOf("derefUnchecked", "fieldDerefUnchecked", "castWrongM", "newArrayNeg", "charAtUnchecked")) {
            val res = vcgFor(name, u)
            assertTrue(
                res.conditions.none { it.kind in RUNTIME_KINDS },
                "unexpected runtime condition in $name: " + res.conditions.map { "${it.kind}:${it.description}" }
            )
        }
    }

    @Test
    fun testNullCheckEmittedAndProvable() {
        expectAllProven(vcgFor("derefOk", u.copy(checkNull = true)))
        expectAllProven(vcgFor("derefOk", b.copy(checkNull = true)))
        expectAllProven(vcgFor("fieldDerefOk", u.copy(checkNull = true)))
        expectAllProven(vcgFor("fieldDerefOk", b.copy(checkNull = true)))
    }

    @Test
    fun testNullCheckFalsifiableOnUnannotatedDereference() {
        expectSomeFalsifiable(vcgFor("derefUnchecked", u.copy(checkNull = true)))
        expectSomeFalsifiable(vcgFor("derefUnchecked", b.copy(checkNull = true)))
        expectSomeFalsifiable(vcgFor("fieldDerefUnchecked", u.copy(checkNull = true)))
    }

    @Test
    fun testCastCheckProvenAndFalsifiable() {
        expectAllProven(vcgFor("castOk", u.copy(checkCast = true)))
        expectAllProven(vcgFor("castOk", b.copy(checkCast = true)))
        expectSomeFalsifiable(vcgFor("castWrongM", u.copy(checkCast = true)))
        expectSomeFalsifiable(vcgFor("castWrongM", b.copy(checkCast = true)))
    }

    @Test
    fun testNegativeArraySizeCheckProven() {
        // The engine models every array with a non-negative length (the `length >= 0`
        // axiom that keeps bit-vector bounds checks sound), and the created array's
        // length is bound to the dimension. The negative-array-size obligation
        // `guard -> dim >= 0` is therefore implied by the model and always provable;
        // the test asserts it is emitted and generation stays valid in both modes.
        expectAllProven(vcgFor("newArrayOk", u.copy(checkNegativeArraySize = true)))
        expectAllProven(vcgFor("newArrayOk", b.copy(checkNegativeArraySize = true)))
        val resU = vcgFor("newArrayNeg", u.copy(checkNegativeArraySize = true))
        assertTrue(
            resU.conditions.any { it.kind == "negative-array-size" },
            "expected a negative-array-size obligation, got " + resU.conditions.map { it.kind }
        )
        val resB = vcgFor("newArrayNeg", b.copy(checkNegativeArraySize = true))
        assertTrue(resB.conditions.any { it.kind == "negative-array-size" })
        expectAllProven(resB)
    }

    @Test
    fun testStringIndexProvenAndFalsifiable() {
        expectAllProven(vcgFor("charAtOk", u.copy(checkStringIndex = true)))
        expectAllProven(vcgFor("charAtOk", b.copy(checkStringIndex = true)))
        expectAllProven(vcgFor("subOk", u.copy(checkStringIndex = true)))
        expectSomeFalsifiable(vcgFor("charAtUnchecked", u.copy(checkStringIndex = true)))
        expectSomeFalsifiable(vcgFor("charAtUnchecked", b.copy(checkStringIndex = true)))
    }

    @Test
    fun testStringLengthIsModelReadUnderFlag() {
        // `String.length()` becomes an uninterpreted length read instead of an
        // opaque contract-call result; the model length of a literal is bound.
        val res = vcgFor("derefOk", u.copy(checkStringIndex = true))
        // the postcondition is trivial; no runtime condition is emitted for a null-safe
        // dereference when only the string-index feature is enabled
        assertTrue(res.conditions.all { it.kind == "postcondition" }, res.conditions.map { it.kind }.toString())
        // the length reads must come from the stringLength function
        assertTrue(res.query.toString().contains("stringLength"), "expected a stringLength model read")
    }

    //endregion

    @Test
    fun testBodyAssertObligationCarriesSourceRange() {
        val res = vcgFor("bodyAssert", u)
        val a = res.conditions.firstOrNull { it.description.contains("assert") }
        assertTrue(a != null, "expected an assert obligation")
        assertTrue(a!!.range != null, "assert obligation should carry a source range")
    }

    @Test
    fun testPreconditionObligationCarriesCalleeName() {
        val res = vcgFor("useInc", uContract)
        assertTrue(res.conditions.any { it.description.contains("precondition of inc") })
    }

    //endregion

    //region options and context helper coverage

    @Test
    fun testExplicitLoopStrategyOverride() {
        // an annotated loop with an explicit loopStrategies entry must use the override
        // instead of the annotation-driven default (LOOP_CONTRACT is never chosen here)
        val loop = method("sumInvariant").findAll(WhileStmt::class.java).first()
        val opts = VcgOptions(
            mode = VerificationMode.UNBOUNDED,
            defaultLoopStrategy = LoopStrategy.UNROLL,
            loopStrategies = mapOf<Node, LoopStrategy>(loop to LoopStrategy.UNROLL),
            loopUnrollDepth = mapOf<Node, Int>(loop to 5),
        )
        expectAllProven(vcgFor("sumInvariant", opts))
    }

    @Test
    fun testExplicitUnannotatedLoopStrategyOverride() {
        // a *plain* (unannotated) loop forced to UNROLL through the options map
        val loop = method("loopWithoutInvariant").findAll(WhileStmt::class.java).first()
        val opts = VcgOptions(
            mode = VerificationMode.UNBOUNDED,
            loopStrategies = mapOf<Node, LoopStrategy>(loop to LoopStrategy.UNROLL),
            loopUnrollDepth = mapOf<Node, Int>(loop to 3),
        )
        val res = vcgFor("loopWithoutInvariant", opts)
        assertEquals(1, res.conditions.size, "only the postcondition obligation")
        expectAllProven(res)
    }

    @Test
    fun testExplicitCallStrategyOverride() {
        // a call whose callStrategies entry forces INLINE despite the default CONTRACT
        val call = method("useInc").findAll(MethodCallExpr::class.java).first { it.nameAsString == "inc" }
        val opts = VcgOptions(
            mode = VerificationMode.UNBOUNDED,
            defaultCallStrategy = CallStrategy.CONTRACT,
            callStrategies = mapOf(call to CallStrategy.INLINE),
        )
        expectAllProven(vcgFor("useInc", opts))
    }

    @Test
    fun testOptionsLookupBranches() {
        val loop = method("loopWithoutInvariant").findAll(WhileStmt::class.java).first()
        val otherLoop = method("sumBounded").findAll(WhileStmt::class.java).first()
        val call = method("useInc").findAll(MethodCallExpr::class.java).first { it.nameAsString == "inc" }
        val otherCall = method("callIncCounter").findAll(MethodCallExpr::class.java).first()
        val opts = VcgOptions(
            mode = VerificationMode.UNBOUNDED,
            loopStrategies = mapOf<Node, LoopStrategy>(loop to LoopStrategy.INVARIANT),
            loopUnrollDepth = mapOf<Node, Int>(loop to 7),
            callStrategies = mapOf(call to CallStrategy.INLINE),
        )
        assertEquals(LoopStrategy.INVARIANT, opts.loopStrategy(loop))
        assertEquals(LoopStrategy.UNROLL, opts.loopStrategy(otherLoop))
        assertEquals(7, opts.unrollDepth(loop))
        assertEquals(VcgOptions().defaultUnrollDepth, opts.unrollDepth(otherLoop))
        assertEquals(CallStrategy.INLINE, opts.callStrategy(call))
        assertEquals(CallStrategy.CONTRACT, opts.callStrategy(otherCall))
    }

    @Test
    fun testOptionsValueSemantics() {
        val a = VcgOptions()
        val b = VcgOptions()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        val c = a.copy(mode = VerificationMode.UNBOUNDED)
        assertNotEquals(a, c)
        assertTrue(a.toString().contains("VcgOptions"), a.toString())
    }

    @Test
    fun testVcgContextEnclosingTypeRejectedForOrphanCallable() {
        // a callable that is not enclosed by any type declaration must be rejected
        // when its enclosing type is requested
        val r = parser.parseBodyDeclaration<MethodDeclaration>("public void m(int x) { }")
        assertTrue(r.isSuccessful, r.problems.toString())
        val md = r.result.get() as MethodDeclaration
        val ctx = VcgContext(md, com.github.javaparser.ast.jml.clauses.JmlContract())
        val ex = assertThrows(IllegalArgumentException::class.java) { ctx.enclosingType }
        assertTrue(ex.message!!.contains("enclosed"), ex.message)
    }
    //endregion
}

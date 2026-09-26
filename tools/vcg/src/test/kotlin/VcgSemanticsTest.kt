/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import io.github.jmltoolkit.smt.Z3
import io.github.jmltoolkit.smt.solver.Solver
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun testSwitchIsRejectedByGeneration() {
        val ex = assertThrows(UnsupportedOperationException::class.java) { vcgFor("switchNotSupported", u) }
        assertTrue(ex.message!!.contains("not yet supported"), ex.message)
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
    fun testObligationIdsAreSequential() {
        val res = vcgFor("bodyAssert", u)
        res.conditions.forEachIndexed { i, vc -> assertEquals("vc${i + 1}", vc.id) }
        assertEquals("postcondition", res.conditions.last().description)
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
}

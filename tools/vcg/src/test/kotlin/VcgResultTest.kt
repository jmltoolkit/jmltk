/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.Range
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import io.github.jmltoolkit.smt.SmtQuery
import io.github.jmltoolkit.smt.Z3
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.solver.AppendableTo
import io.github.jmltoolkit.smt.solver.SExprParser
import io.github.jmltoolkit.smt.solver.Solver
import io.github.jmltoolkit.smt.solver.SolverAnswer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
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
 * Tests for the *result-checking* stage (VcgResult.kt): turning the generated
 * verification conditions into solver verdicts.
 *
 * Most cases drive [VcgResult.check] / [VcgResult.checkProgressive] /
 * [VcgResult.failedConditions] with a stub solver so the verdict mapping,
 * memoization and progress reporting are deterministic and independent of Z3.
 * A handful of end-to-end cases run against the real solver (guarded by a Z3
 * installation assumption). Each [provenFixtures] / [falsifiableFixtures] /
 * [unknownFixtures] row is exercised with all-`unsat` / all-`sat` / all-`unknown`
 * answers, giving 150+ cases over the same fixture bank as the generation suite
 * and full line/branch coverage of VcgResult.kt.
 */
@Timeout(180)
class VcgResultTest {
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

    private fun unsat(n: Int): List<SExpr> = List(n) { SExprParser.parse("unsat")!! }
    private fun sat(n: Int): List<SExpr> = List(n) { SExprParser.parse("sat")!! }
    private fun unknown(n: Int): List<SExpr> = List(n) { SExprParser.parse("unknown")!! }

    private fun allStatuses(result: VcgResult, status: VcgResult.Status): Boolean =
        result.conditions.isNotEmpty() && result.conditions.all { c ->
            result.check()[c.id] == status
        }

    /** A solver stub that counts invocations and returns canned verdicts. */
    private class StubSolver(answers: List<SExpr>) : Solver() {
        val answers: List<SExpr> = answers
        var invocations = 0

        override fun run(
            query: AppendableTo,
            onForm: ((SExpr) -> Unit)?,
            isCancelled: () -> Boolean,
            timeoutMillis: Long,
        ): SolverAnswer {
            invocations++
            answers.forEach { decl -> onForm?.invoke(decl) }
            return SolverAnswer(answers)
        }
    }

    //region parameterized verdict mapping over the shared fixture bank

    companion object {
        val u = VcgOptions(mode = VerificationMode.UNBOUNDED)
        val b = VcgOptions(mode = VerificationMode.BOUNDED)
        val uInline = u.copy(defaultCallStrategy = CallStrategy.INLINE)
        val uContract = u.copy(defaultCallStrategy = CallStrategy.CONTRACT)

        /** Same fixture/options rows as VcgSemanticsTest.proven(). */
        @JvmStatic
        fun provenFixtures(): Stream<Arguments> = Stream.of(
            Arguments.of("usesUnknownType", u),
            Arguments.of("probeGhostField", u),
            Arguments.of("unknownContractName", u),
            Arguments.of("unknownBodyName", u),
            Arguments.of("setThisField", u),
            Arguments.of("setThisField", b),
            Arguments.of("readCounter", u),
            Arguments.of("readCounter", b),
            Arguments.of("writeReadCounter", u),
            Arguments.of("writeReadCounter", b),
            Arguments.of("bumpStatic", u),
            Arguments.of("bumpStatic", b),
            Arguments.of("havocAndNestedLoops", u.copy(defaultUnrollDepth = 3)),
            Arguments.of("breakAndContinue", u),
            Arguments.of("tryFinallyOnly", u),
            Arguments.of("tryFinallyOnly", b),
            Arguments.of("multiCatchFinally", u),
            Arguments.of("multiCatchFinally", b),
            Arguments.of("bodyAssert", u),
            Arguments.of("bodyAssert", b),
            Arguments.of("arrayNullInStmt", u),
            Arguments.of("arrayNullInStmt", b),
            Arguments.of("miscExprs", u),
            Arguments.of("miscExprs", b),
            Arguments.of("declareInThen", u),
            Arguments.of("ifThenPhi", u),
            Arguments.of("doubleReturn", u),
            Arguments.of("newObject", u),
            Arguments.of("newObject", b),
            Arguments.of("throwNew", u),
            Arguments.of("throwNew", b),
            Arguments.of("callUnresolved", u),
            Arguments.of("callInlineMixedArgs", uInline),
            Arguments.of("level0", uInline.copy(maxInlineDepth = 1)),
            Arguments.of("callBumpEverything", uContract),
            Arguments.of("callAssignableField", uContract),
            Arguments.of("callAssignableArray", uContract),
            Arguments.of("callAssignableCast", uContract),
            Arguments.of("callAssignableNothing", uContract),
            Arguments.of("doWhileCount", u),
            Arguments.of("doWhileCount", b),
            Arguments.of("unrolledContinue", b),
            Arguments.of("unrolledLoopWithBreak", b.copy(defaultUnrollDepth = 10)),
            Arguments.of("linearCount", u),
            Arguments.of("linearCount", b),
            Arguments.of("boundedMul", b.copy(checkOverflow = true)),
            Arguments.of("boundedAdd", b.copy(checkOverflow = true, checkDivision = true)),
            Arguments.of("boundedDiv", b.copy(checkDivision = true)),
            Arguments.of("boundedStore", b.copy(checkIndex = true)),
            Arguments.of("storeFirst", b.copy(checkIndex = true)),
            Arguments.of("abs", u),
            Arguments.of("sumInvariant", u),
            Arguments.of("sumBounded", b.copy(defaultUnrollDepth = 6)),
            Arguments.of("countUp", u),
            Arguments.of("countUp", b.copy(defaultUnrollDepth = 6)),
            Arguments.of("storeFirst", u.copy(checkIndex = true)),
            Arguments.of("sumArray", u),
            Arguments.of("sumBoundedArr", b.copy(defaultUnrollDepth = 6)),
            Arguments.of("binarySearch", u.copy(defaultUnrollDepth = 4)),
            Arguments.of("binarySearch", b.copy(defaultUnrollDepth = 4)),
            Arguments.of("divideByItselfWithTry", u),
            Arguments.of("setBox", u),
            Arguments.of("sumAndMax", u),
            Arguments.of("loopContractBreak", u),
            Arguments.of("callBumpBoxValue", uContract),
            Arguments.of("callBumpBoxValueInline", uInline),
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
            Arguments.of("minUnderflowWrap", b),
            Arguments.of("stmtInstanceof", u),
            Arguments.of("stmtInstanceof", b),
            Arguments.of("stmtInstanceofCount", u),
            Arguments.of("stmtInstanceofCount", b),
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

        /** Same fixture/options rows as VcgSemanticsTest.falsifiable(). */
        @JvmStatic
        fun falsifiableFixtures(): Stream<Arguments> = Stream.of(
            Arguments.of("overflowDetected", b.copy(checkOverflow = true)),
            Arguments.of("boundedOverflow", b.copy(checkOverflow = true)),
            Arguments.of("boundedOob", b.copy(checkIndex = true)),
            Arguments.of("returnEarlyNoContinue", u),
            Arguments.of("returnEarlyNoContinue", b),
            Arguments.of("tryBreakOnly", u),
            Arguments.of("tryBreakOnly", b),
            Arguments.of("continueThenReturn", u),
            Arguments.of("continueThenReturn", b),
            Arguments.of("loopContinueNoTry", u),
            Arguments.of("loopContinueNoTry", b),
            Arguments.of("loopBreakNoTry", u),
            Arguments.of("loopBreakNoTry", b),
            Arguments.of("nestedCatchFinally", u),
            Arguments.of("nestedCatchFinally", b),
            Arguments.of("minUnderflow", u),
            Arguments.of("minUnderflow", b),
        )

        /** Small set of (method, options) pairs for the all-`unknown` mapping. */
        @JvmStatic
        fun unknownFixtures(): Stream<Arguments> = Stream.of(
            Arguments.of("abs", u),
            Arguments.of("countUp", u),
            Arguments.of("setThisField", u),
            Arguments.of("readCounter", b),
            Arguments.of("tryFinallyOnly", u),
            Arguments.of("miscExprs", u),
            Arguments.of("newObject", u),
            Arguments.of("throwNew", b),
            Arguments.of("doWhileCount", u),
            Arguments.of("linearCount", b),
            Arguments.of("intMinLiteral", u),
            Arguments.of("nearMax", b),
            Arguments.of("boundedIncrement", u),
            Arguments.of("returnInsideLoop", u),
            Arguments.of("nestedTry", u),
            Arguments.of("stmtInstanceofCount", u),
            Arguments.of("minUnderflow", u),
            Arguments.of("returnEarlyNoContinue", b),
            Arguments.of("loopBreakNoTry", u),
            Arguments.of("nestedCatchFinally", u),
        )
    }

    @ParameterizedTest(name = "[{index}] {0} {1} all-unsat -> PROVEN")
    @MethodSource("provenFixtures")
    fun provenFixturesAreMarkedProvenByUnsat(name: String, options: VcgOptions) {
        val res = vcgFor(name, options)
        val solver = StubSolver(unsat(res.conditions.size))
        val status = res.check(solver)
        assertEquals(1, solver.invocations, "check() runs the solver exactly once")
        res.conditions.forEach { c ->
            assertEquals(VcgResult.Status.PROVEN, status[c.id], "condition ${c.id} must be PROVEN")
        }
        // memoized: second check and failedConditions reuse the cache
        val again = res.check(solver)
        assertEquals(status, again)
        assertTrue(res.failedConditions(solver).isEmpty(), "no failed conditions when all answers are unsat")
        assertEquals(1, solver.invocations, "memoized reads must not re-invoke the solver")
    }

    @ParameterizedTest(name = "[{index}] {0} {1} all-sat -> FAILED")
    @MethodSource("falsifiableFixtures")
    fun falsifiableFixturesAreMarkedFailedBySat(name: String, options: VcgOptions) {
        val res = vcgFor(name, options)
        val solver = StubSolver(sat(res.conditions.size))
        val status = res.check(solver)
        assertEquals(1, solver.invocations)
        res.conditions.forEach { c ->
            assertEquals(VcgResult.Status.FAILED, status[c.id], "condition ${c.id} must be FAILED")
        }
        val failed = res.failedConditions(solver)
        assertEquals(res.conditions, failed, "all conditions must be reported as failed")
        assertEquals(1, solver.invocations, "failedConditions must reuse the memoized check")
    }

    @ParameterizedTest(name = "[{index}] {0} {1} all-unknown -> UNKNOWN")
    @MethodSource("unknownFixtures")
    fun unknownVerdictsAreMarkedUnknown(name: String, options: VcgOptions) {
        val res = vcgFor(name, options)
        val solver = StubSolver(unknown(res.conditions.size))
        val status = res.check(solver)
        res.conditions.forEach { c ->
            assertEquals(VcgResult.Status.UNKNOWN, status[c.id], "condition ${c.id} must be UNKNOWN")
        }
        assertTrue(
            res.failedConditions(solver).isEmpty(),
            "unknown verdicts are neither proven nor failed"
        )
    }

    //region dedicated checking semantics

    @Test
    fun testCheckWithMixedAnswersMapsInOrder() {
        val res = vcgFor("countUp", u)
        val n = res.conditions.size
        assertTrue(n >= 3, "countUp should emit several conditions, got $n")
        // first condition sat (failed), middle unsat (proven), tail unknown
        val answers = mutableListOf<SExpr>()
        answers.add(SExprParser.parse("sat")!!)
        for (i in 1 until n - 1) answers.add(SExprParser.parse("unsat")!!)
        answers.add(SExprParser.parse("unknown")!!)
        val solver = StubSolver(answers)
        val status = res.check(solver)
        assertEquals(VcgResult.Status.FAILED, status[res.conditions[0].id])
        assertEquals(VcgResult.Status.PROVEN, status[res.conditions[n - 2].id])
        assertEquals(VcgResult.Status.UNKNOWN, status[res.conditions[n - 1].id])
        // failedConditions must return exactly the FAILED one
        assertEquals(listOf(res.conditions[0]), res.failedConditions(solver))
    }

    @Test
    fun testCheckStopsAtFirstAnswerBoundary() {
        val res = vcgFor("abs", u)
        val n = res.conditions.size
        // too few answers: everything after the first is UNKNOWN (peek runs out)
        val solver = StubSolver(listOf(SExprParser.parse("unsat")!!))
        val status = res.check(solver)
        assertEquals(VcgResult.Status.PROVEN, status[res.conditions[0].id])
        for (i in 1 until n) {
            assertEquals(VcgResult.Status.UNKNOWN, status[res.conditions[i].id], "condition ${i} beyond answers")
        }
    }

    @Test
    fun testCheckIgnoresTrailingAnswers() {
        val res = vcgFor("abs", u)
        val n = res.conditions.size
        val solver = StubSolver(unsat(n) + sat(2) + unknown(1))
        val status = res.check(solver)
        assertEquals(n, status.size)
        res.conditions.forEach { c -> assertEquals(VcgResult.Status.PROVEN, status[c.id]) }
    }

    @Test
    fun testEmptyAnswerYieldsUnknown() {
        val res = vcgFor("abs", u)
        val n = res.conditions.size
        val solver = StubSolver(emptyList())
        val status = res.check(solver)
        res.conditions.forEach { c -> assertEquals(VcgResult.Status.UNKNOWN, status[c.id]) }
    }

    @Test
    fun testMalformedErrorTokenYieldsUnknown() {
        // an answer whose error token cannot be consumed (no message element) makes
        // consumeErrors throw; runCheck must swallow it and report UNKNOWN everywhere
        val res = vcgFor("abs", u)
        val n = res.conditions.size
        val solver = StubSolver(List(n) { SExprParser.parse("(error)")!! })
        val status = res.check(solver)
        res.conditions.forEach { c -> assertEquals(VcgResult.Status.UNKNOWN, status[c.id]) }
    }

    @Test
    fun testCheckOnEmptyConditionList() {
        // a manually built result with no conditions: check() returns an empty map
        // and still touches the solver
        val solver = StubSolver(emptyList())
        val res = VcgResult(SmtQuery(), emptyList())
        val status = res.check(solver)
        assertTrue(status.isEmpty())
        assertTrue(res.failedConditions(solver).isEmpty())
        assertEquals(1, solver.invocations)
    }

    @Test
    fun testCheckMemoizesSingleInvocation() {
        val res = vcgFor("bodyAssert", u)
        val solver = StubSolver(unsat(res.conditions.size))
        val first = res.check(solver)
        val second = res.check(solver)
        val viaFailed = res.failedConditions(solver)
        assertEquals(first, second)
        assertEquals(1, solver.invocations, "three reads, one solver run")
        assertTrue(viaFailed.isEmpty())
    }

    @Test
    fun testCopiedResultHasIndependentCache() {
        val res = vcgFor("abs", u)
        val copy = res.copy()
        val unsatSolver = StubSolver(unsat(res.conditions.size))
        val satSolver = StubSolver(sat(res.conditions.size))
        val proven = copy.check(unsatSolver)
        val failed = res.check(satSolver)
        copy.conditions.forEach { c -> assertEquals(VcgResult.Status.PROVEN, proven[c.id]) }
        res.conditions.forEach { c -> assertEquals(VcgResult.Status.FAILED, failed[c.id]) }
        assertEquals(1, unsatSolver.invocations)
        assertEquals(1, satSolver.invocations)
    }

    @Test
    fun testFreshResultChecksAgain() {
        val res = vcgFor("abs", u)
        val solver = StubSolver(unsat(res.conditions.size))
        res.check(solver)
        // the doc contract: a fresh (copied-before-check) result re-invokes the solver
        val fresh = VcgResult(res.query, res.conditions)
        val afterCopy = fresh.check(solver)
        assertEquals(2, solver.invocations)
        assertTrue(allStatuses(fresh, VcgResult.Status.PROVEN))
        afterCopy.keys.forEach { assertTrue(afterCopy[it] == VcgResult.Status.PROVEN) }
    }

    @Test
    fun testStatusEnum() {
        assertEquals(listOf("PROVEN", "FAILED", "UNKNOWN"), VcgResult.Status.values().map { it.name })
        assertEquals(
            VcgResult.Status.PROVEN,
            VcgResult.Status.valueOf("PROVEN"),
        )
        assertEquals(
            VcgResult.Status.FAILED,
            VcgResult.Status.valueOf("FAILED"),
        )
        assertEquals(
            VcgResult.Status.UNKNOWN,
            VcgResult.Status.valueOf("UNKNOWN"),
        )
    }

    @Test
    fun testVerificationConditionDataClass() {
        val obligation = SExprParser.parse("(and true true)")!!
        val vc = VerificationCondition("vc12", "postcondition", obligation)
        assertNull(vc.range, "range defaults to null")
        assertEquals("vc12", vc.id)
        assertEquals("postcondition", vc.description)
        assertEquals(obligation, vc.obligation)
        // component accessors
        assertEquals("vc12", vc.component1())
        assertEquals("postcondition", vc.component2())
        assertEquals(obligation, vc.component3())
        assertNull(vc.component4())
        // copy with a range set
        val r = Range.range(1, 1, 1, 1)
        val ranged = vc.copy(range = r)
        assertEquals(r, ranged.range)
        assertEquals(vc.id, ranged.id)
        // equality / hash
        val same = VerificationCondition("vc12", "postcondition", obligation)
        assertEquals(vc, same)
        assertEquals(vc.hashCode(), same.hashCode())
        assertNotEquals(vc, ranged)
        assertNotEquals(vc, VerificationCondition("other", "postcondition", obligation))
        assertTrue(vc.toString().contains("vc12"), "toString should include the id")
    }

    @Test
    fun testVerificationConditionDestructuringAndRange() {
        val r = Range.range(3, 1, 3, 20)
        val vc = VerificationCondition("vc_42", "assert", SExprParser.parse("true")!!, range = r)
        val (id, description, _, range) = vc
        assertEquals("vc_42", id)
        assertEquals("assert", description)
        assertEquals(r, range)
        assertTrue(vc.toString().contains("vc_42"))
        assertTrue(vc.toString().contains("assert"))
    }

    @Test
    fun testToStringRendersQuery() {
        val res = vcgFor("abs", u)
        val s = res.toString()
        assertTrue(s.isNotEmpty())
        assertTrue(res.conditions.isNotEmpty())
        // the query serialization should mention the solver constructs
        assertTrue(s.contains("check-sat") || s.contains("declare"), "unexpected query: $s")
    }

    @Test
    fun testEmptyVcgResultToString() {
        val res = VcgResult(SmtQuery(), emptyList())
        // the query serializes the (empty) command stream; the result just forwards it
        assertEquals(res.query.toString(), res.toString())
    }

    //region checkProgressive

    @Test
    fun testCheckProgressiveStreamsLiveAndFinalReports() {
        val res = vcgFor("countUp", u)
        val n = res.conditions.size
        val solver = StubSolver(unsat(n))
        val progress = mutableListOf<Triple<Int, Int, VcgResult.Status?>>()
        val statuses = res.checkProgressive(
            onCondition = { index, total, status -> progress.add(Triple(index, total, status)) },
            solver = solver,
        )
        assertEquals(2 * n, progress.size, "live + final report per condition")
        assertEquals(1, solver.invocations)
        assertTrue(progress.take(n).all { it.third == null }, "live reports carry no status")
        assertTrue(progress.all { it.second == n }, "totals always equal condition count")
        statuses.values.forEach { assertEquals(VcgResult.Status.PROVEN, it) }
        val finals = progress.drop(n)
        assertTrue(finals.all { it.third == VcgResult.Status.PROVEN }, "final reports carry the verdict")
    }

    @Test
    fun testCheckProgressiveWithDefaultsUsesDefaultSolverAndCallbacks() {
        // omit solver/isCancelled/timeoutMillis -> the default-argument bridges run
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 not installed")
        val res = vcgFor("abs", u)
        val total = res.conditions.size
        var live = 0
        var finals = 0
        val statuses = res.checkProgressive(
            onCondition = { _, _, status -> if (status == null) live++ else finals++ },
        )
        assertEquals(total, live)
        assertEquals(total, finals)
        assertEquals(res.conditions.size, statuses.size)
    }

    @Test
    fun testCheckProgressiveCancelledRunIsNotMemoized() {
        val res = vcgFor("bodyAssert", u)
        val n = res.conditions.size
        assertTrue(n >= 2)
        // a cancelled run returns only a prefix of answers; the rest stay UNKNOWN
        val prefix = StubSolver(unsat(1))
        val statuses = res.checkProgressive(
            onCondition = { _, _, _ -> },
            solver = prefix,
            isCancelled = { true },
        )
        assertEquals(VcgResult.Status.PROVEN, statuses[res.conditions[0].id])
        for (i in 1 until n) {
            assertEquals(VcgResult.Status.UNKNOWN, statuses[res.conditions[i].id], "unanswered condition $i")
        }
        // the cancelled outcome is NOT memoized: a later check() runs the solver again
        val full = StubSolver(unsat(n))
        val after = res.check(full)
        assertEquals(VcgResult.Status.PROVEN, after[res.conditions.last().id])
        assertEquals(2, full.invocations + prefix.invocations, "cancelled run + fresh check")
    }

    @Test
    fun testCheckProgressiveWithTimeout() {
        val res = vcgFor("bodyAssert", u)
        val n = res.conditions.size
        val solver = StubSolver(sat(n))
        val statuses = res.checkProgressive(
            onCondition = { _, _, _ -> },
            solver = solver,
            timeoutMillis = 5000,
        )
        statuses.values.forEach { assertEquals(VcgResult.Status.FAILED, it) }
        assertTrue(res.failedConditions(solver).isNotEmpty())
    }

    @Test
    fun testCheckProgressiveFailedVerdicts() {
        val res = vcgFor("miscExprs", u)
        val n = res.conditions.size
        val solver = StubSolver(sat(n))
        val statuses = res.checkProgressive(
            onCondition = { _, _, _ -> },
            solver = solver,
        )
        statuses.values.forEach { assertEquals(VcgResult.Status.FAILED, it) }
    }

    //region end-to-end against the real solver

    @Test
    fun testProvenFixtureEndToEndViaCheck() {
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 not installed")
        val res = vcgFor("abs", u)
        val statuses = res.check()
        res.conditions.forEach { c ->
            assertEquals(VcgResult.Status.PROVEN, statuses[c.id], "abs must be fully proven")
        }
        assertTrue(res.failedConditions().isEmpty())
    }

    @Test
    fun testFalsifiableFixtureEndToEndViaCheck() {
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 not installed")
        val res = vcgFor("minUnderflow", u)
        val statuses = res.check()
        assertTrue(statuses.values.any { it == VcgResult.Status.FAILED }, "minUnderflow spec is wrong: $statuses")
        assertTrue(res.failedConditions().isNotEmpty())
    }

    @Test
    fun testProvenFixtureEndToEndProgressive() {
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 not installed")
        val res = vcgFor("countUp", u)
        val statuses = res.checkProgressive(onCondition = { _, _, _ -> })
        statuses.values.forEach { assertEquals(VcgResult.Status.PROVEN, it) }
    }

    @Test
    fun testConstructorResultEndToEnd() {
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 not installed")
        val res = Vcg(VcgContext.of(ctor()), u).verify()
        assertEquals(1, res.conditions.size)
        assertEquals(VcgResult.Status.PROVEN, res.check().values.single())
    }

    //region sanity: result structure

    @Test
    fun testConditionsLinkedToIds() {
        val res = vcgFor("bodyAssert", u)
        res.conditions.forEachIndexed { i, c ->
            assertEquals("vc${i + 1}", c.id)
            assertTrue(c.description.isNotEmpty())
        }
    }

    @Test
    fun testDescriptionsDistinguishObligations() {
        val res = vcgFor("boundedStore", b.copy(checkIndex = true))
        val descriptions = res.conditions.map { it.description }.toSet()
        assertTrue(descriptions.size > 1, "expected several distinct obligation kinds, got $descriptions")
        assertTrue(descriptions.any { it.contains("array store bound") })
        assertTrue(descriptions.any { it.contains("postcondition") })
    }

    @Test
    fun testGeneratedConditionsCarryRangeForBodyObligations() {
        val res = vcgFor("bodyAssert", u)
        val a = res.conditions.firstOrNull { it.description.contains("assert") }
        assertTrue(a != null)
        assertTrue(a!!.range != null, "body obligations should carry a source range")
    }
    //endregion
}

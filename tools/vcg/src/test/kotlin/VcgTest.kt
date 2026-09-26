/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import io.github.jmltoolkit.smt.Z3
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.solver.AppendableTo
import io.github.jmltoolkit.smt.solver.SExprParser
import io.github.jmltoolkit.smt.solver.Solver
import io.github.jmltoolkit.smt.solver.SolverAnswer
import io.github.jmltoolkit.utils.JMLUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.nio.file.Path

/**
 * Tests for the verification condition generator. If Z3 is installed, the generated
 * queries are checked; otherwise only the generation (query well-formedness) is tested.
 */
@Timeout(120)
class VcgTest {
    private val parser: JavaParser
    private val cu by lazy {
        val r = parser.parse(
            javaClass.getResourceAsStream("/VcgExamples.java")!!
        )
        Assertions.assertTrue(r.isSuccessful, r.problems.toString())
        r.result.get()
    }

    init {
        val config = ParserConfiguration()
        config.setProcessJml(true)
        config.setSymbolResolver(
            JavaSymbolSolver(
                com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(
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

    private fun vcgFor(name: String, options: VcgOptions = VcgOptions()): VcgResult {
        val m = method(name)
        val res = Vcg(VcgContext.of(m), options).verify()
        Assertions.assertTrue(res.conditions.isNotEmpty(), "no VCs generated for $name")
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
        Assertions.assertTrue(
            errors.isEmpty(),
            "solver errors: $errors\n${result.query}"
        )
        for (vc in result.conditions) {
            Assertions.assertTrue(
                answer.isSymbol("unsat"),
                "VC ${vc.id} (${vc.description}) not proven\n${result.query}"
            )
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
                Assertions.fail<Any>("solver answer not parseable: $answer\n${result.query}")
            }
            answer.consume()
        }
        Assertions.assertTrue(sat > 0, "expected at least one falsifiable VC\n${result.query}")
    }

    @Test
    fun testAbsUnbounded() {
        expectAllProven(
            vcgFor(
                "abs",
                VcgOptions(mode = VerificationMode.UNBOUNDED, defaultCallStrategy = CallStrategy.CONTRACT)
            )
        )
    }

    @Test
    fun testBuggyDetected() {
        expectSomeFalsifiable(
            vcgFor("buggy", VcgOptions(mode = VerificationMode.UNBOUNDED))
        )
    }

    @Test
    fun testBoundedLoopUnrolled() {
        expectAllProven(
            vcgFor(
                "sumBounded",
                VcgOptions(mode = VerificationMode.BOUNDED, defaultUnrollDepth = 6)
            )
        )
    }

    @Test
    fun testInvariantLoop() {
        expectAllProven(
            vcgFor("sumInvariant", VcgOptions(mode = VerificationMode.UNBOUNDED))
        )
    }

    @Test
    fun testLoopContractWithBreak() {
        expectAllProven(
            vcgFor("loopContractBreak", VcgOptions(mode = VerificationMode.UNBOUNDED))
        )
    }

    @Test
    fun testMethodContract() {
        expectAllProven(
            vcgFor(
                "useInc",
                VcgOptions(mode = VerificationMode.UNBOUNDED, defaultCallStrategy = CallStrategy.CONTRACT)
            )
        )
    }

    @Test
    fun testQuantifiedRequiresAndArrays() {
        expectAllProven(
            vcgFor("sumArray", VcgOptions(mode = VerificationMode.UNBOUNDED))
        )
    }

    @Test
    fun testMethodInline() {
        expectAllProven(
            vcgFor(
                "useIncInline",
                VcgOptions(mode = VerificationMode.UNBOUNDED, defaultCallStrategy = CallStrategy.INLINE)
            )
        )
    }

    @Test
    fun testFieldAssignmentsWithInvariant() {
        expectAllProven(
            vcgFor("countUp", VcgOptions(mode = VerificationMode.UNBOUNDED))
        )
    }

    @Test
    fun testTryCatch() {
        expectAllProven(
            vcgFor("divideByItselfWithTry", VcgOptions(mode = VerificationMode.UNBOUNDED))
        )
    }

    @Test
    fun testArrayStoreWithBoundsCheck() {
        expectAllProven(
            vcgFor(
                "storeFirst",
                VcgOptions(mode = VerificationMode.UNBOUNDED, checkIndex = true)
            )
        )
    }

    @Test
    fun testReportingApi() {
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 not installed")
        // the buggy method should surface a FAILED postcondition via the reporting API
        val m = method("buggy")
        val res = Vcg(VcgContext.of(m), VcgOptions(mode = VerificationMode.UNBOUNDED)).verify()
        val status = res.check()
        Assertions.assertTrue(
            status.values.any { it == VcgResult.Status.FAILED },
            "expected at least one failed VC, got $status"
        )
        // the (single) postcondition VC should carry a source range anchor
        val post = res.conditions.firstOrNull { it.description.contains("postcondition") }
        Assertions.assertNotNull(post, "no postcondition VC generated")
        Assertions.assertNotNull(post!!.range, "postcondition VC should carry a source range")
    }

    @Test
    fun testArbitraryReceiverField() {
        expectAllProven(
            vcgFor("setBox", VcgOptions(mode = VerificationMode.UNBOUNDED))
        )
    }

    @Test
    fun testBinarySearchUnrolled() {
        expectAllProven(
            vcgFor(
                "binarySearch",
                VcgOptions(mode = VerificationMode.UNBOUNDED, defaultUnrollDepth = 4)
            )
        )
    }

    @Test
    fun testSumAndMaxWithInvariant() {
        expectAllProven(
            vcgFor("sumAndMax", VcgOptions(mode = VerificationMode.UNBOUNDED))
        )
    }

    /** A solver stub that never touches z3; counts invocations and emits canned verdicts. */
    private class StubSolver(private val verdicts: List<SExpr>) : Solver() {
        var invocations = 0
        override fun run(
            query: AppendableTo,
            onForm: ((SExpr) -> Unit)?,
            isCancelled: () -> Boolean,
            timeoutMillis: Long,
        ): SolverAnswer {
            invocations++
            verdicts.forEach { onForm?.invoke(it) }
            return SolverAnswer(verdicts)
        }
    }

    @Test
    fun testCheckIsMemoized() {
        val result = vcgFor("countUp", VcgOptions(mode = VerificationMode.UNBOUNDED))
        val solver = StubSolver(listOf(SExprParser.parse("unsat")!!))
        val status = result.check(solver)
        val again = result.check(solver)
        val failed = result.failedConditions(solver)
        Assertions.assertEquals(
            1, solver.invocations,
            "check()/failedConditions() must run the solver at most once"
        )
        Assertions.assertEquals(status, again)
        Assertions.assertEquals(0, failed.size, "all conditions were answered unsat")
    }

    @Test
    fun testCheckProgressiveStreamsPerConditionProgress() {
        val result = vcgFor("countUp", VcgOptions(mode = VerificationMode.UNBOUNDED))
        val n = result.conditions.size
        Assertions.assertTrue(n > 0)
        val solver = StubSolver(List(n) { SExprParser.parse("sat")!! })
        val progress = mutableListOf<Triple<Int, Int, VcgResult.Status?>>()
        val statuses = result.checkProgressive(
            solver = solver,
            onCondition = { index, total, status -> progress.add(Triple(index, total, status)) },
        )
        // during the run each condition is reported with unknown status, afterwards with the verdict
        Assertions.assertEquals(2 * n, progress.size, "one live + one final report per condition")
        Assertions.assertEquals(1, solver.invocations, "solver ran exactly once")
        Assertions.assertTrue(progress.take(n).all { it.third == null }, "live reports have no status yet")
        Assertions.assertTrue(progress.all { it.second == n }, "totals must equal the condition count")
        Assertions.assertEquals(VcgResult.Status.FAILED, progress.last().third, "final report carries the verdict")
        Assertions.assertTrue(statuses.values.all { it == VcgResult.Status.FAILED }, "sat answers are FAILED")
    }

    @Test
    fun testInlineCallPropagatesSideEffects() {
        expectAllProven(
            vcgFor(
                "callIncCounter",
                VcgOptions(mode = VerificationMode.UNBOUNDED, defaultCallStrategy = CallStrategy.INLINE)
            )
        )
    }

    @Test
    fun testContractPostconditionSeesHavocedState() {
        expectAllProven(
            vcgFor(
                "callBumpFieldTo42",
                VcgOptions(mode = VerificationMode.UNBOUNDED, defaultCallStrategy = CallStrategy.CONTRACT)
            )
        )
    }

    @Test
    fun testContractPostconditionNotAgainstPreCallValue() {
        // bumpField guarantees fieldV == 7 in the havoced state; asserting fieldV == 5
        // afterwards must stay falsifiable, not be silently provided by the old
        // pre-call snapshot of the postcondition
        expectSomeFalsifiable(
            vcgFor(
                "callBumpFieldThenCheck",
                VcgOptions(mode = VerificationMode.UNBOUNDED, defaultCallStrategy = CallStrategy.CONTRACT)
            )
        )
    }

    @Test
    fun testContractCallBindsReceiverFields() {
        expectAllProven(
            vcgFor(
                "callBumpBoxValue",
                VcgOptions(mode = VerificationMode.UNBOUNDED, defaultCallStrategy = CallStrategy.CONTRACT)
            )
        )
    }

    @Test
    fun testInlineCallPropagatesReceiverFieldWrite() {
        expectAllProven(
            vcgFor(
                "callBumpBoxValueInline",
                VcgOptions(mode = VerificationMode.UNBOUNDED, defaultCallStrategy = CallStrategy.INLINE)
            )
        )
    }

    @Test
    fun testArrayStoreOutOfBoundsDetected() {
        expectSomeFalsifiable(
            vcgFor(
                "storeUnchecked",
                VcgOptions(mode = VerificationMode.UNBOUNDED, checkIndex = true)
            )
        )
    }

    @Test
    fun testDivisionByZeroDetected() {
        expectSomeFalsifiable(
            vcgFor(
                "divUnsafe",
                VcgOptions(mode = VerificationMode.UNBOUNDED, checkDivision = true)
            )
        )
    }

    @Test
    fun testOldValueDiscrepancyDetected() {
        expectSomeFalsifiable(
            vcgFor("bumpButClaimUnchanged", VcgOptions(mode = VerificationMode.UNBOUNDED))
        )
    }

    @Test
    fun testLoopInvariantNotMaintainedDetected() {
        expectSomeFalsifiable(
            vcgFor("unmaintainedInvariant", VcgOptions(mode = VerificationMode.UNBOUNDED))
        )
    }

    @Test
    fun testQuantifiedPostconditionViolatedDetected() {
        expectSomeFalsifiable(
            vcgFor("buggyQuantifiedPost", VcgOptions(mode = VerificationMode.UNBOUNDED))
        )
    }
}

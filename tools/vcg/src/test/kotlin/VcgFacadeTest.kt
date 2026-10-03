/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import io.github.jmltoolkit.smt.Z3
import io.github.jmltoolkit.smt.SmtQuery
import io.github.jmltoolkit.smt.SmtTermFactory
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.model.SList
import io.github.jmltoolkit.smt.model.SmtType
import io.github.jmltoolkit.smt.solver.Solver
import io.github.jmltoolkit.smt.solver.SolverAnswer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.nio.file.Files
import java.nio.file.Path

/**
 * Tests for the [VcgFacade]: selection of verification targets, parallel
 * generation/checking, sync/async solving and the content-based [VcgCache].
 */
@Timeout(180)
class VcgFacadeTest {
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

    @Test
    fun targetsSelectsOnlyAnnotatedCallablesWithBody() {
        val r = parser.parse(
            """
            abstract class C {
                //@ requires true;
                //@ ensures true;
                public int m(int x) { return x; }

                //@ requires true;
                //@ ensures true;
                public abstract int n(int x);

                public int p(int x) { return x; }
            }
            """.trimIndent()
        )
        assertTrue(r.isSuccessful, r.problems.toString())
        val targets = VcgFacade().targets(r.result.get())
        assertEquals(listOf("m"), targets.map { it.nameAsString }, "only contract-carrying callables with a body")
    }

    @Test
    fun verifyAllIsDeterministicAndOrdered() {
        val facade = VcgFacade()
        val callables = facade.targets(cu).filter { it.nameAsString == "setThisField" }
        val results = facade.verifyAll(callables)
        assertEquals(callables.size, results.size)
        val sequential = callables.map { facade.verify(it) }
        results.zip(sequential).forEach { (r1, r2) ->
            assertEquals(VcgFacade.contentKey(r1), VcgFacade.contentKey(r2), "parallel vs sequential generation")
            assertEquals(r1.conditions.map { it.id }, r2.conditions.map { it.id })
        }
    }

    @Test
    fun asyncCheckReturnsSameStatusesAsSyncCheck() {
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 not installed")
        val facade = VcgFacade(options = VcgOptions(mode = VerificationMode.UNBOUNDED))
        val result = facade.verify(method("setThisField"))
        val sync = facade.check(result)
        val async = facade.checkAsync(result).get()
        assertEquals(sync, async)
        assertTrue(sync.values.all { it == VcgResult.Status.PROVEN }, sync.toString())
    }

    @Test
    fun identicalContentIsCheckedOnceAndCachedInMemory() {
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 not installed")
        val counting = CountingSolver()
        val facade = VcgFacade(
            options = VcgOptions(mode = VerificationMode.UNBOUNDED),
            solver = { counting },
            cache = InMemoryVcgCache(),
        )
        val callable = method("setThisField")
        val first = facade.check(facade.verify(callable))
        assertEquals(1, counting.runs, "first check must hit the solver")
        assertEquals(first, facade.check(facade.verify(callable)), "second check must reuse the cached verdict")
        assertEquals(1, counting.runs, "identical content must not hit the solver again")
    }

    @Test
    fun fileCacheRoundTripsAcrossFacades() {
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 not installed")
        val cacheFile = Files.createTempFile("vcg-facade-test", ".properties")
        val counting = CountingSolver()
        val options = VcgOptions(mode = VerificationMode.UNBOUNDED)
        val callable = method("setThisField")

        val fileCache = FileVcgCache(cacheFile)
        VcgFacade(options = options, solver = { counting }, cache = fileCache)
            .check(VcgFacade(options = options).verify(callable))
        fileCache.save()
        assertEquals(1, counting.runs)

        val reloaded = FileVcgCache(cacheFile).also { it.load() }
        assertEquals(1, reloaded.size, "saved cache must contain one entry")
        assertTrue(Files.size(cacheFile) > 0, "cache file must be written")

        val second = VcgFacade(options = options, solver = { counting }, cache = reloaded)
            .check(VcgFacade(options = options).verify(callable))
        assertEquals(1, counting.runs, "reloaded cache must answer without a solver run")
        assertTrue(second.values.all { it == VcgResult.Status.PROVEN })
    }

    @Test
    fun cacheMissWhenContentDiffers() {
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 not installed")
        val counting = CountingSolver()
        val facade = VcgFacade(
            options = VcgOptions(mode = VerificationMode.UNBOUNDED),
            solver = { counting },
            cache = InMemoryVcgCache(),
        )
        facade.check(facade.verify(method("setThisField")))
        facade.check(facade.verify(method("breakAndContinue")))
        assertEquals(2, counting.runs, "different content must run the solver anew")
    }

    @Test
    fun normalizeStripsCommentsWhitespaceAndNames() {
        val messy = "(assert\n  (! (not true) :named my_condition_42)) ; comment\n  "
        val clean = "(assert (! (not true) :named v1))"
        assertEquals(VcgFacade.normalize(clean), VcgFacade.normalize(messy))
        assertTrue(!VcgFacade.normalize(messy).contains('\n'))
        assertTrue(!VcgFacade.normalize(messy).contains('\t'))
        assertTrue(!VcgFacade.normalize(messy).contains("  "), "doubled spaces must collapse")
        assertTrue(!VcgFacade.normalize(messy).contains(';'), "comments must vanish")
    }

    @Test
    fun contentKeyIgnoresConditionNames() {
        assertEquals(
            VcgFacade.contentKey(vcResultOf("a_very_specific_name")),
            VcgFacade.contentKey(vcResultOf("v1"))
        )
        assertTrue(
            VcgFacade.contentKey(vcResultOf("x", oblig = "false")) != VcgFacade.contentKey(vcResultOf("x")),
            "different obligations must differ"
        )
    }

    @Test
    fun inMemoryCacheMissReturnsNull() {
        assertNull(InMemoryVcgCache().get("nope"))
        val file = FileVcgCache(Files.createTempFile("vcg-facade-test", ".properties"))
        assertNull(file.get("nope"))
    }

    private fun vcResultOf(named: String, oblig: String = "true"): VcgResult {
        // build the `(assert (! (not OB) :named NAME))` command without SExprParser,
        // which rejects the `!` form
        val query = SmtQuery()
        query.addCommand(
            SList(
                SmtType.COMMAND, null,
                listOf(
                    SmtTermFactory.symbol("!"),
                    SmtTermFactory.not(SmtTermFactory.makeBoolean(oblig == "true")),
                    SmtTermFactory.symbol(":named"),
                    SmtTermFactory.symbol(named),
                )
            )
        )
        return VcgResult(query, emptyList())
    }

    private class CountingSolver : Solver() {
        var runs = 0
            private set

        override fun run(
            query: io.github.jmltoolkit.smt.solver.AppendableTo,
            onForm: ((SExpr) -> Unit)?,
            isCancelled: () -> Boolean,
            timeoutMillis: Long,
        ): SolverAnswer {
            runs++
            return super.run(query, onForm, isCancelled, timeoutMillis)
        }
    }
}

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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Golden snapshots of the generated SMT queries (doc/vcg-semantics-plan.md, Phase 0).
 *
 * Every curated case is run through the full generation pipeline and its complete
 * `SmtQuery` is compared against a committed golden file under
 * `src/test/resources/golden`. A drift fails the test and shows the first divergent
 * line, so a refactor can prove "only the intended obligations changed".
 *
 * Set `UPDATE_GOLDEN=1` to (re)write the snapshots from the current generator
 * output — only the intended golden update, reviewed in the diff.
 */
class VcgGoldenQueriesTest {
    private val parser: JavaParser
    private val cu by lazy {
        val r = parser.parse(javaClass.getResourceAsStream("/VcgExamples.java")!!)
        check(r.isSuccessful) { r.problems.toString() }
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

    private fun method(name: String): MethodDeclaration =
        cu.types[0].asClassOrInterfaceDeclaration().methods.first { it.nameAsString == name }

    /** Curated representative subset: name, options, golden-file tag. */
    private fun cases(): List<Triple<String, String, String>> =
        listOf(
            // arithmetic + contracts, both modes
            arrayOf("abs", "U", "UNBOUNDED"),
            arrayOf("abs", "B", "BOUNDED"),
            arrayOf("miscExprs", "U", "UNBOUNDED"),
            arrayOf("miscExprs", "B", "BOUNDED"),
            arrayOf("boundedIncrement", "U", "UNBOUNDED"),
            arrayOf("boundedIncrement", "B", "BOUNDED"),
            // runtime checks + bit-vector arithmetic
            arrayOf("boundedAdd", "B_OVF_DIV", "BOUNDED_overflow_division"),
            arrayOf("boundedStore", "B_IDX", "BOUNDED_index"),
            // fields through this / receivers
            arrayOf("setThisField", "U", "UNBOUNDED"),
            arrayOf("setThisField", "B", "BOUNDED"),
            arrayOf("readCounter", "U", "UNBOUNDED"),
            // object allocation
            arrayOf("newObject", "U", "UNBOUNDED"),
            arrayOf("newObject", "B", "BOUNDED"),
            // exceptions
            arrayOf("tryFinallyOnly", "U", "UNBOUNDED"),
            arrayOf("tryFinallyOnly", "B", "BOUNDED"),
            // loops: unrolling, invariant, break/continue
            arrayOf("linearCount", "U", "UNBOUNDED"),
            arrayOf("linearCount", "B", "BOUNDED"),
            arrayOf("sumInvariant", "U_INV", "UNBOUNDED_invariant"),
            arrayOf("loopNoReturn", "U_INV", "UNBOUNDED_invariant"),
            arrayOf("binarySearchInv", "U_INV", "UNBOUNDED_invariant"),
            arrayOf("binarySearch", "U_UNROLL4", "UNBOUNDED_unroll4"),
            arrayOf("binarySearch", "B_UNROLL4", "BOUNDED_unroll4"),
            arrayOf("breakAndContinue", "U", "UNBOUNDED"),
            // calls: contract and inlined
            arrayOf("callAssignableField", "U_CONTRACT", "UNBOUNDED_contract"),
            arrayOf("callInlineMixedArgs", "U_INLINE", "UNBOUNDED_inline"),
            arrayOf("callBumpEverything", "U_CONTRACT", "UNBOUNDED_contract"),
            // multiple spec cases (P1.1): disjunctive requires/ensures semantics
            arrayOf("twoCaseAbs", "U", "UNBOUNDED"),
            arrayOf("twoCaseAbs", "B", "BOUNDED"),
            arrayOf("callTwoCaseAbs", "U_CONTRACT", "UNBOUNDED_contract"),
            arrayOf("unreachableGap", "U", "UNBOUNDED"),
        ).map { Triple(it[0], it[1], it[2]) }

    private fun options(tag: String): VcgOptions = when (tag) {
        "U" -> VcgOptions(mode = VerificationMode.UNBOUNDED)

        "B" -> VcgOptions(mode = VerificationMode.BOUNDED)

        "B_OVF_DIV" ->
            VcgOptions(mode = VerificationMode.BOUNDED, checkOverflow = true, checkDivision = true)

        "B_IDX" -> VcgOptions(mode = VerificationMode.BOUNDED, checkIndex = true)

        "U_INV" -> VcgOptions(mode = VerificationMode.UNBOUNDED, defaultLoopStrategy = LoopStrategy.INVARIANT)

        "U_CONTRACT" -> VcgOptions(mode = VerificationMode.UNBOUNDED, defaultCallStrategy = CallStrategy.CONTRACT)

        "U_INLINE" -> VcgOptions(mode = VerificationMode.UNBOUNDED, defaultCallStrategy = CallStrategy.INLINE)

        "U_UNROLL4" -> VcgOptions(mode = VerificationMode.UNBOUNDED, defaultUnrollDepth = 4)

        "B_UNROLL4" -> VcgOptions(mode = VerificationMode.BOUNDED, defaultUnrollDepth = 4)

        else -> error("no options for tag $tag")
    }

    @Test
    fun queriesMatchGoldenSnapshots() {
        val goldenDir = Path.of("src/test/resources/golden")
        val update = System.getenv("UPDATE_GOLDEN") == "1"
        var written = 0
        for ((name, tag, _) in cases()) {
            val res = Vcg(VcgContext.of(method(name)), options(tag)).verify()
            assertEquals(true, res.conditions.isNotEmpty(), "no VCs generated for $name ($tag)")
            val query = res.query.toString().trimEnd() + "\n"
            val file = goldenDir.resolve("${name}__$tag.smt2")
            if (update) {
                Files.createDirectories(goldenDir)
                Files.writeString(file, query, StandardCharsets.UTF_8)
                written++
            } else if (!Files.exists(file)) {
                throw AssertionError(
                    "no golden snapshot for $name ($tag): expected ${file}\n" +
                        "regenerate with: UPDATE_GOLDEN=1 ./gradlew :tools:vcg:test"
                )
            } else {
                val golden = Files.readString(file, StandardCharsets.UTF_8)
                val msg = firstDiffMessage(golden, query, file)
                assertEquals(golden, query, msg)
            }
        }
        if (update) {
            println("UPDATE_GOLDEN: wrote/updated $written golden snapshot(s) under $goldenDir")
        }
    }

    /** Compact failure message: first divergent line with context. */
    private fun firstDiffMessage(golden: String, actual: String, file: Path): String {
        val g = golden.lines().toList()
        val a = actual.lines().toList()
        val n = minOf(g.size, a.size)
        val i = (0 until n).firstOrNull { g[it] != a[it] }
        val gap = 3
        val from = ((i ?: n) - gap).coerceAtLeast(0)
        val to = ((i ?: n) + gap).coerceAtMost(n)
        val sb = StringBuilder()
        sb.append("golden query drifted: ").append(file).append('\n')
        val shown = (from until to).map { k ->
            val marker = if (k == i) ">>>" else "   "
            "$marker [${k + 1}] golden=${g[k]}  actual=${a[k]}"
        }
        if (i == null && g.size != a.size) {
            sb.append("   (line counts differ: golden=${g.size}, actual=${a.size})\n")
        }
        sb.append(shown.joinToString("\n"))
        return sb.toString()
    }
}

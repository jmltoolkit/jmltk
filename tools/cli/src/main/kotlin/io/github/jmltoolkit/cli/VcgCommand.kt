/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import io.github.jmltoolkit.vcg.CallStrategy
import io.github.jmltoolkit.vcg.FileVcgCache
import io.github.jmltoolkit.vcg.InMemoryVcgCache
import io.github.jmltoolkit.vcg.LoopStrategy
import io.github.jmltoolkit.vcg.VcgFacade
import io.github.jmltoolkit.vcg.VcgOptions
import io.github.jmltoolkit.vcg.VcgResult
import io.github.jmltoolkit.vcg.VerificationMode
import java.util.concurrent.ForkJoinPool
import java.util.regex.Pattern

/**
 * Verification condition generation for Java + JML.
 *
 * Parses the given source files, runs the verification condition generator over
 * every method/constructor that carries a JML contract, and prints per-condition
 * results (PROVEN / FAILED / UNKNOWN), keyed by the condition's unique id of the
 * form `<class>#<method>(<params>)#<kind>-<n>@<line>:<column>`.
 *
 * Generation and solving go through [VcgFacade]: each callable runs as an
 * independent task on the common [ForkJoinPool], and its conditions are printed
 * as soon as it is solved, so a slow callable does not delay the report of the
 * already-finished ones. With `--vcg-cache <file>` solver answers are reused
 * (keyed by the *content* sent to the SMT solver, not by condition names).
 * `--vcg-include`/`--vcg-exclude` filter the report by glob expressions on the
 * condition id.
 *
 * @author Alexander Weigl
 * @version 1 (26.09.26)
 */
class VcgCommand : FileBasedCommand(name = "vcg") {
    private val unbounded by option(
        "--unbounded", help = "use unbounded mathematical integers instead of bounded bit-vectors"
    ).flag()
    private val unroll by option(
        "--loop-unroll", help = "unroll loops this many times (instead of invariant-based reasoning)"
    ).int().default(10)
    private val invariant by option(
        "--loop-invariant", help = "handle loops via their loop invariants instead of unrolling"
    ).flag()
    private val checkDivision by option(
        "--check-division", help = "emit VCs asserting no division/modulo by zero"
    ).flag()
    private val checkOverflow by option(
        "--check-overflow", help = "emit VCs asserting no signed arithmetic overflow (BOUNDED mode)"
    ).flag()
    private val checkIndex by option(
        "--check-index", help = "emit VCs asserting array indices are within bounds"
    ).flag()
    private val checkNull by option(
        "--check-null", help = "emit VCs asserting no null-pointer dereference"
    ).flag()
    private val checkCast by option(
        "--check-cast", help = "emit VCs asserting reference casts are type-correct"
    ).flag()
    private val checkNegativeArraySize by option(
        "--check-negative-array-size", help = "emit VCs asserting no negative array sizes"
    ).flag()
    private val checkStringIndex by option(
        "--check-string-index", help = "emit VCs asserting string indices are in bounds"
    ).flag()
    private val checksPolicy by option(
        "--checks",
        help = "runtime-check policy: all | none | default (the six exception checks). " +
            "Individual --check-* flags enable a check on top of the policy."
    ).choice("all", "none", "default").default("default")
    private val allowUnrefinedCalls by option(
        "--allow-unrefined-calls",
        help = "havoc the caller-visible frame for calls whose callee has no JML contract " +
            "and no body, instead of rejecting them"
    ).flag()
    private val vcgInclude by option(
        "--vcg-include", help = "only report VCs whose id matches this glob (repeatable)"
    ).multiple()
    private val vcgExclude by option(
        "--vcg-exclude", help = "suppress VCs whose id matches this glob (repeatable)"
    ).multiple()
    private val cacheFile by option(
        "--vcg-cache", help = "persist and reuse solver answers in the given file"
    ).file()

    override fun help(context: Context): String =
        "Verification condition generation for Java + JML (SMT backend)."

    override fun run() {
        if (files.isEmpty()) {
            echo("no input files")
            return
        }
        val config = parserConfiguration()
        val cus = parse(files, config)
        val cache = cacheFile?.let {
            FileVcgCache(it.toPath()).also { c -> c.load() }
        } ?: InMemoryVcgCache()
        val facade = VcgFacade(
            options = vcgOptions(),
            pool = ForkJoinPool.commonPool(),
            cache = cache,
        )
        val includes = vcgInclude.map { glob(it) }
        val excludes = vcgExclude.map { glob(it) }
        fun included(id: String): Boolean =
            (includes.isEmpty() || includes.any { it.matcher(id).matches() }) &&
                excludes.none { it.matcher(id).matches() }
        val outputLock = Object()
        var total = 0
        var failed = 0
        // Verify and solve each callable in parallel and print its conditions as soon
        // as it is done; the per-callable block keeps the lines of one callable
        // contiguous and the guarded counters make the summary exact.
        facade.verifyAndCheckAll(cus) { outcome ->
            val lines = StringBuilder()
            var localTotal = 0
            var localFailed = 0
            for (vc in outcome.result.conditions) {
                if (!included(vc.id)) continue
                localTotal++
                val status = outcome.statuses[vc.id] ?: VcgResult.Status.UNKNOWN
                when (status) {
                    VcgResult.Status.PROVEN -> lines.append("  ok   ${vc.id}  ${vc.description}\n")

                    VcgResult.Status.FAILED -> {
                        lines.append("  FAIL ${vc.id}  ${vc.description}\n")
                        localFailed++
                    }

                    VcgResult.Status.UNKNOWN -> lines.append("  ???? ${vc.id}  ${vc.description}\n")
                }
            }
            if (lines.isNotEmpty()) {
                synchronized(outputLock) {
                    echo(lines.toString().removeSuffix("\n"))
                    total += localTotal
                    failed += localFailed
                }
            }
        }
        cache.save()
        echo("$total conditions, $failed failing")
    }

    private fun vcgOptions(): VcgOptions {
        val all = checksPolicy == "all"
        val none = checksPolicy == "none"
        // the VcgOptions defaults are the documented default policy: the six
        // runtime-exception checks on, overflow off (BOUNDED wrap-around already
        // matches Java; the overflow check only *flags* wraparounds)
        val base = VcgOptions(
            mode = if (unbounded) VerificationMode.UNBOUNDED else VerificationMode.BOUNDED,
            defaultLoopStrategy = if (invariant) LoopStrategy.INVARIANT else LoopStrategy.UNROLL,
            defaultUnrollDepth = unroll,
            defaultCallStrategy = CallStrategy.CONTRACT,
            allowUnrefinedCalls = allowUnrefinedCalls,
        )
        // `--checks=all|none` sets the whole set; `--checks=default` (and no
        // --checks) keeps the library defaults; individual --check-* flags only
        // ever enable a check on top of the chosen policy.
        fun pick(policyBase: Boolean, flag: Boolean): Boolean = when {
            all -> true
            none -> flag
            else -> policyBase || flag
        }
        return base.copy(
            checkOverflow = pick(base.checkOverflow, checkOverflow),
            checkDivision = pick(base.checkDivision, checkDivision),
            checkIndex = pick(base.checkIndex, checkIndex),
            checkNull = pick(base.checkNull, checkNull),
            checkCast = pick(base.checkCast, checkCast),
            checkNegativeArraySize = pick(base.checkNegativeArraySize, checkNegativeArraySize),
            checkStringIndex = pick(base.checkStringIndex, checkStringIndex),
        )
    }

    /** Converts a glob (`*` matches any run of characters, `?` any single one) to a regex. */
    private fun glob(pattern: String): Pattern {
        val sb = StringBuilder("^")
        for (c in pattern) {
            when (c) {
                '*' -> sb.append(".*")
                '?' -> sb.append('.')
                else -> sb.append(Pattern.quote(c.toString()))
            }
        }
        return Pattern.compile(sb.append('$').toString())
    }

    private fun parserConfiguration(): ParserConfiguration {
        val config = ParserConfiguration()
        config.setProcessJml(true)
        for (key in activeJmlKeys) config.jmlKeys.add(listOf(key))
        if (activeJmlKeys.isEmpty()) config.jmlKeys.add(listOf("key"))
        config.setSymbolResolver(JavaSymbolSolver(combinedTypeSolvers()))
        return config
    }

    private fun combinedTypeSolvers(): CombinedTypeSolver {
        val solvers = mutableListOf<com.github.javaparser.resolution.TypeSolver>()
        solvers.add(ReflectionTypeSolver())
        files.mapNotNull { it.absoluteFile.parentFile?.absolutePath }.distinct().forEach { parent ->
            val dir = java.io.File(parent)
            if (dir.isDirectory) {
                solvers.add(JavaParserTypeSolver(dir.toPath()))
            }
        }
        return CombinedTypeSolver(solvers)
    }
}

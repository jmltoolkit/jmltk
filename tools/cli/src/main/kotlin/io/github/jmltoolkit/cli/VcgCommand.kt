/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import io.github.jmltoolkit.vcg.CallStrategy
import io.github.jmltoolkit.vcg.LoopStrategy
import io.github.jmltoolkit.vcg.Vcg
import io.github.jmltoolkit.vcg.VcgContext
import io.github.jmltoolkit.vcg.VcgOptions
import io.github.jmltoolkit.vcg.VcgResult
import io.github.jmltoolkit.vcg.VerificationMode

/**
 * Verification condition generation for Java + JML.
 *
 * Parses the given source files, runs the verification condition generator over
 * every method/constructor that carries a JML contract, and prints per-condition
 * results (PROVEN / FAILED / UNKNOWN).
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
    private val checkIndex by option(
        "--check-index", help = "emit VCs asserting array indices are within bounds"
    ).flag()

    override fun help(context: Context): String =
        "Verification condition generation for Java + JML (SMT backend)."

    override fun run() {
        if (files.isEmpty()) {
            echo("no input files")
            return
        }
        val config = parserConfiguration()
        val cus = parse(files, config)
        var total = 0
        var failed = 0
        for (cu in cus) {
            for (callable in callablesWithBody(cu)) {
                if (callable.contracts.isEmpty()) continue
                val result = verify(callable)
                total += result.conditions.size
                for (vc in result.conditions) {
                    val status = result.check()[vc.id] ?: VcgResult.Status.UNKNOWN
                    val loc = vc.range?.let { "line ${it.begin.line}" } ?: "-"
                    when (status) {
                        VcgResult.Status.PROVEN -> echo("  ok   $loc  ${vc.description}")

                        VcgResult.Status.FAILED -> {
                            echo("  FAIL $loc  ${vc.description}")
                            failed++
                        }

                        VcgResult.Status.UNKNOWN -> echo("  ???? $loc  ${vc.description}")
                    }
                }
            }
        }
        echo("$total conditions, $failed failing")
    }

    private fun verify(callable: CallableDeclaration<*>): VcgResult {
        val loopStrategy = if (invariant) LoopStrategy.INVARIANT else LoopStrategy.UNROLL
        val options = VcgOptions(
            mode = if (unbounded) VerificationMode.UNBOUNDED else VerificationMode.BOUNDED,
            defaultLoopStrategy = loopStrategy,
            defaultUnrollDepth = unroll,
            defaultCallStrategy = CallStrategy.CONTRACT,
            checkDivision = checkDivision,
            checkIndex = checkIndex,
        )
        return Vcg(VcgContext.of(callable), options).verify()
    }

    private fun callablesWithBody(cu: CompilationUnit): List<CallableDeclaration<*>> =
        cu.findAll(CallableDeclaration::class.java)

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

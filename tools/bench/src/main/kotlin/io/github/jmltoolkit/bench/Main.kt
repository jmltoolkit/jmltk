/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.bench

import java.nio.file.Path
import java.util.Locale
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(runMain(args))
}

fun runMain(args: Array<String>): Int {
    val config =
        try {
            parseBenchConfig(args)
        } catch (e: IllegalArgumentException) {
            System.err.println(e.message)
            return 2
        }

    if (config.verify && config.baseline == null) {
        System.err.println("--verify requires --baseline <json>")
        return 2
    }
    if (config.baseline != null && config.json != null && samePath(config.baseline, config.json)) {
        System.err.println("--json must not be the same file as --baseline")
        return 2
    }

    val baseline =
        config.baseline?.let { path ->
            try {
                readResult(path)
            } catch (e: Exception) {
                System.err.println("Cannot read baseline $path: ${e.message}")
                return 2
            }
        }

    val result =
        try {
            runBenchmark(config)
        } catch (e: IllegalArgumentException) {
            System.err.println(e.message)
            return 2
        } catch (e: java.io.IOException) {
            System.err.println(e.message)
            return 2
        }

    printResult(result)

    if (baseline != null) {
        printComparison(baseline, result)
        if (config.verify) {
            val differences = verifyParity(baseline, result)
            if (differences.isNotEmpty()) {
                differences.forEach { System.err.println(it) }
                return 1
            }
            println("verify: ok/fail set matches baseline")
        }
    }

    config.json?.let { writeResult(result, it) }
    return 0
}

private fun printResult(result: BenchResult) {
    println("${result.corpus}/${result.mode}: ${result.files} files, ${result.bytes} bytes, ${result.ok} ok, ${result.fail} fail")
    result.rounds.forEachIndexed { index, round ->
        println("round ${index + 1}: ${format(round.millis)} ms, ${format(round.megabytesPerSecond)} MiB/s")
    }
    val medianMbps = (result.bytes / 1_048_576.0) / (result.medianMillis / 1000.0)
    val bestMbps = (result.bytes / 1_048_576.0) / (result.bestMillis / 1000.0)
    println("median: ${format(result.medianMillis)} ms, ${format(medianMbps)} MiB/s")
    println("best:   ${format(result.bestMillis)} ms, ${format(bestMbps)} MiB/s")
}

private fun printComparison(
    baseline: BenchResult,
    result: BenchResult,
) {
    println(
        "baseline best: ${format(baseline.bestMillis)} ms -> ${format(result.bestMillis)} ms " +
            "(${format(percentDelta(baseline.bestMillis, result.bestMillis))}%)",
    )
    println(
        "baseline median: ${format(baseline.medianMillis)} ms -> ${format(result.medianMillis)} ms " +
            "(${format(percentDelta(baseline.medianMillis, result.medianMillis))}%)",
    )
}

private fun samePath(
    first: Path,
    second: Path,
): Boolean = first.toAbsolutePath().normalize() == second.toAbsolutePath().normalize()

private fun format(value: Double): String = String.format(Locale.ROOT, "%.3f", value)

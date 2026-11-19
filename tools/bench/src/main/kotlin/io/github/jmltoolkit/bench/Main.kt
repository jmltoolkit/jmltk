/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.bench

import java.util.Locale
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val config =
        try {
            parseBenchConfig(args)
        } catch (e: IllegalArgumentException) {
            System.err.println(e.message)
            exitProcess(2)
        }

    if (config.verify && config.baseline == null) {
        System.err.println("--verify requires --baseline <json>")
        exitProcess(2)
    }

    val result = runBenchmark(config)
    println("${result.corpus}/${result.mode}: ${result.files} files, ${result.bytes} bytes, ${result.ok} ok, ${result.fail} fail")
    result.rounds.forEachIndexed { index, round ->
        println("round ${index + 1}: ${format(round.millis)} ms, ${format(round.megabytesPerSecond)} MB/s")
    }
    val bytes = result.bytes
    val medianMbps = (bytes / 1_048_576.0) / (result.medianMillis / 1000.0)
    val bestMbps = (bytes / 1_048_576.0) / (result.bestMillis / 1000.0)
    println("median: ${format(result.medianMillis)} ms, ${format(medianMbps)} MB/s")
    println("best:   ${format(result.bestMillis)} ms, ${format(bestMbps)} MB/s")

    config.json?.let { writeResult(result, it) }

    config.baseline?.let { baselinePath ->
        val baseline = readResult(baselinePath)
        println(
            "baseline best: ${format(baseline.bestMillis)} ms -> ${format(result.bestMillis)} ms " +
                "(${format(percentDelta(baseline.bestMillis, result.bestMillis))}%)",
        )
        println(
            "baseline median: ${format(baseline.medianMillis)} ms -> ${format(result.medianMillis)} ms " +
                "(${format(percentDelta(baseline.medianMillis, result.medianMillis))}%)",
        )
        if (config.verify) {
            val differences = verifyParity(baseline, result)
            if (differences.isNotEmpty()) {
                differences.forEach { System.err.println(it) }
                exitProcess(1)
            }
            println("verify: ok/fail set matches baseline")
        }
    }
}

private fun format(value: Double): String = String.format(Locale.ROOT, "%.3f", value)

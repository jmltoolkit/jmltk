/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.cli

import io.github.jmltoolkit.smt.Z3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * End-to-end tests for the `jmltk vcg` command: they spawn the *real* CLI as a
 * subprocess (a separate JVM running [MainKt] on the test classpath) and run it
 * against the KeY firstTouch example files shipped under
 * `src/test/resources/key/firstTouch`.
 *
 * The fixtures are the VCG-adapted KeY teaching material (05-ReverseArray,
 * 06-BinarySearch, 08-Java5, 10-SITA) that is also verified in-process in
 * `tools/vcg`; the deviations required by the harness (explicit non-null
 * `requires`, dropped `main`/Iterable machinery) are documented in the header
 * of `VcgExamples.java`. The CLI plumbing is what is asserted here: parse, VC
 * generation, parallel solving and the report format. Condition lines arrive in
 * completion order, so the assertions never depend on line order.
 */
@Timeout(300)
class VcgCommandE2eTest {
    private val isWindows: Boolean =
        System.getProperty("os.name", "").lowercase(Locale.ROOT).contains("win")

    /** Points at a KeY firstTouch fixture shipped as a test resource. */
    private fun fixture(example: String, file: String): Path {
        val rel = "key/firstTouch/$example/$file"
        val url = checkNotNull(javaClass.classLoader.getResource(rel)) { "missing test resource $rel" }
        return Path.of(url.toURI())
    }

    /** Output of one `vcg` subprocess run. */
    private class CliResult(val exitCode: Int, val output: String) {
        val lines: List<String> = output.lines()

        /** The `N conditions, M failing` summary line. */
        fun summary(): String =
            lines.lastOrNull { it.matches(Regex("\\d+ conditions, \\d+ failing")) }
                ?: throw AssertionError("no condition summary in output:\n$output")

        /** Condition lines that carry a verdict other than `ok`. */
        fun nonOkConditions(): List<String> =
            lines.filter { it.startsWith("  ") && !it.startsWith("  ok   ") }
    }

    /**
     * Runs `jmltk vcg` in a fresh JVM with the given arguments and returns its
     * exit code and the merged stdout/stderr. A watchdog [timeoutMillis] hard-
     * kills the subprocess so a hung solver cannot hang the test suite.
     */
    private fun runCli(vararg args: String, timeoutMillis: Long = 8 * 60_000L): CliResult {
        val javaHome = System.getProperty("java.home") ?: throw AssertionError("java.home not set")
        val java = Path.of(javaHome, "bin", if (isWindows) "java.exe" else "java").toString()
        val classpath = System.getProperty("java.class.path")
            ?: throw AssertionError("java.class.path not set")
        val command = listOf(
            java,
            "-cp", classpath,
            "--enable-native-access=ALL-UNNAMED",
            "io.github.jmltoolkit.cli.MainKt",
            "vcg",
        ) + args
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = StringBuilder()
        val reader = Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { output.append(it).append('\n') }
            }
        }
        reader.start()
        try {
            val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                throw AssertionError(
                    "vcg subprocess did not finish within ${timeoutMillis / 1000}s; " +
                    "partial output:\n$output"
                )
            }
            return CliResult(process.exitValue(), output.toString())
        } finally {
            // never leave a solver subprocess behind, even if the test thread is
            // interrupted (e.g. by the JUnit timeout)
            if (process.isAlive) process.destroyForcibly()
            reader.join(5_000)
        }
    }

    @Test
    fun binarySearchIsVerifiedThroughItsLoopInvariant() {
        assumeTrue(Z3.z3Installed(), "z3 not installed")
        // KeY firstTouch 06-BinarySearch: every generated condition (null-checks,
        // array bounds, loop-invariant init/preservation, variant, postcondition)
        // solves to `ok`; the CLI exits 0 and reports 20 conditions.
        val res = runCli(fixture("06-BinarySearch", "BinarySearch.java").toString())
        assertEquals(0, res.exitCode, res.output)
        assertEquals("20 conditions, 0 failing", res.summary())
        assertTrue(res.nonOkConditions().isEmpty(), "unexpected verdicts:\n${res.output}")
    }

    @Test
    fun sita3CommonEntryAndSwapAreVerified() {
        assumeTrue(Z3.z3Installed(), "z3 not installed")
        // KeY firstTouch 10-SITA: commonEntry (break loop) and swap prove through
        // their invariants. The rearranging driver is generated and checked too,
        // but its contract (`a1.length == a2.length`) is too weak for the default
        // runtime-check policy, so its conditions legitimately fail — the
        // in-process suite likewise only asserts commonEntry/swap. Exclude it from
        // the report to keep this e2e focused on the verified part.
        val res = runCli(
            fixture("10-SITA", "SITA3.java").toString(),
            "--vcg-exclude", "KeySITA3#rearrange()*",
        )
        assertEquals(0, res.exitCode, res.output)
        assertEquals("20 conditions, 0 failing", res.summary())
        assertTrue(res.nonOkConditions().isEmpty(), "unexpected verdicts:\n${res.output}")
    }

    @Test
    fun java5SumIsRejectedLoudly() {
        assumeTrue(Z3.z3Installed(), "z3 not installed")
        // KeY firstTouch 08-Java5 uses the JML `\sum` quantifier, which the VCG
        // does not model: the run must abort with the structured diagnostic and a
        // non-zero exit code, not silently mis-encode the contract.
        val res = runCli(fixture("08-Java5", "For.java").toString())
        assertTrue(res.exitCode != 0, "expected non-zero exit, got ${res.exitCode}:\n${res.output}")
        assertTrue(res.output.contains("\\sum"), res.output)
        assertTrue(res.output.contains("UnsupportedFeatureException"), res.output)
    }

    @Test
    fun reverseArrayGeneratesAndReportsBothMethods() {
        assumeTrue(Z3.z3Installed(), "z3 not installed")
        // KeY firstTouch 05-ReverseArray: the quantified `\old` content reasoning
        // exceeds the solver budget, so most verdicts are UNKNOWN (`????`) — the
        // in-process suite treats these methods as generation-only and asserts no
        // verdicts. The flags match that configuration (U_INV in the fixture list).
        // The e2e asserts the CLI plumbing: the KeY example is parsed, conditions
        // for *both* methods are generated and reported, and the run exits 0.
        val res = runCli(
            fixture("05-ReverseArray", "ReverseArray.java").toString(),
            "--unbounded", "--loop-invariant",
        )
        assertEquals(0, res.exitCode, res.output)
        // 31 = all conditions of both contracts (generation-pinned by the goldens)
        assertEquals(31, res.summary().substringBefore(" conditions").toInt(), res.output)
        // both VCG-adapted methods are targeted and reported
        assertTrue(res.lines.any { it.contains("KeyReverseArray#reverse(") }, res.output)
        assertTrue(res.lines.any { it.contains("KeyReverseArray#reverse2(") }, res.output)
        // the search itself never yields a counterexample — the solver budget only
        // turns its queries UNKNOWN (reverse2's two store-index checks do fail;
        // the fixture's invariant does not pin the store index)
        assertTrue(
            res.lines.none { it.startsWith("  FAIL") && it.contains("KeyReverseArray#reverse(") },
            res.output,
        )
    }
}

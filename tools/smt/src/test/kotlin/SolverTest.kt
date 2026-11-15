/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.smt

import io.github.jmltoolkit.smt.solver.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * @author Alexander Weigl
 * @version 1 (08.08.22)
 */
@Timeout(60)
class SolverTest {

    /**
     * A "solver" that never finishes on its own, on every OS.
     *
     * `sleep` does not exist on Windows. The Windows stand-in must also be
     * *silent* and *never read stdin*: the Solver pipes the query into the
     * process' stdin and parses its stdout incrementally, so a chatty stub
     * (like `ping` printing headers and replies) makes the SMT parser throw
     * before the watchdog kills it. `cmd /c "timeout /t 30 /nobreak >nul"`
     * would be silent, but `timeout.exe` refuses to run when stdin is
     * redirected, so the process exits immediately and the run returns before
     * the deadline. `cmd /c "ping -n 31 127.0.0.1 >nul"` stays alive ~30 s,
     * writes nothing to stdout, and never touches stdin.
     */
    private fun neverFinishingProcess(): Process {
        val cmd =
            if (System.getProperty("os.name").lowercase().contains("win")) {
                listOf("cmd", "/c", "ping -n 31 127.0.0.1 >nul")
            } else {
                listOf("sleep", "30")
            }
        return ProcessBuilder(cmd).start()
    }

    @Test
    @Throws(IOException::class)
    fun startZ3Mini() {
        Assumptions.assumeTrue(Z3.z3Installed())
        val s = Solver()
        val result = s.run("(assert (= (* 2 3) 6)) (check-sat) (get-model) (exit)")
        result.expectSat().consume()
    }

    @Test
    fun cancellationDestroysTheSolverProcess() {
        // a "solver" that never finishes on its own
        val solver = object : Solver() {
            override fun startSmtSolver(): Process = neverFinishingProcess()
        }
        val started = System.nanoTime()
        val answer = solver.run(
            { writer: java.io.PrintWriter -> writer.println("(check-sat)") },
            onForm = null,
            isCancelled = { true },
            timeoutMillis = 0L,
        )
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
        assertTrue(elapsedMs < 5000, "cancelled run returned after ${elapsedMs}ms; watchdog did not kill the process")
        assertTrue(answer.toString().isBlank(), "no answer should be read from a cancelled run")
    }

    @Test
    fun timeoutKillsExactlyAtTheDeadline() {
        // a "solver" that never finishes on its own: only the deadline can unblock the run
        val solver = object : Solver() {
            override fun startSmtSolver(): Process = neverFinishingProcess()
        }
        val started = System.nanoTime()
        val answer = solver.run(
            { writer: java.io.PrintWriter -> writer.println("(check-sat)") },
            onForm = null,
            isCancelled = { false },
            timeoutMillis = 500L,
        )
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
        assertTrue(elapsedMs >= 400, "timed out run must not return before the deadline (${elapsedMs}ms)")
        assertTrue(elapsedMs < 5000, "timed out run returned after ${elapsedMs}ms; watchdog missed the deadline")
        assertTrue(answer.toString().isBlank(), "no answer should be read from a timed-out run")
    }
}

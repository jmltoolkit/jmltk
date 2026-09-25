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
            override fun startSmtSolver(): Process = ProcessBuilder("sleep", "30").start()
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
}

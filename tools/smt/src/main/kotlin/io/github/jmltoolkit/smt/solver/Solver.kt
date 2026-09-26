/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.smt.solver

import io.github.jmltoolkit.smt.model.SExpr
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.PushbackReader
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Alexander Weigl
 * @version 1 (08.08.22)
 *
 * @param defaultTimeoutMillis fallback deadline used by runs that do not pass an
 *   explicit timeout, so no invocation blocks on the solver forever (0 disables the
 *   fallback).
 */
open class Solver(private val defaultTimeoutMillis: Long = 60_000L) {
    fun runAsync(input: String): ForkJoinTask<SolverAnswer> = ForkJoinPool.commonPool().submit<SolverAnswer> { run(input) }

    @Throws(IOException::class)
    open fun run(input: String): SolverAnswer = run { writer: PrintWriter -> writer.println(input) }

    @Throws(IOException::class)
    protected open fun startSmtSolver(): Process {
        val pb = ProcessBuilder("sh", "-c", "z3 -smt2 -in")
        return pb.start()
    }

    @Throws(IOException::class)
    open fun run(query: AppendableTo): SolverAnswer =
        run(query, onForm = null, isCancelled = { false }, timeoutMillis = 0)

    /**
     * Runs [query] on the SMT solver and parses the answer stream incrementally.
     *
     * A watchdog thread terminates the solver process tree as soon as [isCancelled]
     * becomes true, or once the deadline elapses. The deadline is [timeoutMillis]
     * when positive, otherwise this solver's [defaultTimeoutMillis]. A short-circuited
     * (cancelled/timed-out) run returns the answers parsed so far; the reader unblocks
     * because the terminating process closes its streams.
     *
     * @param onForm invoked for every top-level S-expression the solver emits (usually
     *   one `sat`/`unsat`/`unknown` verdict per checked condition).
     * @param isCancelled polled by the watchdog; must return `true` to abort the run.
     * @param timeoutMillis hard deadline for the whole run (0 = use the configured default).
     */
    @Throws(IOException::class)
    open fun run(
        query: AppendableTo,
        onForm: ((SExpr) -> Unit)?,
        isCancelled: () -> Boolean,
        timeoutMillis: Long,
    ): SolverAnswer {
        val p = startSmtSolver()
        val deadline = if (timeoutMillis > 0) timeoutMillis else defaultTimeoutMillis
        val watchdog = Watchdog(p, isCancelled, deadline)
        watchdog.start()
        try {
            PrintWriter(p.outputStream).use { out ->
                query.appendTo(out)
                out.close()
                val reader = PushbackReader(InputStreamReader(p.inputStream))
                val answers = ArrayList<SExpr>(1024)
                while (true) {
                    val sexpr = try {
                        SExprParser.parse(reader)
                    } catch (e: Exception) {
                        // a cancelled/timed-out run may cut the stream mid-form
                        if (watchdog.triggered.get() || isCancelled()) null else throw e
                    }
                    if (sexpr == null) break
                    answers.add(sexpr)
                    onForm?.invoke(sexpr)
                }
                return SolverAnswer(answers)
            }
        } finally {
            watchdog.shutdown()
            killTree(p)
        }
    }

    /**
     * A daemon monitoring the solver process. It kills the process when the caller
     * cancels the run or the deadline is reached — crucial because the main thread
     * may be blocked inside a read on the solver's output stream.
     */
    private class Watchdog(
        private val process: Process,
        private val isCancelled: () -> Boolean,
        timeoutMillis: Long,
    ) {
        /** Set once the watchdog terminated the process (cancellation or timeout). */
        val triggered = AtomicBoolean(false)
        private val done = AtomicBoolean(false)

        private val thread = Thread {
            val deadline = if (timeoutMillis > 0) {
                System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
            } else {
                Long.MAX_VALUE
            }
            while (!done.get() && process.isAlive) {
                if (isCancelled() || System.nanoTime() >= deadline) {
                    triggered.set(true)
                    killTree(process)
                    return@Thread
                }
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    return@Thread
                }
            }
        }

        fun start() {
            thread.isDaemon = true
            thread.start()
        }

        fun shutdown() {
            done.set(true)
            thread.interrupt()
        }
    }
}

/** Hard-kills a solver process tree (z3 runs as a child of `sh -c`). */
private fun killTree(process: Process) {
    runCatching { process.descendants().forEach { runCatching { it.destroyForcibly() } } }
    runCatching { process.destroyForcibly() }
}

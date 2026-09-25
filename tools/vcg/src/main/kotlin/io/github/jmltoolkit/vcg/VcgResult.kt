/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.Range
import io.github.jmltoolkit.smt.SmtQuery
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.solver.Solver

/**
 * A single verification condition. The obligation is negated and asserted with a
 * `:named` attribute; the solver must answer *unsat* for the condition to hold.
 *
 * @param range source location the condition is associated with (may be null
 *   for conditions without an obvious source anchor, e.g. clausal requirements).
 */
data class VerificationCondition(
    val id: String,
    val description: String,
    val obligation: SExpr,
    val range: Range? = null,
)

/**
 * Result of the verification condition generation.
 *
 * @property query the SMT query: declarations, assumptions and one
 * `push / assert(!(!obligation) :named vc_i) / check-sat / pop` block per condition.
 * @property conditions the emitted verification conditions in emission order.
 */
data class VcgResult(
    val query: SmtQuery,
    val conditions: List<VerificationCondition>,
) {
    /** The status of a single verification condition after checking. */
    enum class Status { PROVEN, FAILED, UNKNOWN }

    /**
     * Memoized per-condition status map produced by [check]. The solver is run at
     * most once per [VcgResult]; subsequent reads return this cached result, so e.g.
     * the CLI can call `check()` once per condition without re-invoking the solver.
     * Written under a benign race (both racers compute the same map).
     */
    @Volatile
    private var checked: Map<String, Status>? = null

    /**
     * Runs the solver against [query] and determines the status of each condition:
     * [PROVEN] when the solver answers `unsat`, [FAILED] when `sat`, otherwise [UNKNOWN].
     *
     * The result is memoized: the first call invokes [solver.run], every later call
     * returns the same map without touching the solver. This assumes a deterministic
     * solver; if you need to check with a different solver, build a fresh [VcgResult]
     * (e.g. via `copy()`).
     */
    fun check(solver: Solver = Solver()): Map<String, Status> {
        checked?.let { return it }
        return runCheck(solver, onCondition = null, isCancelled = { false }, timeoutMillis = 0)
            .also { checked = it }
    }

    /**
     * Like [check], but reports per-condition progress and supports cancellation.
     *
     * [onCondition] is invoked while the solver runs (with a `null` status, once per
     * answer received) and again after all answers are mapped (with the actual status).
     * The convention is `onCondition(alreadyDone, total, status)`.
     *
     * When [isCancelled] turns `true` (checked by a watchdog in the solver) the solver
     * process is terminated; conditions answered so far keep their status, the rest are
     * reported as [UNKNOWN] and the outcome is *not* memoized. [timeoutMillis] (0 = no
     * timeout) aborts the run the same way, so a stuck solver can never block the caller
     * forever and maps to [UNKNOWN] rather than an exception.
     */
    fun checkProgressive(
        onCondition: (index: Int, total: Int, status: Status?) -> Unit,
        solver: Solver = Solver(),
        isCancelled: () -> Boolean = { false },
        timeoutMillis: Long = 0,
    ): Map<String, Status> = runCheck(solver, onCondition, isCancelled, timeoutMillis)

    private fun runCheck(
        solver: Solver,
        onCondition: ((Int, Int, Status?) -> Unit)?,
        isCancelled: () -> Boolean,
        timeoutMillis: Long,
    ): Map<String, Status> {
        val total = conditions.size
        var seen = 0
        val answer = solver.run(
            query,
            onForm = { seen++; onCondition?.invoke(seen.coerceAtMost(total), total, null) },
            isCancelled = isCancelled,
            timeoutMillis = timeoutMillis,
        )
        // skip any error responses up to the first verdict
        try {
            answer.consumeErrors()
        } catch (e: Exception) {
            // fall through: if the solver protocol broke, everything is unknown
        }
        val map = LinkedHashMap<String, Status>()
        var done = 0
        for (vc in conditions) {
            val verdict = try {
                when {
                    answer.isSymbol("unsat") -> Status.PROVEN
                    answer.isSymbol("sat") -> Status.FAILED
                    else -> Status.UNKNOWN
                }
            } catch (e: Exception) {
                Status.UNKNOWN
            }
            map[vc.id] = verdict
            answer.consume()
            onCondition?.invoke(++done, total, verdict)
        }
        return map
    }

    /** Returns the conditions with status [Status.FAILED]; reuses the [check] cache. */
    fun failedConditions(solver: Solver = Solver()): List<VerificationCondition> {
        val status = check(solver)
        return conditions.filter { status[it.id] == Status.FAILED }
    }

    override fun toString(): String = query.toString()
}

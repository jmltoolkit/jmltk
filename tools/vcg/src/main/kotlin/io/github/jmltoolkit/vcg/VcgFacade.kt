/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import io.github.jmltoolkit.smt.solver.Solver
import java.io.PrintWriter
import java.io.StringWriter
import java.security.MessageDigest
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

/**
 * Facade around the VCG engine and the SMT solvers.
 *
 * Given [CompilationUnit]s (or the callables selected from them) it produces the
 * corresponding verification conditions ([VcgResult]) and dispatches them to the
 * SMT solver — synchronously ([check]) or asynchronously on a [ForkJoinPool]
 * ([checkAsync]). All batch operations run the independent per-callable tasks on
 * the pool and join them in submission order, so callers observe deterministic
 * results.
 *
 * A [VcgCache] may be attached to reuse earlier solver answers: a verification
 * condition whose *content* was already solved before (see [contentKey]) is not
 * sent to the solver again. The human-readable condition name is deliberately not
 * part of the identity.
 *
 * @param options engine options used for every generated [VcgResult].
 * @param pool the pool used for [verifyAll]/[checkAsync]/[verifyAndCheckAll].
 * @param solver provider of fresh solver instances (thread-confined; default spawns
 *   a fresh `z3` process per [Solver]).
 * @param cache shared between all checks of this facade (in-memory by default).
 */
class VcgFacade(
    val options: VcgOptions = VcgOptions(),
    val pool: ForkJoinPool = ForkJoinPool.commonPool(),
    private val solver: () -> Solver = { Solver() },
    private val cache: VcgCache = InMemoryVcgCache(),
) {
    //region selection & generation

    /** The callables of [cu] that carry a JML contract and a method body. */
    fun targets(cu: CompilationUnit): List<CallableDeclaration<*>> =
        cu.findAll(CallableDeclaration::class.java).filter { it.contracts.isNotEmpty() && hasBody(it) }

    /** The callables of all given compilation units that should be verified. */
    fun targets(cus: Iterable<CompilationUnit>): List<CallableDeclaration<*>> =
        cus.flatMap { targets(it) }

    /** Generates the verification conditions of a single callable. */
    fun verify(callable: CallableDeclaration<*>): VcgResult = Vcg(VcgContext.of(callable), options).verify()

    /** Generates the verification conditions of the given callables in parallel. */
    fun verifyAll(callables: Collection<CallableDeclaration<*>>): List<VcgResult> =
        callables.map { pool.submit<VcgResult> { verify(it) } }.map { it.get() }

    /** Generates the verification conditions of the given compilation units in parallel. */
    fun verifyAll(cus: Iterable<CompilationUnit>): List<VcgResult> = verifyAll(targets(cus))

    //endregion
    //region solving

    /**
     * Checks [result] against the SMT solver and returns a per-condition status map.
     *
     * When this facade holds a [VcgCache] the [contentKey] of [result] is consulted
     * first; a hit with a matching number of conditions avoids the solver entirely.
     * Otherwise the solver runs once ([VcgResult.check] memoises) and the outcome is
     * stored in the cache.
     */
    fun check(result: VcgResult): Map<String, VcgResult.Status> {
        val key = contentKey(result)
        val cached = cache.get(key)
        if (cached != null && cached.size == result.conditions.size) {
            val map = LinkedHashMap<String, VcgResult.Status>()
            result.conditions.forEachIndexed { i, vc -> map[vc.id] = cached[i] }
            return map
        }
        val computed = result.check(solver())
        cache.put(key, result.conditions.map { computed[it.id] ?: VcgResult.Status.UNKNOWN })
        return computed
    }

    /** Asynchronously checks [result] on [pool]; the returned task carries the status map. */
    fun checkAsync(result: VcgResult): ForkJoinTask<Map<String, VcgResult.Status>> =
        pool.submit<Map<String, VcgResult.Status>> { check(result) }

    /**
     * Checks all given results in parallel on [pool]; the returned list mirrors the
     * input order (submission order is preserved by joining in order).
     */
    fun checkAll(results: Collection<VcgResult>): List<VcgOutcome> =
        results.map { r -> pool.submit<VcgOutcome> { VcgOutcome(r, check(r)) } }.map { it.get() }

    /** Convenience: [verify] + [check] for a single callable. */
    fun verifyAndCheck(callable: CallableDeclaration<*>): VcgOutcome {
        val result = verify(callable)
        return VcgOutcome(result, check(result))
    }

    /** Convenience: [verifyAndCheck] for many callables, run in parallel on [pool]. */
    fun verifyAndCheckAll(callables: Collection<CallableDeclaration<*>>): List<VcgOutcome> {
        if (callables.isEmpty()) return emptyList()
        return callables.map { c ->
            pool.submit<VcgOutcome> {
                val result = verify(c)
                VcgOutcome(result, check(result))
            }
        }.map { it.get() }
    }

    /** Convenience: [verifyAndCheck] for the targets of the given compilation units. */
    fun verifyAndCheckAll(cus: Iterable<CompilationUnit>): List<VcgOutcome> =
        verifyAndCheckAll(targets(cus))

    //endregion

    /**
     * The outcome of verifying one callable: the generated conditions plus the
     * solver verdict per condition (keyed by [VerificationCondition.id]).
     */
    data class VcgOutcome(
        val result: VcgResult,
        val statuses: Map<String, VcgResult.Status>,
    )

    companion object {
        /**
         * Identity of a verification-condition query used by [VcgCache].
         *
         * Only the content that is sent to the SMT solver matters: comments,
         * newlines/tabs and doubled spaces are normalised away and the `:named`
         * symbols (the condition *names*) are replaced by a fixed placeholder, so
         * renaming a condition never invalidates cached answers.
         */
        fun contentKey(result: VcgResult): String {
            val writer = StringWriter()
            result.query.appendTo(PrintWriter(writer))
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(normalize(writer.toString()).toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }

        /** Strips comments, newlines, tabs, doubled spaces and `:named` symbols. */
        fun normalize(content: String): String =
            content
                .lineSequence()
                .map { it.substringBefore(';') } // SMT comments
                .joinToString(" ")
                .replace(Regex(":named\\s+[^\\s()]+"), ":named vc")
                .replace(Regex("\\s+"), " ")
                .trim()

        private fun hasBody(callable: CallableDeclaration<*>): Boolean = when (callable) {
            is MethodDeclaration -> callable.body.isPresent
            is ConstructorDeclaration -> callable.body.isPresent
            else -> false
        }
    }
}

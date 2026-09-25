/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.jml.clauses.JmlContract
import io.github.jmltoolkit.utils.JMLUtils

/**
 * Verification mode.
 *
 *  * [UNBOUNDED]: Java `int`/`long` are encoded as mathematical integers (`Int` sort),
 *    no overflow. Requires loop invariants/contracts for loops (or unrolling).
 *  * [BOUNDED]: Java primitive types are encoded as bit vectors with Java overflow
 *    semantics (like JBMC). Loops must be unrolled or bounded.
 */
enum class VerificationMode { UNBOUNDED, BOUNDED }

/** How a specific loop is handled. */
enum class LoopStrategy {
    /** Unroll the loop body a fixed number of iterations. */
    UNROLL,

    /** Abstract the loop by its (three-part) loop invariant. */
    INVARIANT,

    /**
     * Abstract the loop by a loop contract, i.e. invariant, decreases, assignable,
     * breaks/continues (abrupt termination inside the loop).
     */
    LOOP_CONTRACT,
}

/** How a method call is handled. */
enum class CallStrategy {
    /** Inline the callee's body (if resolvable and available). */
    INLINE,

    /**
     * Replace the call by its JML contract: assert the precondition, havoc the
     * assignable locations, assume the postcondition.
     */
    CONTRACT,
}

/**
 * Options of the verification condition generator.
 *
 * @param mode bounded or unbounded data types
 * @param defaultLoopStrategy used for loops without an individual entry in [loopStrategies]
 * @param loopStrategies individual strategy per loop AST node
 * @param loopUnrollDepth individual unrolling depth per loop AST node
 * @param defaultUnrollDepth global default unrolling constant
 * @param defaultCallStrategy used for calls without individual entry in [callStrategies]
 * @param callStrategies individual strategy per call site
 * @param maxInlineDepth recursion bound for [CallStrategy.INLINE]
 * @param checkOverflow emit VCs asserting no signed arithmetic overflow (bounded mode)
 * @param checkDivision emit VCs asserting no division/modulo by zero
 * @param checkIndex emit VCs asserting array indices are within bounds and non-null
 */
data class VcgOptions(
    val mode: VerificationMode = VerificationMode.BOUNDED,
    val defaultLoopStrategy: LoopStrategy = LoopStrategy.UNROLL,
    val loopStrategies: Map<Node, LoopStrategy> = emptyMap(),
    val loopUnrollDepth: Map<Node, Int> = emptyMap(),
    val defaultUnrollDepth: Int = 10,
    val defaultCallStrategy: CallStrategy = CallStrategy.CONTRACT,
    val callStrategies: Map<MethodCallExpr, CallStrategy> = emptyMap(),
    val maxInlineDepth: Int = 3,
    val checkOverflow: Boolean = false,
    val checkDivision: Boolean = false,
    val checkIndex: Boolean = false,
) {
    fun loopStrategy(loop: Node): LoopStrategy = loopStrategies[loop] ?: defaultLoopStrategy

    fun unrollDepth(loop: Node): Int = loopUnrollDepth[loop] ?: defaultUnrollDepth

    fun callStrategy(call: MethodCallExpr): CallStrategy = callStrategies[call] ?: defaultCallStrategy
}

/**
 * Context of a verification task: the callable (method or constructor) to verify,
 * its contract and the surrounding class members.
 */
data class VcgContext(
    val callable: CallableDeclaration<*>,
    val contract: com.github.javaparser.ast.jml.clauses.JmlContract,
) {
    val enclosingType: com.github.javaparser.ast.body.TypeDeclaration<*> by lazy {
        callable.findAncestor(com.github.javaparser.ast.body.TypeDeclaration::class.java)
            .orElseThrow { IllegalArgumentException("callable is not enclosed by a type") }
    }

    companion object {
        /**
         * Builds the verification context for a callable (method or constructor):
         * all its JML contracts are unrolled and their clauses merged into a single
         * contract.
         */
        fun of(callable: CallableDeclaration<*>): VcgContext {
            JMLUtils.unroll(callable.contracts)
            val joint = JmlContract()
            for (c in callable.contracts) {
                joint.clauses.addAll(c.clauses)
            }
            return VcgContext(callable, joint)
        }
    }
}

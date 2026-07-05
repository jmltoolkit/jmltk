/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.smt.model

open class SmtType private constructor(private val name: String) {
    class BitVec internal constructor(val width: Int) : SmtType("(_ BitVec $width)")

    override fun toString(): String = name

    class Array(val from: SmtType, val to: SmtType) : SmtType("(Array " + from.name + " " + to.name + ")")

    companion object {
        val FP32: SmtType = SmtType("(_ FloatingPoint 32)")
        val FP64: SmtType = SmtType("(_ FloatingPoint 64)")
        val STRING: SmtType = SmtType("String")

        private val bvCache: MutableMap<Int, BitVec> = HashMap(8)

        val COMMAND: SmtType = SmtType("_COMMAND_")
        val INT: SmtType = SmtType("Int")
        val REAL: SmtType = SmtType("Real")
        val BOOL: SmtType = SmtType("Bool")
        val BV8: SmtType = getBitVec(8)
        val BV16: SmtType = getBitVec(16)
        val BV32: SmtType = getBitVec(32)
        val BV64: SmtType = getBitVec(64)
        val SYMBOL: SmtType = SmtType("_SYMBOL_")
        val TYPE: SmtType = SmtType("_TYPE_")

        val JAVA_OBJECT: SmtType = SmtType("_TYPE_")

        fun getBitVec(width: Int): BitVec = bvCache.computeIfAbsent(width) { width: Int -> BitVec(width) }
    }
}

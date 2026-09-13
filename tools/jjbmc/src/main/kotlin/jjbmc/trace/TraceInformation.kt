/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.trace

import jjbmc.TranslationException
import java.util.*

class TraceInformation {
    private fun addRelevantVar(guess: String, lineNumber: Int) {
        getRelevantRange(lineNumber)?.let { (a, b) ->
            for (i in a..b) {
                try {
                    val assertVarsForLine = getAssertVarsForLine(i)
                    assertVarsForLine.add(guess)
                } catch (_: RuntimeException) {
                    // thats ok in this case
                }
            }
        }
    }

    companion object {
        val ignoredVars = setOf(
            "enableAssume",
            "enableNondet",
            "(void *)",
            "nondet_array_length",
            "array_data_init",
            "array_init_iter",
            "new_array_item",
            "malloc",
            "@class_identifier",
            "tmp",
            "assertionsDisabled"
        )
        private val lineMap: SortedMap<Int, Int> = TreeMap()
        private val methods: SortedMap<Int, String> = TreeMap()
        private val assertVars: SortedMap<Int, MutableSet<String>> = TreeMap()
        private val asserts: SortedMap<Int?, String?> = TreeMap()
        private val expressionMap: MutableMap<String, String> = HashMap<String, String>()

        fun reset() {
            lineMap.clear()
            methods.clear()
            asserts.clear()
            assertVars.clear()
            expressionMap.clear()
        }

        fun isRelevantValue(value: String): Boolean {
            return !value.contains("@class_identifier")
            // return !value.contains("{");
            // if (value.contains("dynamic")) {
            // return false;
            // }
        }

        fun getMethod(lineNumber: Int): String? = methods[lineNumber]

        private fun isRelevant(relevantVars: MutableSet<String>, guess: String?): Boolean {
            if (guess == null) {
                return false
            }
            for (s in relevantVars) {
                if (s == guess) {
                    return true
                }
                if (guess.contains("[") && guess.substring(0, guess.indexOf("[")) == s) {
                    return true
                }
                if (guess.contains(".") && guess.substring(0, guess.indexOf(".")) == s) {
                    return true
                }
            }
            return false
        }

        fun addLineEquality(printed: Int, orig: Int) {
            lineMap[printed] = orig
        }

        fun setExpressionMap(expressionMap: MutableMap<String, String>) {
            Companion.expressionMap.clear()
            Companion.expressionMap.putAll(expressionMap)
        }

        fun addMethod(line: Int, name: String?) {
            methods[line] = name
        }

        fun addAssert(line: Int, ass: String?) {
            asserts[line] = ass
        }

        fun addAssertVars(line: Int, vars: MutableSet<String>) {
            assertVars[line] = vars
        }

        fun getStartingLineForMethodAt(line: Int): Int {
            var idx: Int = methods.firstKey()!!
            for (k in methods.keys) {
                if (line < k!!) {
                    break
                } else {
                    idx = k
                }
            }
            return idx
        }

        fun getAssertForLine(line: Int): String? {
            if (!asserts.containsKey(line)) {
                throw TranslationException("Tried to access assert for line $line but found none.")
            }

            return asserts[line]
        }

        fun getOriginalLine(line: Int): Int {
            if (!lineMap.containsKey(line)) {
                return -1
            }
            return lineMap[line]!!
        }

        fun getAssertVarsForLine(line: Int): MutableSet<String> = assertVars[line] ?: error("No assert found in line: $line but requested variables for it.")

        fun getRelevantRange(lineIn: Int): Pair<Int, Int>? {
            var begin = -1
            for (line in methods.keys) {
                if (line!! <= lineIn) {
                    begin = line
                } else {
                    return Pair(begin, line)
                }
            }
            return null
        }

        fun isActualNewLine(oldLine: Int, newLine: Int): Boolean {
            val range = getRelevantRange(oldLine)
            return range?.let { (a, b) ->
                newLine != oldLine && newLine >= a && newLine < b
            } ?: false
        }

        fun cleanValue(value: String): String {
            var value = value
            value = value.trim { it <= ' ' }
            if (value.startsWith("(") && value.endsWith(")")) {
                value = value.substring(1, value.length - 1)
            }
            if (value.startsWith("(void *)")) {
                value = value.substring(8)
            }
            if (value.startsWith("&")) {
                value = value.substring(1)
            }
            if (value.endsWith("[0L]")) {
                value = value.substring(0, value.length - 4)
            }
            return value
        }

        fun cleanLHS(lhs: String): String {
            var lhs = lhs
            if (lhs.startsWith("(") && lhs.endsWith(")")) {
                lhs = lhs.substring(1, lhs.length - 1)
            }
            if (lhs.startsWith("(void *)")) {
                lhs = lhs.substring(8)
            }
            if (lhs.startsWith("&")) {
                lhs = lhs.substring(1)
            }
            return lhs
        }

        fun applyExpressionMap(lhs: String?): String? {
            expressionMap["returnVar"] = "\\result"
            if (lhs == null) {
                return null
            }
            var res: String? = lhs
            for (s in expressionMap.keys) {
                res = res!!.replace(s, expressionMap[s]!!)
            }
            return res
        }
    }
}

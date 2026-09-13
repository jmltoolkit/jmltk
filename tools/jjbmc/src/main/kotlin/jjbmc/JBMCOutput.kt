/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc

import jjbmc.trace.Trace
import jjbmc.trace.TraceInformation

class JBMCOutput(
    var proverStatus: String = "",
    val messages: MutableList<String> = ArrayList(),
    val errors: MutableList<String> = ArrayList(),
    val properties: MutableList<String> = ArrayList(),
    val reasons: MutableList<String> = ArrayList(),
    val asserts: MutableList<String> = ArrayList(),
    val traces: MutableList<Trace> = ArrayList(),
    val lineNumbers: MutableList<Int> = ArrayList()
) {

    fun addProperty(name: String, trace: Trace, lineNumber: Int, reason: String, ass: String) {
        properties.add(name)
        traces.add(trace)
        lineNumbers.add(lineNumber)
        reasons.add(reason)
        asserts.add(ass)
    }

    @JvmOverloads
    fun printTrace(property: String?, printGuesses: Boolean = true): String {
        val sb = StringBuilder()
        val idx = properties.indexOf(property)
        if (idx == -1) {
            return ""
        }
        val trace = traces[idx]
        if (trace == null) {
            return ""
        }

        sb.append("Trace for PVC: ")
            .append(property)
            .append(" in line ")
            .append(lineNumbers[idx])
            .append("\n")
        trace.filterAssignments()
        trace.getFinalVals()
        if (printGuesses) {
            for (a in trace.assignments) {
                a.guess?.let {
                    sb.append(it).append("\n")
                }
            }
        }

        if (asserts[idx] != null) {
            var assertion = asserts[idx]
            if (assertion != null && assertion.contains("\"Illegal assignment ")) {
                assertion = assertion.substring(assertion.indexOf("\"") + 1, assertion.length - 2)
            }
            sb.append("Fail in line ")
                .append(lineNumbers[idx])
                .append(": ")
                .append(assertion)
                .append(" (")
                .append(reasons[idx])
                .append(")\n")
            sb.append("with concrete values: \n")
            sb.append(printFinalVals(traces[idx]))
        } else {
            sb.append("Fail in line ")
                .append(lineNumbers[idx])
                .append(": ")
                .append(reasons[idx])
                .append("\n")
        }
        sb.append("\n")
        return sb.toString()
    }

    private fun printFinalVals(trace: Trace?): String {
        if (trace == null) return ""

        val sb = StringBuilder()
        for (k in trace.finalVals.keys) {
            sb.append(TraceInformation.applyExpressionMap(k)).append(" = ").append(trace.finalVals[k])
            sb.append("\n")
        }
        return sb.toString()
    }

    fun printErrors(): String {
        val sb = StringBuilder()
        for (e in errors) {
            sb.append(e)
        }
        return sb.toString()
    }

    fun printAllTraces(): String {
        val sb = StringBuilder()
        for (s in properties) {
            sb.append(printTrace(s))
        }
        return sb.toString()
    }

    fun printStatus(): String {
        val sb = StringBuilder()
        sb.append(proverStatus).append("\n")
        if (!errors.isEmpty()) {
            for (error in errors) {
                sb.append(error)
            }
        }
        return sb.toString()
    }

    private fun cutArrayString(s: String, size: Int): String {
        var size = size
        var pos = s.indexOf(",")
        while (--size > 0 && pos != -1) {
            pos = s.indexOf(",", pos + 1)
        }
        return s.substring(0, pos - 1) + "}"
    }
}

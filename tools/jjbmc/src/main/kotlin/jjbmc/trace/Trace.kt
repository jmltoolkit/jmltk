/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.trace

import jjbmc.Assignment
import java.util.*

data class Trace(
    val assignments: MutableList<Assignment>,
    val fullTraceRequested: Boolean = false,
    val maxArraySize: Int = 10
) {
    private val filteredAssignments: MutableList<Assignment> = LinkedList<Assignment>()
    private var allAssignments: MutableList<Assignment> = LinkedList<Assignment>()
    var relevantVars: MutableSet<String> = HashSet<String>()
    private val objectMap: MutableMap<String, String> = HashMap()
    private val reverseObjectMap: MutableMap<String, String> = HashMap()
    var finalVals: MutableMap<String, Any> = HashMap()

    private fun isRelevantVar(`var`: String): Boolean {
        if (`var` == null) {
            return false
        }
        if (`var`.startsWith("(") && `var`.endsWith(")")) {
            return false
        }
        if (`var`.contains("@")) {
            return false
        }
        for (s in relevantVars) {
            var s = s
            val vars = `var`.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (v in vars) {
                var v = v
                s = s.replace("this.", "").trim { it <= ' ' }
                v = v.replace("this.", "").trim { it <= ' ' }
                if (s == v || v.startsWith(v + ".")) {
                    return true
                }
            }
        }
        return `var`.contains("[") && isRelevantVar(`var`.substring(0, `var`.lastIndexOf("[")))
    }

    fun filterAssignments() {
        val trace =
            allAssignments // trace = trace.stream().filter(a -> !a.jbmcVarname.equals("this")).collect(Collectors.toList());
                .filter { a -> !a.jbmcVarname.contains("malloc") }
                .filter { a -> !a.jbmcVarname.contains("this$0") }
                .filter { a -> !a.jbmcVarname.contains("derefd_pointer") }
                .toMutableList()
        // trace = trace.stream().filter(a -> !a.value.contains("@class_identifier") &&
        // !a.value.startsWith("[")).collect(Collectors.toList());
        allAssignments = trace

        var res: MutableList<Assignment?> = ArrayList()
        var idx = 0
        var group: MutableList<Assignment>?
        while (idx < trace.size) {
            group = ArrayList<Assignment>()
            group.add(trace[idx])
            var newIdx = idx
            var i = idx
            while (i < trace.size - 1 &&
                !TraceInformation.isActualNewLine(
                    trace[idx].lineNumber,
                    trace[i + 1].lineNumber
                )
            ) {
                newIdx = i + 1
                group.add(trace[i + 1]!!)
                ++i
            }
            idx = newIdx
            provideGuesses(group)
            group = filterGroup(group)
            for (assignment in group) {
                assignment.guessedValue = (getValue(assignment.value, idx))
                if ((
                    assignment.jbmcVarname.contains("_object") ||
                        assignment.jbmcVarname.contains("_array")
                ) &&
                    assignment.jbmcVarname.startsWith("dynamic_")
                ) {
                    if (assignment.jbmcVarname.contains("[")) {
                        assignment.guessedValue =
                            getValue(
                                assignment
                                    .jbmcVarname
                                    .substring(
                                        0, assignment.jbmcVarname.indexOf("[")
                                    ),
                                idx
                            )
                        assignment.guess = assignment.guess?.substringBefore('[')
                    } else {
                        assignment.guessedValue = getValue(assignment.jbmcVarname, idx)
                    }
                }
            }
            group = filterGroup(group)
            idx++
            res.addAll(group)
        }

        if (fullTraceRequested) {
            res = res.filter { isRelevantVar(it.guess) }.toList()
        }
    }

    private fun getValue(value: String, idx: Int): Any? {
        var value = value
        value = value.trim { it <= ' ' }
        value = TraceInformation.cleanValue(value)
        if (value.contains("#")) {
            // not sure if this is always correct
            return arrayOfNulls<Any>(maxArraySize).toList()
        }
        if (value == "null") {
            return "null"
        }
        try {
            return value.toInt()
        } catch (e: NumberFormatException) {
            // this may happen
        }
        try {
            return value.toFloat()
        } catch (e: NumberFormatException) {
            // this may happen
        }
        try {
            return value.toLong()
        } catch (e: NumberFormatException) {
            // this may happen
        }
        try {
            return value.toDouble()
        } catch (e: NumberFormatException) {
            // this may happen
        }
        if (value == "true") {
            return true
        }
        if (value == "false") {
            return false
        }
        if (value.startsWith("&dynamic") || value.startsWith("dynamic_")) {
            return findValue(value, idx)
        }
        if (value.startsWith("{")) {
            // its an array
            value = value.replace("{", "")
            value = value.substring(0, value.length - 1)
            val values = value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (!values[0].trim { it <= ' ' }.startsWith(".@")) {
                // its an array
                val vals: MutableList<Any?> = ArrayList()
                for (`val` in values) {
                    vals.add(getValue(`val`, idx))
                }
                return vals
            } else {
                // its an object
                val vals: MutableMap<String?, Any?> = HashMap()
                for (`val` in values) {
                    var `val` = `val`
                    `val` = `val`.trim { it <= ' ' }
                    var key = `val`.substring(0, `val`.indexOf("=")).trim { it <= ' ' }
                    key = key.replace(".", "")
                    val innerVal = `val`.substring(`val`.indexOf("=") + 1).trim { it <= ' ' }

                    if (!key.startsWith("@") && !key.contains("this$0")) {
                        vals.put(key, getValue(innerVal, idx))
                    }
                }
                return vals
            }
        }
        // guess its a String
        return value
    }

    private fun filterGroup(group: MutableList<Assignment>): MutableList<Assignment> {
        // group = group.stream().filter(a -> !a.value.contains("dynamic_object")).collect(Collectors.toList());
        val groupMap = LinkedHashMap<String?, Assignment?>()
        for (a in group) {
            groupMap.put(a.guess, a)
        }
        groupMap.remove(null)
        return ArrayList<Assignment>(groupMap.values)
    }

    private fun findValue(value: String, maxIdx: Int = allAssignments.size - 1): Any? {
        // all assignments in the same lane will be respected
        var value = value
        var maxIdx = maxIdx
        while (maxIdx < allAssignments.size - 1 &&
            (allAssignments[maxIdx].lineNumber == allAssignments[maxIdx + 1].lineNumber)
        ) {
            maxIdx++
        }
        value = value.replace("&", "")
        for (i in maxIdx downTo 0) {
            if (allAssignments[i].jbmcVarname.equals(value) ||
                allAssignments[i].jbmcVarname.equals(value + ".data")
            ) {
                var `val` = getValue(allAssignments[i].value, maxIdx)
                if (`val` is ArrayList<*>) {
                    `val` = performArrayUpdates(allAssignments[i].jbmcVarname, `val`, i, maxIdx)
                }
                if (`val` is MutableMap<*, *>) {
                    `val` = performFieldUpdates(allAssignments[i].jbmcVarname, `val`, i, maxIdx)
                }
                return `val`
            }
        }
        if (value.startsWith("dynamic_object")) {
            return performFieldUpdates(value, HashMap<String?, Any?>(), 0, maxIdx)
        }
        return noValue
    }

    private fun performArrayUpdates(varName: String?, `val`: Any?, idx: Int, maxIdx: Int): Any {
        val valArray = `val` as ArrayList<Any?>
        for (i in idx..<maxIdx) {
            if (allAssignments[i].jbmcVarname.startsWith(varName + "[")) {
                try {
                    var s: String = allAssignments[i].jbmcVarname
                    s = s.replace("L]", "]")
                    s = s.substring(s.indexOf("[") + 1, s.indexOf("]"))
                    val index = s.toInt()
                    if (index >= valArray.size) {
                        println("error updating array in trace.")
                    } else {
                        valArray[index] = getValue(allAssignments[i].value, maxIdx)
                    }
                } catch (e: NumberFormatException) {
                    throw RuntimeException("Error parsing the trace.")
                }
            }
        }
        return valArray
    }

    private fun performFieldUpdates(varName: String?, `val`: Any?, idx: Int, maxIdx: Int): Any {
        val valMap = `val` as MutableMap<String?, Any?>
        for (i in idx..<maxIdx) {
            if (allAssignments[i].jbmcVarname.startsWith(varName + ".")) {
                val s: String = allAssignments[i].jbmcVarname
                val fieldName = s.substring(s.indexOf(".") + 1)
                if (!fieldName.startsWith("@") && !fieldName.contains("this$")) {
                    valMap.put(fieldName, getValue(allAssignments[i].value))
                }
            }
        }
        return valMap
    }

    fun getFinalVals() {
        for (rv in relevantVars) {
            var rv = rv
            for (a in this.filteredAssignments) {
                val vars: Array<String> = a.guess?.split("=") ?: arrayOf()
                for (v in vars) {
                    var v = v
                    v = v.trim { it <= ' ' }.replace("this.", "")
                    rv = rv.trim { it <= ' ' }.replace("this.", "")
                    if (v == rv) {
                        finalVals.put(rv, a.guessedValue.toString())
                    }
                }
            }
        }
    }

    private fun getValue(value: String): Any? = getValue(value, allAssignments.size - 1)

    fun provideGuesses(lineAssignments: MutableList<Assignment>) {
        for (a in lineAssignments) {
            a.value = TraceInformation.cleanValue(a.value)
            a.jbmcVarname = (TraceInformation.cleanLHS(a.jbmcVarname))
            if (TraceInformation.isRelevantValue(a.value)) {
                if (a.value.startsWith("dynamic_")) {
                    val value: String = TraceInformation.cleanValue(a.value)
                    trackDynamicObject(a.jbmcVarname, value)
                }
                if (a.jbmcVarname.endsWith(".data") &&
                    a.value.contains("dynamic_") &&
                    a.value.contains("_array")
                ) {
                    val value: String = TraceInformation.cleanValue(a.value)
                    trackDynamicObject(
                        a.jbmcVarname.substring(0, a.jbmcVarname.length - 5), value
                    )
                }
            }
        }
        for (i in lineAssignments.indices) {
            val a = lineAssignments[i]
            if (a.jbmcVarname.startsWith("array_data_init")) {
                processArrayInit(lineAssignments, i)
            }
            if (TraceInformation.isRelevantValue(a.value)) {
                a.guess = (guessVariable(a.jbmcVarname))
                if (a.guess != null && a.parameterName != null) {
                    val method = TraceInformation.getMethod(TraceInformation.getStartingLineForMethodAt(a.lineNumber))
                    if (a.parameterName.contains(method)) {
                        if (!a.guess.isEmpty()) {
                            relevantVars.add(a.guess)
                        }
                    }
                }
            }
            a.lineNumber = TraceInformation.getOriginalLine(a.lineNumber)
        }
    }

    private fun processArrayInit(lineAssignments: MutableList<Assignment>, idx: Int) {
        var arrayIdx = 0
        val arrayName: String? = lineAssignments[idx].value
        val length = findArrayLength(arrayName, idx, lineAssignments)
        if (length <= 0) {
            return
        }
        var i = idx
        while (true) {
            val a = lineAssignments[i]
            if (a.jbmcVarname.startsWith("new_array_item")) {
                a.jbmcVarname = "$arrayName[$arrayIdx]"
            }
            if (a.jbmcVarname.startsWith("array_init_iter")) {
                var newIdx = -1
                try {
                    newIdx = a.value.toInt()
                } catch (e: Exception) {
                    println("Error parsing trace.")
                }
                if (newIdx == length) {
                    break
                } else {
                    arrayIdx = newIdx
                }
            }
            ++i
        }
    }

    private fun findArrayLength(arrayName: String?, startIdx: Int, assignments: MutableList<Assignment>): Int {
        var arrayName = arrayName
        var startIdx = startIdx
        while (startIdx < assignments.size - 1 &&
            (
                assignments[startIdx].lineNumber
                === assignments[startIdx + 1].lineNumber
            )
        ) {
            startIdx++
        }
        while (arrayName!!.contains("array")) {
            arrayName = objectMap[arrayName]
            if (arrayName == null) {
                return -1
            }
        }
        var i = startIdx
        while (0 <= i) {
            if (assignments[i].jbmcVarname.equals(arrayName + ".length")) {
                return assignments[i].value.toInt()
            }
            --i
        }
        return -1
    }

    private fun trackDynamicObject(jbmcVarname: String, value: String) {
        if (!jbmcVarname.contains("array_data")) {
            objectMap[value] = jbmcVarname
        }
        reverseObjectMap[jbmcVarname] = value
    }

    private fun getObjectName(obj: String?) = objectMap[obj]

    private fun applyObjectMap(lhs: String): String? {
        if (lhs.contains(".")) {
            val `object` = getObjectName(lhs.substring(0, lhs.indexOf(".")))
            if (`object` == null && !lhs.contains("[")) {
                return lhs
            } else if (`object` != null) {
                val res = lhs.replace(lhs.substring(0, lhs.indexOf(".")), `object`)
                if (res.endsWith(".data")) {
                    return res.substring(0, res.length - 5)
                }
                return res
            }
        }
        if (lhs.contains("[")) {
            val `object` = getObjectName(lhs.substring(0, lhs.indexOf("[")))
            if (`object` == null) {
                return lhs
            }
            if (`object`.endsWith(".data")) {
                return lhs.replace(lhs.substring(0, lhs.indexOf("[")), `object`.substring(0, `object`.length - 5))
            }
            return lhs.replace(lhs.substring(0, lhs.indexOf("[")), `object`)
        }
        val `object` = getObjectName(lhs)
        if (`object` != null) {
            return applyObjectMap(`object`)
        }
        return lhs
    }

    fun guessVariable(lhs: String?): String? {
        if (lhs == null) {
            return null
        }

        var res = StringBuilder(applyObjectMap(lhs))
        var oldRes: String? = null
        while (res.toString() != oldRes) {
            oldRes = res.toString()
            res = StringBuilder(applyObjectMap(res.toString()))
        }

        var tmpRes: String? = res.toString()
        var rest = ""
        if (tmpRes!!.contains("[")) {
            rest = tmpRes.substring(tmpRes.indexOf("["))
            tmpRes = tmpRes.substring(0, tmpRes.indexOf("["))
        }
        val `object` = reverseObjectMap[tmpRes]
        if (`object` != null) {
            res = StringBuilder()
            for (k in reverseObjectMap.entries) {
                if (k.value == `object` && relevantVars.contains(k.key)) {
                    res.append(k.key).append(" = ")
                }
            }
            if (!res.isEmpty()) {
                tmpRes = res.substring(0, res.length - 3)
            } else {
                return null
            }
        }
        return tmpRes + rest
    }

    companion object {
        private val noValue = Any()
    }
}

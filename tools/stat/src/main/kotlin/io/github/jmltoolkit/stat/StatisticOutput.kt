/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.stat

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import java.io.PrintWriter
import kotlin.math.max

/**
 *
 * @author Alexander Weigl
 * @version 1 (12.08.26)
 */
abstract class StatisticOutput(protected val statSelect: Set<String>?) {
    protected var indent = 0

    fun run(global: Statistics, cu: List<CompilationUnit>) {
        indent = 0
        reportLine(" ", "TOTAL", global.summary())
        cu.forEach { cu -> report(cu) }
    }

    fun report(cu: CompilationUnit) {
        val name = cu.primaryTypeName.orElse(null)
            ?: cu.types.firstOrNull()?.nameAsString
            ?: cu.storage.orElse(null)?.fileName
            ?: "<noname>"
        reportLine("C", name, cu)
        withIndent {
            cu.types.forEach { type -> report(type) }
        }
    }

    protected fun withIndent(function: () -> Unit) {
        indent++
        function()
        indent--
    }

    fun report(type: TypeDeclaration<*>) {
        reportLine("T", type.nameAsString, type)

        withIndent {
            type.members().filterIsInstance<ConstructorDeclaration>()
                .sortedBy { it.nameAsString + it.signature.toString() }
                .forEach { constr -> report(constr) }

            type.members().filterIsInstance<MethodDeclaration>()
                .sortedBy { it.nameAsString + it.signature.toString() }
                .forEach { constr -> report(constr) }

            type.members().filterIsInstance<TypeDeclaration<*>>()
                .sortedBy { it.nameAsString }
                .forEach { constr -> report(constr) }
        }
    }

    private fun report(method: MethodDeclaration) {
        reportLine("c", method.signature.toString(), method)
    }

    private fun report(constr: ConstructorDeclaration) {
        reportLine("c", constr.signature.toString(), constr)
    }

    abstract fun start()
    abstract fun end()
    fun reportLine(type: String, repr: String, n: Node) {
        val stat = try {
            n.getData(DATA_KEY_STATISTICS)
        } catch (_: IllegalStateException) {
            Statistics()
        }
        val summary = stat.summary()
        reportLine(type, repr, summary)
    }

    abstract fun reportLine(type: String, repr: String, s: Statistics)
}

val NAME = "NAME"
val TYPE = "TYPE"

class StatisticsTxtOutput(val out: PrintWriter, statSelect: Set<String>?) : StatisticOutput(statSelect) {
    private val allKeys = mutableMapOf<String, Int>()
    private val result = mutableListOf<Map<String, String>>()

    override fun start() {}
    override fun end() {
        val selectableKeys = allKeys.keys
            .filter { it != NAME && it != TYPE }
            .filter { statSelect == null || it in statSelect }
            .sortedBy { it }
            .toList()

        val keys: List<String> = listOf(TYPE, NAME) + selectableKeys
        val colSize = keys.map { max(allKeys[it] ?: 0, it.length) + 2 }.toList()
        val format = colSize.mapIndexed { index, i -> if (index <= 1) "%-${i}s" else "%${i}s" }
            .joinToString(" | ", prefix = "| ", postfix = " |\n")

        val sepLine = { c: String ->
            val values =
                colSize.map { it + 2 }.map { c * it }.joinToString("|", prefix = "|", postfix = "|\n") { it }
            out.print(values)
        }

        val values = keys.map { it }.toTypedArray()
        out.printf(format, *values)
        sepLine("=")

        result.forEach { row ->
            val values = keys.map { k -> row[k] ?: " " }.toTypedArray()
            out.printf(format, *values)
        }
    }

    override fun reportLine(type: String, repr: String, summary: Statistics) {
        val line = mutableMapOf<String, String>().also { result.add(it) }

        line[NAME] = (" " * indent) + repr
        line[TYPE] = type

        summary.data().forEach { (key, value) ->
            line[key.name] = key.toString(value)
        }

        line.forEach { (k, v) ->
            allKeys[k] = max(allKeys.getOrDefault(k, 0), v.length)
        }
    }
}

private operator fun String.times(count: Int): String {
    if (count <= 0) return ""
    return buildString { repeat(count) { append(this@times) } }
}

enum class LineType {
    CU,
    TYPE,
    METHOD,
    CONSTR
}

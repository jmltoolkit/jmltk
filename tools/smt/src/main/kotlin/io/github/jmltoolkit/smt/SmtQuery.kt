/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.smt

import io.github.jmltoolkit.smt.SmtTermFactory.command
import io.github.jmltoolkit.smt.SmtTermFactory.nonNull
import io.github.jmltoolkit.smt.SmtTermFactory.variable
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.model.SmtType
import io.github.jmltoolkit.smt.solver.AppendableTo
import java.io.PrintWriter
import java.io.StringWriter

open class SmtTheory() {
    protected val theories = mutableSetOf<SmtTheory>()
    internal val commands: MutableList<SExpr> = ArrayList(1024)

    fun addPreamble(theory: SmtTheory) = theories.add(theory)

    fun declareSort(name: String): SmtType {
        addCommand("declare-sort", SmtTermFactory.symbol(name))
        return SmtType.userDefined(name)
    }


    open fun declareConst(name: String, type: SmtType) {
        val a = SmtTermFactory.list(
            null, SmtType.COMMAND,
            "declare-const", name, SmtTermFactory.type(type)
        )
        commands.add(a)
    }

    open fun declareFun(name: String, vararg type: SmtType) : SmtFunction {
        val f = SmtFunction(name, type.asList())
        commands.add(f.declare())
        return f
    }

    open fun defineFun(name: String, vararg type: Pair<String, SmtType>, expr: SExpr): SmtFunction {
        val types = type.map { it.second }.toList()
        val f = SmtFunction(name, types)
        commands.add(f.define(type.map { it.first }.toList(), expr))
        return f
    }


    fun defineThis() {
        declareConst("this", SmtType.JAVA_OBJECT)
        addAssert(nonNull(variable(SmtType.JAVA_OBJECT, null, "this")))
    }

    fun addAssert(nonNull: SExpr) {
        commands.add(command("assert", nonNull))
    }

    /**
     * Adds an arbitrary SMT-LIB command to the query.
     */
    fun addCommand(symbol: String, vararg args: SExpr) {
        commands.add(command(symbol, *args))
    }

    val allTheories: Set<SmtTheory> =
        setOf(this) + theories.flatMap { it.allTheories }.toSet()
}

/**
 * @author Alexander Weigl
 * @version 1 (07.08.22)
 */
class SmtQuery : SmtTheory(), AppendableTo {
    private val variableStack: MutableList<MutableMap<String, SmtType>> = ArrayList()

    init {
        variableStack.add(HashMap())
    }

    fun push() {
        variableStack.add(HashMap())
        commands.add(term.command("push"))
    }

    fun pop() {
        variableStack.remove(currentFrame)
        commands.add(term.command("pop"))
    }

    override fun declareConst(name: String, type: SmtType) {
        if (!declared(name)) {
            val a = term.list(
                null, SmtType.COMMAND,
                "declare-const", name, term.type(type)
            )
            commands.add(a)
            currentFrame[name] = type
        }
    }

    private fun declared(name: String): Boolean = currentFrame.containsKey(name)

    private val currentFrame
        get() = variableStack[variableStack.size - 1]

    override fun appendTo(writer: PrintWriter) {
        for (command in commands) {
            command.appendTo(writer)
            writer.println()
        }
    }

    fun checkSat() {
        commands.add(term.command("check-sat"))
    }

    override fun toString(): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)

        allTheories.asSequence().filter { it != this }
            .forEach {
                pw.format(";; THEORY %s%n", it.javaClass.simpleName)
                for (e in it.commands) {
                    e.appendTo(pw)
                    pw.println()
                }
            }


        commands.forEach { a: SExpr ->
            a.appendTo(pw)
            pw.println()
        }

        return sw.toString()
    }

    companion object {
        private val term: SmtTermFactory = SmtTermFactory
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.stat

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.jml.body.JmlClassAccessibleDeclaration
import com.github.javaparser.ast.jml.body.JmlClassExprDeclaration
import com.github.javaparser.ast.jml.clauses.JmlClause
import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr
import com.github.javaparser.ast.stmt.Behavior
import java.util.*

data class Statistics(
    private val data: MutableMap<StatisticKey<*>, Any> = IdentityHashMap(),
    val children: MutableList<Statistics> = mutableListOf()
) {
    operator fun get(key: JmlClauseKind) = StatisticKeys.jmlClause(key).let { this[it] }
    operator fun get(key: String) = StatisticKeys.keys[key]?.let { this[it] }
    operator fun <T : Any> get(key: StatisticKey<T>) = (data[key] ?: key.defValue) as T
    operator fun set(key: StatisticKey<*>, value: Any) {
        data[key] = value
    }

    operator fun plus(x: Statistics): Statistics {
        val newStat = Statistics()
        val keys = x.data.keys + data.keys
        for (key in keys) {
            val a = key.aggregate(this[key], x[key])
            newStat.data[key] = a
        }
        return newStat
    }

    fun subStat(n: Node): Statistics = Statistics().also {
        this.children.add(it)
        n.setData(DATA_KEY_STATISTICS, it)
    }

    fun data() = data.toMap()

    fun inc(key: StatisticKey<*>) {
        this[key] = key.inc(this[key])
    }

    fun inc(key: JmlClause) {
        inc(StatisticKeys.jmlClause(key.kind))
        inc(StatisticKey.JML_CLAUSE)
    }

    fun inc(n: com.github.javaparser.ast.Modifier) {
        inc(StatisticKeys.modifier(n.keyword))
    }

    fun summary(): Statistics {
        var copy = this
        for (c in children) {
            copy += c.summary()
        }
        return copy
    }

    fun inc(n: JmlClassExprDeclaration) {
        inc(StatisticKey.JML_BODY_DECLARATION)
        inc(StatisticKeys.classExpr(n.kind))
    }

    fun inc(n: JmlClassAccessibleDeclaration) {
        inc(StatisticKey.JML_BODY_DECLARATION)
        inc(StatisticKey.BODY_ACCESSIBLE)
    }

    fun inc(binder: JmlQuantifiedExpr.JmlBinder) {
        inc(StatisticKey.totalQuantifiers)
        inc(StatisticKeys.binder(binder.jmlSymbol()))
    }

    fun inc(behavior: Behavior) {
        inc(StatisticKeys.behavior(behavior.jmlSymbol()))
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.stat

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import java.util.*
import kotlin.math.max

object StatisticKeys {
    val keys = TreeMap<String, StatisticKey<*>>()
    fun <T : Any> register(key: StatisticKey<T>) {
        keys[key.name ?: ""] = key
    }

    fun normalize(s: String) = s.replace("\\", "").uppercase()

    private fun getOrCreate(n: String): StatisticKey<*> {
        val n = normalize(n)
        val key = keys[n]
        return if (key != null) {
            key
        } else {
            val key = StatisticKey.ArbitraryIntSumKey(n)
            register(key)
            key
        }
    }

    fun jmlClause(kind: JmlClauseKind) = getOrCreate("CLAUSE_$kind")
    fun modifier(keyword: Modifier.Keyword) = getOrCreate("MODIFIER_$keyword")
    fun classExpr(keyword: SimpleName) = getOrCreate("CLASS_$keyword")
    fun classExpr(keyword: String) = getOrCreate("CLASS_$keyword")
    fun behavior(keyword: Any) = getOrCreate("BEHAVIOR_$keyword")
    fun binder(keyword: Any) = getOrCreate("BINDER_$keyword")
}

sealed class StatisticKey<T : Any>(val defValue: Any) {
    init {
        StatisticKeys.register(this)
    }

    open val name get() = javaClass.simpleName
    override fun toString() = name

    abstract fun aggregate(a: Any, b: Any): Any
    abstract fun toString(a: Any): String
    abstract fun inc(any: Any): Any

    open class IntSumKey : StatisticKey<Int>(0) {
        override fun aggregate(a: Any, b: Any) = (a as Int) + (b as Int)
        override fun toString(a: Any) = "%d".format(a as Int)
        override fun inc(any: Any): Any = (any as Int) + 1
    }

    data class ArbitraryIntSumKey(override val name: String) : IntSumKey()

    private open class DoubleSumKey : StatisticKey<Double>(1.0) {
        override fun aggregate(a: Any, b: Any) = (a as Double) + (b as Double)
        override fun toString(a: Any) = "%8.2f".format(a as Double)
        override fun inc(any: Any): Any = (any as Double) + 1.0
    }

    open class IntMaxKey : StatisticKey<Int>(0) {
        override fun aggregate(a: Any, b: Any) = max(a as Int, b as Int)
        override fun toString(a: Any) = "%d".format(a as Int)
        override fun inc(any: Any): Any = (any as Int) + 1
    }

    data object TOTALCLASSES : IntSumKey()
    data object TOTAL_METHODS : IntSumKey()
    data object TOTAL_FIELDS : IntSumKey()
    data object SPECIFIED_METHODS : IntSumKey()
    data object REQUIRES : IntSumKey()
    data object ASSIGNABLES : IntSumKey()
    data object INVARIANTS : IntSumKey()
    data object GHOST_FIELDS : IntSumKey()
    data object totalModelMethods : IntSumKey()
    data object pureMethods : IntSumKey()
    data object helperMethods : IntSumKey()
    data object totalQuantifiers : IntSumKey()
    data object CONTRACT_METHOD : IntSumKey()
    data object CONTRACT_LAMBDA : IntSumKey()
    data object CONTRACT_STATEMENT : IntSumKey()
    data object CONTRACT_BLOCK : IntSumKey()
    data object CONTRACT_LOOP : IntSumKey()
    data object CONTRACT_LOOPINV : IntSumKey()
    data object JML_CLAUSE : IntSumKey()
    data object JML_BODY_DECLARATION : IntSumKey()
    data object BODY_ACCESSIBLE : IntSumKey()
    data object LET : IntSumKey()
}

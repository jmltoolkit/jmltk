/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.redux

import com.github.javaparser.ast.Node
import java.util.*

/**
 * @author Alexander Weigl
 * @version 1 (12/29/21)
 */
class ReduxFacade(private val config: ReduxConfig) {
    private val transformers: MutableList<Transformer> = ArrayList()

    constructor(config: ReduxConfig, transformers: List<Transformer>) : this(config) {
        this.transformers.addAll(transformers)
    }

    fun apply(nodes: List<Node>): List<Node?> {
        val n: MutableList<Node?> = ArrayList(nodes.size)
        for (node in nodes) {
            var new = node
            for (transformer in transformers) {
                new = transformer.apply(new)
            }
            n.add(new)
        }
        return n
    }

    companion object {
        fun create(config: ReduxConfig): ReduxFacade {
            val sl = ServiceLoader.load(Transformer::class.java)
            return ReduxFacade(config, sl.filter { config.isEnabled(it.javaClass.name) })
        }
    }
}

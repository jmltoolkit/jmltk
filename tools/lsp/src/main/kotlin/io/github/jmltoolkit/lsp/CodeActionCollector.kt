/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import com.github.javaparser.ast.Node
import io.github.jmltoolkit.lsp.actions.LspAction
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.*

/**
 * This visitor gathers actions, that can be executed on nodes within the given range.
 */
object CodeActionCollector {
    internal val actions by lazy {
        ServiceLoader.load(LspAction::class.java).toList()
    }

    val actionTable by lazy {
        HashMap<Class<out Node>, List<LspAction>>(128)
    }

    fun collect(uri: String, node: Node): List<Either<Command, CodeAction>> {
        val result: MutableList<Either<Command, CodeAction>> = arrayListOf()
        node.walk {
            result += createCodeAction(uri, node)
        }
        return result
    }

    fun createCodeAction(uri: String, node: Node): List<Either<Command, CodeAction>> {
        val actions = actionTable.computeIfAbsent(node.javaClass) {
            actions.filter { it.isCallableForNode(node) }.toList()
        }
        return actions.mapNotNull { it.createCodeAction(uri, node) }
    }

    @JvmName("collectN")
    fun collect(uri: String, node: Node?): List<Either<Command, CodeAction>> {
        if (node != null) {
            return collect(uri, node)
        }
        return emptyList()
    }
}

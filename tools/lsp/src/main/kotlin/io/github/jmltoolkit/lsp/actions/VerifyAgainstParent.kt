/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.jml.clauses.JmlContract
import com.google.common.cache.CacheBuilder
import io.github.jmltoolkit.lsp.JmlLanguageServer
import io.github.jmltoolkit.lsp.asLeft
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.MessageType
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture

class VerifyAgainstParent : LspAction {
    override val id: String = "jml.verify.liskov"
    override val title: String = "Verify against parent"

    private val cache = CacheBuilder.newBuilder().softValues().build<Int, JmlContract>()

    override fun execute(server: JmlLanguageServer, value: List<Any>?): CompletableFuture<Any> {
        if (value == null) return CompletableFuture.completedFuture(null)
        cache.getIfPresent(value.first())?.let {
            server.client.showMessage(
                MessageParams(MessageType.Warning, "Prove is not implemented yet.")
            )
        }
        return CompletableFuture.completedFuture("")
    }

    override fun isCallableForNode(node: Node): Boolean = node is JmlContract

    override fun createCodeAction(
        uri: String,
        node: Node
    ): Either<Command, CodeAction>? =
        if (node is JmlContract) {
            // Remember the contract instance under the hashCode passed as command
            // argument, so execute() can find it again and report on it.
            cache.put(node.hashCode(), node)
            command(listOf(node.hashCode())).asLeft()
        } else {
            null
        }
}

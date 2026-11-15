/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.jml.body.JmlClassExprDeclaration
import com.github.javaparser.ast.jml.clauses.JmlSignalsClause
import com.github.javaparser.ast.jml.clauses.JmlSimpleExprClause
import com.github.javaparser.ast.jml.stmt.JmlExpressionStmt
import io.github.jmltoolkit.lsp.JmlLanguageServer
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.MessageType
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture

class WellDefinednessCheck : LspAction {
    override val id: String
        get() = "jml.welldefinedness-check"
    override val title: String
        get() = "Check expression for well-definedness"

    override fun isCallableForNode(node: Node) = when (node) {
        is JmlSignalsClause -> true
        is JmlClassExprDeclaration -> true
        is JmlSimpleExprClause -> true
        is JmlExpressionStmt -> true
        else -> false
    }

    override fun execute(server: JmlLanguageServer, value: List<Any>?): CompletableFuture<Any> {
        if (value.isNullOrEmpty()) {
            error("No parameters specified")
        }
        val uri: String = GsonHelper.unwrap(value[0])
        val range: Range = GsonHelper.unwrap(value[1])

        return CompletableFuture.supplyAsync {
            execute(server, uri, range)
        }
    }

    fun execute(server: JmlLanguageServer, uri: String, range: Range) {
        server.client.showMessage(
            MessageParams(MessageType.Info, "The well-definedness check is not yet implemented.")
        )
    }

    /**
     * The well-definedness analysis is not implemented yet, so the action is not
     * advertised. Re-enable once [execute] performs an actual analysis instead of
     * reporting the missing implementation.
     */
    override fun createCodeAction(uri: String, node: Node): Either<Command, CodeAction>? = null
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.jml.clauses.JmlBehaviorKind
import com.github.javaparser.ast.jml.clauses.JmlContract
import io.github.jmltoolkit.lsp.JmlLanguageServer
import io.github.jmltoolkit.lsp.asLeft
import io.github.jmltoolkit.lsp.asPosition
import io.github.jmltoolkit.lsp.asRight
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

/**
 *
 * @author Alexander Weigl
 * @version 1 (20.09.26)
 */
class AddBehaviorKeyword : LspAction {
    override val title: String = "Add a behavior keyword"

    override fun execute(server: JmlLanguageServer, value: List<Any>?): CompletableFuture<Any> {
        TODO("Not yet implemented")
    }

    override fun isCallableForNode(node: Node): Boolean = (node is JmlContract)

    override fun createCodeAction(uri: String, node: Node): Either<Command, CodeAction>? {
        if (isCallableForNode(node) && (node as? JmlContract)?.behavior() != null) {
            return CodeAction(title)
                .also {
                    it.kind = kind
                    it.edit = WorkspaceEdit(
                        listOf(
                            TextDocumentEdit(
                                VersionedTextDocumentIdentifier(uri, -1),
                                listOf(createEdit(node).asRight())
                            ).asLeft()
                        )
                    )
                }
                .asRight()
        }
        return null
    }

    private fun createEdit(node: JmlContract): SnippetTextEdit {
        val afterNode = listOfNotNull(
            node.begin.getOrNull(),
            node.modifiers().lastOrNull()?.end?.getOrNull(),
        ).lastOrNull()!!.asPosition

        return SnippetTextEdit(
            Range(afterNode, afterNode),
                StringValue(
                StringValueKind.SNIPPET,
                " \${1:${JmlBehaviorKind.entries.joinToString("|") { it.jmlSymbol() }}: "
            )
        )
    }
}

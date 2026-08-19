/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.jml.body.JmlClassExprDeclaration
import com.github.javaparser.ast.jml.body.JmlClassLevelDeclaration
import com.github.javaparser.ast.jml.clauses.JmlClause
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
class AddNameToClauseOrContract : LspAction {
    override val title: String = "Add a name to clause or contract"

    override fun execute(server: JmlLanguageServer, value: List<Any>?): CompletableFuture<Any> {
        TODO("Not yet implemented")
    }

    override fun isCallableForNode(node: Node): Boolean =
        (node is JmlContract || node is JmlClause || node is JmlClassLevelDeclaration<*>)

    fun hasName(node: Node): Boolean =
        when (node) {
            is JmlContract -> node.name.isPresent
            is JmlClause -> node.name.isPresent
            is JmlClassExprDeclaration -> node.name.isPresent
            else -> false
        }

    override fun createCodeAction(uri: String, node: Node): Either<Command, CodeAction>? {
        if (isCallableForNode(node) && !hasName(node)) {
            return CodeAction(title)
                .also {
                    it.kind = kind
                    val edit = TextDocumentEdit(
                        VersionedTextDocumentIdentifier(uri, -1),
                        listOf(createEdit(node).asRight())
                    )
                    it.edit = WorkspaceEdit(listOf(edit.asLeft()))
                }
                .asRight()
        }
        return null
    }

    private fun createEdit(node: Node): SnippetTextEdit {
        val afterNode: Position? = when (node) {
            is JmlContract ->
                listOfNotNull(
                    node.begin.getOrNull(),
                    node.modifiers().lastOrNull()?.end?.getOrNull(),
                    node.behavior()?.end?.getOrNull()
                ).lastOrNull()

            is JmlClause -> node.kind.end.getOrNull()

            is JmlClassExprDeclaration -> node.kind.end.getOrNull()

            else -> error("Unexpected node: $node")
        }?.asPosition

        return SnippetTextEdit(
            Range(afterNode, afterNode),
            StringValue(
            StringValueKind.SNIPPET,
            " \${1:name}: "
        )
        )
    }
}

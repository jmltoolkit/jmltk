/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import io.github.jmltoolkit.lsp.JmlLanguageServer
import io.github.jmltoolkit.lsp.asLeft
import io.github.jmltoolkit.lsp.asPosition
import io.github.jmltoolkit.lsp.asRight
import io.github.jmltoolkit.utils.JMLUtils.findTopMostAncestorConsecutive
import io.github.jmltoolkit.utils.JMLUtils.isInJML
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

/**
 *
 * @author Alexander Weigl
 * @version 1 (20.09.26)
 */
class AddQuantification : LspAction {
    override val title: String = "Add Quantification"

    override fun execute(server: JmlLanguageServer, value: List<Any>?): CompletableFuture<Any> {
        TODO("Not yet implemented")
    }

    override fun createCodeAction(uri: String, node: Node): Either<Command, CodeAction>? {
        if (node is NameExpr && node.isInJML()) {
            return CodeAction(title)
                .also {
                    it.kind = kind
                    it.edit = createEdit(uri, node)
                }
                .asRight()
        }
        return null
    }

    private fun createEdit(uri: String, node: NameExpr): WorkspaceEdit {
        val range = node.findTopMostAncestorConsecutive<Expression>()
        val startRange = range?.begin?.getOrNull()?.asPosition?.let { Range(it, it) } ?: error("Could not find range")
        val endRange = range?.end?.getOrNull()?.asPosition?.let { Range(it, it) } ?: error("Could not find range")

        val v = node.toString()
        val type = node.calculateResolvedType()

        val x = TextDocumentEdit(
            VersionedTextDocumentIdentifier(uri, -1),
            listOf(
                SnippetTextEdit(startRange, StringValue(StringValueKind.SNIPPET, "(\\forall $type $v;")).asRight(),
                SnippetTextEdit(endRange, StringValue(StringValueKind.SNIPPET, ")")).asRight()
            )
        )
        return WorkspaceEdit(listOf(x.asLeft()))
    }
}

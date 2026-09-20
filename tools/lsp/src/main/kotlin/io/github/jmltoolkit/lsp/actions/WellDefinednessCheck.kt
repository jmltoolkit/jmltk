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
import io.github.jmltoolkit.lsp.asRange
import io.github.jmltoolkit.lsp.asRight
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture

class WellDefinednessCheck : LspAction {
    override val id: String
        get() = "jml.welldefinedness-check"
    override val title: String
        get() = "Check expression for well-definedness"

    override fun isCallableForNode(node: Node) = when(node) {
        JmlSignalsClause::class.java -> true
        JmlClassExprDeclaration::class.java -> true
        JmlSimpleExprClause::class.java -> true
        JmlExpressionStmt::class.java -> true
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
        TODO("Not yet implemented")
    }

    override fun createCodeAction(uri: String, node: Node): Either<Command, CodeAction> = CodeAction(title).also {
        it.command = command(listOf(uri, node.range.get().asRange))
        it.kind = CodeActionKind.QuickFix
    }.asRight()
}

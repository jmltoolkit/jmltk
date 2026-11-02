/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr.JmlDefaultBinder
import io.github.jmltoolkit.lsp.JmlLanguageServer
import io.github.jmltoolkit.lsp.asRight
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture

/**
 * Offers an "Add boundary" quick-fix on classic `\forall`/`\exists`
 * quantifiers (`(\forall int i; a[i] > 0)`), i.e. quantifiers whose binding
 * carries a declared integer range instead of an element/range domain.
 *
 * Port of the `JmlQuantifiedExpr` handling of the former class-based
 * [io.github.jmltoolkit.lsp.CodeActionCollector] onto the action-table design.
 * The action is advertised but performs no edit yet.
 *
 * @author Alexander Weigl
 */
class AddBoundary : LspAction {
    override val title: String = "Add boundary"

    override fun execute(server: JmlLanguageServer, value: List<Any>?): CompletableFuture<Any> {
        TODO("Not yet implemented")
    }

    // Callability is a pure function of the node class here: the collector
    // caches per-class which actions are callable (CodeActionCollector#actionTable).
    override fun isCallableForNode(node: Node): Boolean = node is JmlQuantifiedExpr

    override fun createCodeAction(uri: String, node: Node): Either<Command, CodeAction>? {
        val quantified = node as? JmlQuantifiedExpr ?: return null
        if (quantified.binder != JmlDefaultBinder.FORALL && quantified.binder != JmlDefaultBinder.EXISTS) {
            return null
        }
        return CodeAction(title).asRight()
    }
}

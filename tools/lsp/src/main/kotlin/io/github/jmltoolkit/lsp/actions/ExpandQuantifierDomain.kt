/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr
import com.github.javaparser.jml.JmlQuantifierDesugarer
import io.github.jmltoolkit.lsp.JmlLanguageServer
import io.github.jmltoolkit.lsp.asRight
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

/**
 * Quick-fix for element/range-domain quantifiers (core PR #83):
 * `(\forall int e : a; e > 0)` rewrites to the classic index form
 * `(\forall int _i; 0 <= _i && _i < a.length; a[_i] > 0)` via
 * [JmlQuantifierDesugarer]. Kinds 2 (range) and 3 (array) desugar to the
 * classic form; kind 4 (`Iterable`) and unresolvable domains degrade to no
 * action.
 *
 * Port of the `JmlQuantifiedExpr` handling of the former class-based
 * [io.github.jmltoolkit.lsp.CodeActionCollector] (`Expand quantifier domain:
 * <desugared>` in main) onto the action-table design. The action is advertised
 * but performs no edit yet.
 *
 * @author Alexander Weigl
 */
class ExpandQuantifierDomain : LspAction {
    override val title: String = "Expand quantifier domain"

    override fun execute(server: JmlLanguageServer, value: List<Any>?): CompletableFuture<Any> {
        TODO("Not yet implemented")
    }

    // Callability is a pure function of the node class here: the collector
    // caches per-class which actions are callable (CodeActionCollector#actionTable),
    // while whether this particular quantifier *desugars* is decided per node
    // in createCodeAction.
    override fun isCallableForNode(node: Node): Boolean = node is JmlQuantifiedExpr

    override fun createCodeAction(uri: String, node: Node): Either<Command, CodeAction>? {
        val quantified = node as? JmlQuantifiedExpr ?: return null
        if (!JmlQuantifierDesugarer.isElementQuantifier(quantified)) {
            return null
        }
        return JmlQuantifierDesugarer.desugar(quantified).getOrNull()?.let { desugared ->
            CodeAction("Expand quantifier domain: $desugared").asRight()
        }
    }
}

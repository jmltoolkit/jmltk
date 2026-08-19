/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import io.github.jmltoolkit.lsp.JmlLanguageServer
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeLens
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture

/**
 * This class encapsulates both the execution of an action on the ast, and the metadata.
 * @author Alexander Weigl
 */
interface LspAction {
    val id: String
        get() = this::class.java.name
    val title: String

    val kind: String?
        get() = null

    fun isCallableForNode(node: Node): Boolean = true
    fun command(args: List<Any>? = null): Command = Command(title, id, args)
    fun createCodeLens(uri: String, node: Node): CodeLens? = null
    fun execute(server: JmlLanguageServer, value: List<Any>?): CompletableFuture<Any>
    fun createCodeAction(uri: String, node: Node): Either<Command, CodeAction>? = null
}

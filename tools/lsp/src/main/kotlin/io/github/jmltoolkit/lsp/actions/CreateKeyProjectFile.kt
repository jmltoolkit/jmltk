/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import io.github.jmltoolkit.lsp.JmlLanguageServer
import io.github.jmltoolkit.lsp.asLeft
import io.github.jmltoolkit.lsp.asRight
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture
import kotlin.io.path.exists
import kotlin.io.path.writeText

class CreateKeyProjectFile : LspAction {
    override val title: String = "Create KeY project file"
    override fun execute(server: JmlLanguageServer, value: List<Any>?): CompletableFuture<Any> {
        val root = server.rootFolder
        val keyFile = root.resolve("project.key")
        if (keyFile.exists()) {
            return CompletableFuture.completedFuture("Project key already exists")
        }

        keyFile.writeText(
            """
            \javaSrc "./src";
            \chooseContract
        """.trimIndent()
        )

        return CompletableFuture.completedFuture("")
    }

    override fun createCodeAction(uri: String, node: Node) = null

    private fun command(rootUri: String): WorkspaceEdit {
        val uri = "${rootUri}/project.key"
        val p = Position(0, 0)
        val range = Range(p, p)
        val text = """
            \javaSrc "./src";
            \chooseContract
        """.trimIndent()

        return WorkspaceEdit(
            listOf(
                CreateFile(uri, CreateFileOptions(false, true)).asRight(),
                TextDocumentEdit(
                    VersionedTextDocumentIdentifier(uri, 0),
                    listOf(TextEdit(range, text).asLeft())
                ).asLeft()
            )
        )
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import io.github.jmltoolkit.lsp.JmlLanguageServer
import java.util.concurrent.CompletableFuture
import kotlin.io.path.exists
import kotlin.io.path.writeText

class CreateKeyProjectFile : LspAction<Node> {
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
}

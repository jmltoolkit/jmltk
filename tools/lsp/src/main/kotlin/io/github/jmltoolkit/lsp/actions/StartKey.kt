/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import de.uka.ilkd.key.gui.MainWindow
import io.github.jmltoolkit.lsp.JmlLanguageServer
import java.util.concurrent.CompletableFuture

/**
 *
 * @author Alexander Weigl
 * @version 1 (18.08.26)
 */
class StartKey : LspAction<Node> {
    override val title: String = "Start Key"

    var mainWindow: MainWindow? = null

    override fun execute(server: JmlLanguageServer, value: List<Any>?): CompletableFuture<Any> {
        val mainWindow = mainWindow?.let { MainWindow.getInstance(true) }!!
        mainWindow.isVisible = true
        mainWindow.requestFocus()

        this.mainWindow = mainWindow

        /*if (value != null) {
            val contract = value[0] as HoverParams // document + pos
            val path = Uri(contract.textDocument.uri).path
            val env = KeYEnvironment.load(path)
            env.proofContracts.find {
                // file name + position must fit!
                false
            }
        }*/

        return CompletableFuture.completedFuture(null)
    }
}

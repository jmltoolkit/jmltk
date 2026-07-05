/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import org.eclipse.lsp4j.DidChangeNotebookDocumentParams
import org.eclipse.lsp4j.DidCloseNotebookDocumentParams
import org.eclipse.lsp4j.DidOpenNotebookDocumentParams
import org.eclipse.lsp4j.DidSaveNotebookDocumentParams
import org.eclipse.lsp4j.services.NotebookDocumentService

class JmlNotebookDocumentServices : NotebookDocumentService {
    override fun didOpen(params: DidOpenNotebookDocumentParams?) {}

    override fun didChange(params: DidChangeNotebookDocumentParams?) {}

    override fun didSave(params: DidSaveNotebookDocumentParams?) {}

    override fun didClose(params: DidCloseNotebookDocumentParams?) {}
}

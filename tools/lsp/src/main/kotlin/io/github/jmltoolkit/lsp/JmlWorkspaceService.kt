/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.WorkspaceService
import org.tinylog.kotlin.Logger
import java.util.concurrent.CompletableFuture

class JmlWorkspaceService(val jmlLanguageServer: JmlLanguageServer) : WorkspaceService {
    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        Logger.info("didChangeConfiguration {}", params)
    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        Logger.info("didChangeWatchedFiles {}", params)
    }

    override fun executeCommand(params: ExecuteCommandParams): CompletableFuture<Any> {
        Logger.info("executeCommand {}", params)
        return jmlLanguageServer.actions.find { it.id == params.command }
            ?.execute(jmlLanguageServer, params.arguments)
            ?: CompletableFuture.completedFuture("")
    }

    override fun symbol(params: WorkspaceSymbolParams?): CompletableFuture<Either<MutableList<out SymbolInformation>, MutableList<out WorkspaceSymbol>>> = super.symbol(params)

    override fun resolveWorkspaceSymbol(workspaceSymbol: WorkspaceSymbol?): CompletableFuture<WorkspaceSymbol> = super.resolveWorkspaceSymbol(workspaceSymbol)

    override fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams?) {
        super.didChangeWorkspaceFolders(params)
    }

    override fun willCreateFiles(params: CreateFilesParams?): CompletableFuture<WorkspaceEdit> = super.willCreateFiles(params)

    override fun didCreateFiles(params: CreateFilesParams?) {
        super.didCreateFiles(params)
    }

    override fun willRenameFiles(params: RenameFilesParams?): CompletableFuture<WorkspaceEdit> = super.willRenameFiles(params)

    override fun didRenameFiles(params: RenameFilesParams?) {
        super.didRenameFiles(params)
    }

    override fun willDeleteFiles(params: DeleteFilesParams?): CompletableFuture<WorkspaceEdit> = super.willDeleteFiles(params)

    override fun didDeleteFiles(params: DeleteFilesParams?) {
        super.didDeleteFiles(params)
    }

    override fun diagnostic(params: WorkspaceDiagnosticParams?): CompletableFuture<WorkspaceDiagnosticReport> = super.diagnostic(params)
}

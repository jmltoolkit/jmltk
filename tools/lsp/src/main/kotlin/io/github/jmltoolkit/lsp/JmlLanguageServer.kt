/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import io.github.jmltoolkit.lsp.actions.LspAction
import io.github.jmltoolkit.lsp.highlighting.LEGEND
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.*
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * Cancellation token for an in-flight server-initiated task (e.g. a VCG verification).
 * The client references it through the `window/workDoneProgress` token; when the client
 * sends `window/workDoneProgress/cancel` the LSP server flips the flag and the running
 * solver process is torn down.
 */
internal class VerificationHandle(val token: String) {
    private val flag = AtomicBoolean(false)
    fun cancel() {
        flag.set(true)
    }

    fun isCancelled(): Boolean = flag.get()
}

class JmlLanguageServer :
    LanguageServer,
    LanguageClientAware {
    internal val executorService: ExecutorService = ForkJoinPool.commonPool()
    internal val jmlTextDocumentService by lazy { JmlTextDocumentService(this) }
    internal val jmlWorkspaceService by lazy { JmlWorkspaceService(this) }

    internal lateinit var client: LanguageClient
    internal lateinit var rootFolder: Path
    internal lateinit var workspaceFolders: List<WorkspaceFolder>
    internal lateinit var capabilities: ClientCapabilities

    internal val jmlNotebookDocumentServices by lazy { JmlNotebookDocumentServices() }

    internal val config = ProjectDefinitionService()

    internal val actions by lazy {
        ServiceLoader.load(LspAction::class.java).toList()
    }

    /** Cancellation handles of in-flight verifications, keyed by progress token. */
    private val runningVerifications = ConcurrentHashMap<String, VerificationHandle>()

    /** Starts a new cancellable verification session; call [endVerification] when done. */
    internal fun beginVerification(): VerificationHandle {
        val handle = VerificationHandle("jml-vcg-" + UUID.randomUUID())
        runningVerifications[handle.token] = handle
        return handle
    }

    internal fun endVerification(handle: VerificationHandle) {
        runningVerifications.remove(handle.token)
    }

    /** Client-initiated cancellation of a `window/workDoneProgress` token (LSP spec). */
    override fun cancelProgress(params: WorkDoneProgressCancelParams) {
        val token = params.token
        val key = if (token.isLeft) token.left else token.right?.toString() ?: return
        runningVerifications[key]?.cancel()
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        rootFolder = params.rootUri?.let { Uri(it) }?.path
            ?: Paths.get(".").toAbsolutePath() // no better clue what to-do

        workspaceFolders = params.workspaceFolders ?: emptyList()
        capabilities = params.capabilities

        config.update(workspaceFolders.map { Uri(it.uri) })

        return CompletableFuture.supplyAsync {
            val capabilities = ServerCapabilities()
            capabilities.setTextDocumentSync(TextDocumentSyncKind.Full)
            capabilities.diagnosticProvider = DiagnosticRegistrationOptions(true, false)
            capabilities.setDocumentSymbolProvider(true)
            capabilities.setWorkspaceSymbolProvider(true)

            capabilities.setDeclarationProvider(DeclarationRegistrationOptions("JML"))

            // capabilities.signatureHelpProvider = SignatureHelpOptions()
            capabilities.setHoverProvider(true)

            // capabilities.setDocumentFormattingProvider(true)
            capabilities.foldingRangeProvider = null

            // Code lenses ("Verify method (VCG)", ...); resolveProvider=false since
            // resolveCodeLens is not implemented.
            capabilities.codeLensProvider = CodeLensOptions(false)
            capabilities.setSelectionRangeProvider(true)

            // capabilities.setDefinitionProvider(true)
            // capabilities.setDocumentHighlightProvider(true)
            // capabilities.completionProvider = CompletionOptions(true, null)

            capabilities.semanticTokensProvider = SemanticTokensWithRegistrationOptions(
                LEGEND, SemanticTokensServerFull(false), false,
                listOf(
                    DocumentFilter("java", "file", Either.forLeft("*.java")),
                    DocumentFilter("key", "file", Either.forLeft("*.key")),
                )
            )

            capabilities.setCodeActionProvider(CodeActionOptions(listOf("validity", "key")))
            capabilities.executeCommandProvider = ExecuteCommandOptions(actions.map { it.id })

            return@supplyAsync InitializeResult(capabilities)
        }
    }

    override fun shutdown(): CompletableFuture<Any> {
        executorService.shutdown()
        val c = executorService.awaitTermination(5, TimeUnit.SECONDS)
        val i = executorService.shutdownNow()
        return CompletableFuture.completedFuture("Finish: Waited 5 seconds. $c, ${i.size} jobs killed.")
    }

    override fun exit() {
        shutdown()
        exitProcess(0)
    }

    override fun getNotebookDocumentService(): NotebookDocumentService = jmlNotebookDocumentServices

    override fun getTextDocumentService(): TextDocumentService = jmlTextDocumentService

    override fun getWorkspaceService(): WorkspaceService = jmlWorkspaceService

    override fun connect(client: LanguageClient) {
        this.client = client
    }
}

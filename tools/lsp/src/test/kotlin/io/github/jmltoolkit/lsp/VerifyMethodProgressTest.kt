/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import com.github.javaparser.ast.body.MethodDeclaration
import io.github.jmltoolkit.lsp.actions.VerifyMethod
import io.github.jmltoolkit.smt.Z3
import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.ProgressParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.WorkDoneProgressBegin
import org.eclipse.lsp4j.WorkDoneProgressCancelParams
import org.eclipse.lsp4j.WorkDoneProgressCreateParams
import org.eclipse.lsp4j.WorkDoneProgressEnd
import org.eclipse.lsp4j.WorkDoneProgressReport
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the `window/workDoneProgress`-based progress and cancellation support of
 * the VCG verification action: the server must announce begin/report/end notifications,
 * and a `window/workDoneProgress/cancel` must flip the matching [VerificationHandle].
 */
@Timeout(180)
class VerifyMethodProgressTest : TestUtilities() {

    /** Records server-initiated progress traffic; always accepts the progress token. */
    class RecordingClient : LanguageClient {
        val createProgressTokens = CopyOnWriteArrayList<String>()
        val progressNotifications = CopyOnWriteArrayList<ProgressParams>()

        override fun createProgress(params: WorkDoneProgressCreateParams?): CompletableFuture<Void> {
            if (params != null && params.token.isLeft) {
                createProgressTokens.add(params.token.left)
            }
            return CompletableFuture.completedFuture(null)
        }

        override fun notifyProgress(params: ProgressParams?) {
            if (params != null) progressNotifications.add(params)
        }

        override fun telemetryEvent(`object`: Any?) {}
        override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams?) {}
        override fun showMessage(messageParams: MessageParams?) {}
        override fun showMessageRequest(requestParams: org.eclipse.lsp4j.ShowMessageRequestParams?): CompletableFuture<MessageActionItem> =
            CompletableFuture.completedFuture(MessageActionItem("Test!"))

        override fun logMessage(message: MessageParams?) {}
    }

    private fun jmlMethod(name: String): MethodDeclaration {
        val cu = languageServer.jmlTextDocumentService.repo
            .createJavaParser().parse(File(workspace, "Stack.java"))
            .result.orElseThrow { IllegalStateException("workspace/Stack.java does not parse") }
        return cu.findAll(MethodDeclaration::class.java).first { it.nameAsString == name }
    }

    @Test
    fun verificationReportsWorkDoneProgress() {
        Assumptions.assumeTrue(Z3.z3Installed(), "z3 required for the VCG")
        val client = RecordingClient()
        languageServer.client = client

        val action = VerifyMethod()
        val lens = action.createCodeLens(jmlMethod("push"))!!
        action.execute(languageServer, lens.command.arguments).get()

        assertEquals(1, client.createProgressTokens.size, "exactly one progress session is created")
        val token = client.createProgressTokens.single()
        val values = client.progressNotifications
        assertTrue(values.isNotEmpty(), "progress notifications must be sent")
        assertTrue(values.all { it.token.isLeft && it.token.left == token }, "all notifications share the session token")
        assertEquals(1, values.count { it.value.isLeft && it.value.left is WorkDoneProgressBegin })
        assertEquals(1, values.count { it.value.isLeft && it.value.left is WorkDoneProgressEnd })
        // begin and end book-end any intermediate reports
        assertTrue(values.first().value.isLeft && values.first().value.left is WorkDoneProgressBegin)
        assertTrue(values.last().value.isLeft && values.last().value.left is WorkDoneProgressEnd)
        val reports = values.mapNotNull { (it.value as? Either<org.eclipse.lsp4j.WorkDoneProgressNotification, Any>)?.left }
            .filterIsInstance<WorkDoneProgressReport>()
        assertTrue(reports.all { it.percentage != null && it.percentage!! >= 0 && it.percentage <= 100 })
    }

    @Test
    fun cancelProgressFlagsTheMatchingHandle() {
        val handle = languageServer.beginVerification()
        try {
            languageServer.cancelProgress(WorkDoneProgressCancelParams(Either.forLeft(handle.token)))
            assertTrue(handle.isCancelled(), "the verification handle must be flagged as cancelled")
        } finally {
            languageServer.endVerification(handle)
        }
        // cancelling unknown progress is a no-op and must not throw
        languageServer.cancelProgress(WorkDoneProgressCancelParams(Either.forLeft("unknown-token")))
        languageServer.cancelProgress(WorkDoneProgressCancelParams(Either.forRight(42)))
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.google.common.cache.CacheBuilder
import io.github.jmltoolkit.lsp.JmlLanguageServer
import io.github.jmltoolkit.lsp.VerificationHandle
import io.github.jmltoolkit.lsp.asRange
import io.github.jmltoolkit.vcg.CallStrategy
import io.github.jmltoolkit.vcg.Vcg
import io.github.jmltoolkit.vcg.VcgContext
import io.github.jmltoolkit.vcg.VcgOptions
import io.github.jmltoolkit.vcg.VcgResult
import io.github.jmltoolkit.vcg.VerificationMode
import org.eclipse.lsp4j.CodeLens
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.MessageType
import org.eclipse.lsp4j.ProgressParams
import org.eclipse.lsp4j.WorkDoneProgressBegin
import org.eclipse.lsp4j.WorkDoneProgressCreateParams
import org.eclipse.lsp4j.WorkDoneProgressEnd
import org.eclipse.lsp4j.WorkDoneProgressNotification
import org.eclipse.lsp4j.WorkDoneProgressReport
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs the verification condition generator over a JML-annotated method and reports
 * the per-condition results (PROVEN / FAILED / UNKNOWN) to the client.
 *
 * The run is surfaced as a server-initiated `window/workDoneProgress` session: the
 * client receives begin/report/end notifications while the SMT solver is running and
 * can cancel the verification (via `window/workDoneProgress/cancel`), which tears down
 * the solver process via the server's [VerificationHandle] registry.
 *
 * @author Alexander Weigl
 * @version 1 (26.09.26)
 */
class VerifyMethod : LspAction<MethodDeclaration> {
    override val id: String = "jml.verify.vcg"
    override val title: String = "Verify method (VCG)"

    /** Hard deadline for one solver run; a stuck solver can never pin the LSP forever. */
    private val solverTimeoutMillis = 60_000L

    private val cache = CacheBuilder.newBuilder().softValues().build<Int, CallableDeclaration<*>>()

    override fun execute(server: JmlLanguageServer, value: List<Any>?): CompletableFuture<Any> {
        val key = value?.firstOrNull() as? Int
        val callable = key?.let { cache.getIfPresent(it) }
        if (callable == null) {
            server.client.showMessage(
                MessageParams(MessageType.Error, "Method not available for verification (already collected).")
            )
            return CompletableFuture.completedFuture(null)
        }
        return CompletableFuture.supplyAsync {
            val handle = server.beginVerification()
            // Progress notifications are only sent once the client confirmed the token.
            val progress = AtomicBoolean(false)
            val report: (WorkDoneProgressNotification) -> Unit = { value ->
                server.client.notifyProgress(
                    ProgressParams(
                        Either.forLeft(handle.token),
                        Either.forLeft<WorkDoneProgressNotification, Any>(value)
                    )
                )
            }
            server.client.createProgress(WorkDoneProgressCreateParams(Either.forLeft(handle.token)))
                .thenAccept {
                    progress.set(true)
                    report(
                        WorkDoneProgressBegin().apply {
                            title = "Verifying ${callable.nameAsString}"
                            message = "Generating verification conditions"
                            cancellable = true
                            percentage = 0
                        }
                    )
                }
                .exceptionally { null } // client has no progress support: run without it
            try {
                val result = verify(callable, server)
                val status = result.checkProgressive(
                    onCondition = { index, total, _ ->
                        if (progress.get()) {
                            report(
                                WorkDoneProgressReport().apply {
                                    percentage = if (total == 0) 100 else (index * 100) / total
                                    message = "Checking condition $index of $total"
                                }
                            )
                        }
                    },
                    isCancelled = { handle.isCancelled() },
                    timeoutMillis = solverTimeoutMillis,
                )
                val proven = status.values.count { it == VcgResult.Status.PROVEN }
                val failed = status.values.count { it == VcgResult.Status.FAILED }
                val unknown = status.values.count { it == VcgResult.Status.UNKNOWN }
                val msg = if (handle.isCancelled()) {
                    "Verification cancelled: ${callable.nameAsString}."
                } else {
                    String.format(
                        "Verification: %d proven, %d failed, %d unknown (%s).",
                        proven, failed, unknown, callable.nameAsString
                    )
                }
                val failedDesc = result.conditions.filter { status[it.id] == VcgResult.Status.FAILED }
                    .joinToString("; ") { "${it.id} ${it.description}" }
                server.client.showMessage(
                    MessageParams(
                        if (failed == 0) MessageType.Info else MessageType.Warning,
                        msg + if (failedDesc.isNotEmpty()) " -> $failedDesc" else ""
                    )
                )
            } catch (e: Exception) {
                server.client.showMessage(
                    MessageParams(MessageType.Error, "Verification failed: ${e.javaClass.simpleName}: ${e.message}")
                )
            } finally {
                if (progress.get()) {
                    report(
                        WorkDoneProgressEnd().apply {
                            message = if (handle.isCancelled()) "Verification cancelled." else "Verification finished."
                        }
                    )
                }
                server.endVerification(handle)
            }
            null
        }
    }

    private fun verify(callable: CallableDeclaration<*>, server: JmlLanguageServer): VcgResult {
        val options = VcgOptions(
            mode = VerificationMode.UNBOUNDED,
            defaultCallStrategy = CallStrategy.CONTRACT,
        )
        return Vcg(VcgContext.of(callable), options).verify()
    }

    override fun createCodeLens(node: MethodDeclaration): CodeLens? {
        if (node.contracts.isEmpty()) return null
        cache.put(node.hashCode(), node)
        return CodeLens(node.asRange, command(listOf(node.hashCode())), null)
    }
}

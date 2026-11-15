/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.jmltoolkit.lsp.JmlLanguageServer
import org.eclipse.lsp4j.CodeLens
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.MessageType
import org.eclipse.lsp4j.jsonrpc.json.adapters.CollectionTypeAdapter
import org.eclipse.lsp4j.jsonrpc.json.adapters.EitherTypeAdapter
import org.eclipse.lsp4j.jsonrpc.json.adapters.EnumTypeAdapter
import org.eclipse.lsp4j.jsonrpc.json.adapters.ThrowableTypeAdapter
import org.eclipse.lsp4j.jsonrpc.json.adapters.TupleTypeAdapters.TwoTypeAdapterFactory
import java.util.concurrent.CompletableFuture

/**
 *
 * @author Alexander Weigl
 * @version 1 (17.09.26)
 */
class AddStubs : LspAction {
    override val title: String = "Add stub contracts to type if missing"

    /**
     * Stub generation is not implemented yet, so no code lens is offered.
     * Re-enable once [execute] actually generates the stub contracts.
     */
    override fun createCodeLens(uri: String, node: Node): CodeLens? = null

    override fun execute(
        server: JmlLanguageServer,
        value: List<Any>?
    ): CompletableFuture<Any> {
        if (value == null) {
            error("This command expects the uri and a range as parameter")
        }
        server.client.showMessage(
            MessageParams(MessageType.Info, "Adding stub contracts is not yet implemented.")
        )
        return CompletableFuture.completedFuture("")
    }
}

object GsonHelper {
    private val gson by lazy { getDefaultGsonBuilder().create() }
    fun getDefaultGsonBuilder(): GsonBuilder {
        return GsonBuilder()
            .registerTypeAdapterFactory(CollectionTypeAdapter.Factory())
            .registerTypeAdapterFactory(ThrowableTypeAdapter.Factory())
            .registerTypeAdapterFactory(EitherTypeAdapter.Factory())
            .registerTypeAdapterFactory(TwoTypeAdapterFactory())
            .registerTypeAdapterFactory(EnumTypeAdapter.Factory())
        // .registerTypeAdapterFactory(MessageTypeAdapter.Factory(this))
    }

    inline fun <reified T> unwrap(x: Any): T = unwrap(x, T::class.java)
    fun <T> unwrap(x: Any, java: Class<T>) = gson.fromJson(Gson().toJson(x), java) as T
}

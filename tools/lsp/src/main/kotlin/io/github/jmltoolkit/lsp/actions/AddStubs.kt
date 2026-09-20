package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.LambdaExpr
import com.github.javaparser.ast.nodeTypes.NodeWithName
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.jmltoolkit.lsp.JmlLanguageServer
import io.github.jmltoolkit.lsp.Uri
import io.github.jmltoolkit.lsp.asRange
import io.github.jmltoolkit.utils.Helper.findAll
import org.eclipse.lsp4j.CodeLens
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.json.adapters.CollectionTypeAdapter
import org.eclipse.lsp4j.jsonrpc.json.adapters.EitherTypeAdapter
import org.eclipse.lsp4j.jsonrpc.json.adapters.EnumTypeAdapter
import org.eclipse.lsp4j.jsonrpc.json.adapters.ThrowableTypeAdapter
import org.eclipse.lsp4j.jsonrpc.json.adapters.TupleTypeAdapters.TwoTypeAdapterFactory
import java.nio.charset.CharsetDecoder
import java.util.concurrent.CompletableFuture


/**
 * 
 * @author Alexander Weigl 
 * @version 1 (17.09.26)
 */
class AddStubs : LspAction {
    override val title: String = "Add stub contracts to type if missing"

    fun command(uri: String, target: NodeWithName<*>): Command {
        return super.command(listOf(uri, target.name.range.asRange()))
    }

    override fun createCodeLens(uri: String, node: Node) =
        if (node is TypeDeclaration<*>)
            CodeLens(node.name().asRange, command(uri, node as NodeWithName<*>), null)
        else null

    override fun execute(
        server: JmlLanguageServer,
        value: List<Any>?
    ): CompletableFuture<Any> {
        val (a, b) = value ?: error("This command expect the uri and a range as parameter")

        val uri: String = GsonHelper.unwrap(a)
        val nameRange: Range = GsonHelper.unwrap(b)

        val node = server.jmlTextDocumentService.repo[Uri(uri)]
            .get()
            .findAll<Node> { it is NodeWithName<*> && it.name.range == nameRange }
            .firstOrNull()
            ?: error("This node could not be found.")

        when(node) {
            is TypeDeclaration<*> -> {

            }

            is CallableDeclaration<*> -> {

            }

            is LambdaExpr -> {

            }
        }
        return TODO("Provide the return value")
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
        //.registerTypeAdapterFactory(MessageTypeAdapter.Factory(this))
    }

    inline fun <reified T> unwrap(x: Any): T = unwrap(x, T::class.java)
    fun <T> unwrap(x: Any, java: Class<T>) = gson.fromJson(Gson().toJson(x), java) as T
}
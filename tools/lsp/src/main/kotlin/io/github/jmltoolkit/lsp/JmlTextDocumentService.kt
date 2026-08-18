/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import com.github.javaparser.JavaParser
import com.github.javaparser.ParseResult
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Jmlish
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.resolution.TypeSolver
import com.github.javaparser.resolution.declarations.AssociableToAST
import com.github.javaparser.resolution.declarations.ResolvedDeclaration
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.google.common.hash.Hashing.crc32
import io.github.jmltoolkit.lint.JmlLintingConfig
import io.github.jmltoolkit.lint.JmlLintingFacade
import io.github.jmltoolkit.lsp.actions.CreateKeyProjectFile
import io.github.jmltoolkit.lsp.actions.StartKey
import io.github.jmltoolkit.lsp.highlighting.JmlDocumentHighlighter
import io.github.jmltoolkit.lsp.highlighting.KeyDocumentHighlighter
import io.github.jmltoolkit.lsp.hover.JmlDocumentationIndex
import io.github.jmltoolkit.lsp.symbols.JmlCatchSymbols
import io.github.jmltoolkit.lsp.symbols.KeyCatchSymbols
import jdk.jshell.UnresolvedReferenceException
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import org.tinylog.kotlin.Logger
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.readText
import com.github.javaparser.Position as JPosition

private val Node?.parentalSelectionRange: SelectionRange?
    get() = when {
        this == null -> null
        !this.parentNode.isPresent -> SelectionRange(asRange, null)
        else -> SelectionRange(asRange, parentNode.get().parentalSelectionRange)
    }

class AstRepository(val server: JmlLanguageServer) {
    private val sourceFolders = Collections.synchronizedList(arrayListOf<Path>())

    val config: ParserConfiguration = ParserConfiguration()

    val inParsing: MutableMap<Uri, CompletableFuture<ParseResult<CompilationUnit>>> =
        Collections.synchronizedMap(mutableMapOf<Uri, CompletableFuture<ParseResult<CompilationUnit>>>())
    val cached: MutableMap<Uri, ParseResult<CompilationUnit>> =
        Collections.synchronizedMap(mutableMapOf<Uri, ParseResult<CompilationUnit>>())
    val version: MutableMap<Uri, Long> = Collections.synchronizedMap(mutableMapOf<Uri, Long>())

    val typeSolver: TypeSolver
        get() {
            synchronized(sourceFolders) {
                val elements: MutableList<TypeSolver> =
                    sourceFolders.asSequence().map { JavaParserTypeSolver(it) }.toMutableList()
                val classLoaderTypeSolver = ClassLoaderTypeSolver(ClassLoader.getSystemClassLoader())
                elements.add(0, classLoaderTypeSolver)
                return CombinedTypeSolver(elements)
            }
        }

    val symbolSolver: JavaSymbolSolver
        get() = JavaSymbolSolver(typeSolver)

    init {
        config.setSymbolResolver(symbolSolver)
        config.isProcessJml = true
    }

    fun createJavaParser() = JavaParser(config)

    private fun isUpToDate(uri: Uri, content: String): Boolean {
        return uri in cached && uri in version && crc32(content) == version[uri]
    }

    fun crc32(content: String) = crc32().hashBytes(content.toByteArray()).asLong()

    private fun parseSync(path: Uri): ParseResult<CompilationUnit> {
        val jp = createJavaParser()
        val content = path.path.readText()
        if (isUpToDate(path, content)) {
            return cached[path]!!
        }
        val result = jp.parse(content)
        result.result.ifPresent {
            it.setStorage(path.path, jp.parserConfiguration.characterEncoding)
            addSourceFolder(it.storage.get().sourceRoot)
        }
        announceResult(path, result)
        return result
    }

    private fun parse(path: Uri): CompletableFuture<ParseResult<CompilationUnit>> {
        val future = CompletableFuture
            .supplyAsync { parseSync(path) }
        synchronized(inParsing) {
            inParsing[path] = future
        }
        return future
    }

    private fun CompletableFuture<ParseResult<CompilationUnit>>.unpack() =
        thenApply { if (it.isSuccessful) it.result.get() else throw NullPointerException() }

    private fun announceResult(path: Uri, result: ParseResult<CompilationUnit>) {
        synchronized(cached) {
            synchronized(inParsing) {
                inParsing.remove(path)
            }
            cached[path] = result
            if (result.isSuccessful && result.result.isPresent) {
                server.client.showMessage(MessageParams(MessageType.Log, "$path parsed"))
            } else {
                reportOnParseError(path, result)
            }
        }
    }

    private fun getSourcePath(cu: CompilationUnit): Path? =
        cu.storage.map { it.sourceRoot }.orElse(null)

    fun addSourceFolder(folder: Path) {
        if (folder !in sourceFolders) {
            sourceFolders.add(folder)
            updateTypeSolver()
        }
    }

    fun updateTypeSolver() {
        val copy = synchronized(cached) { cached.toMap() }
        val solver = symbolSolver
        for (value in copy.values) {
            value.result.ifPresent { it.setData(Node.SYMBOL_RESOLVER_KEY, solver) }
        }
    }

    fun getDiagnostics(path: Uri): CompletableFuture<MutableList<Diagnostic>> =
        getParseResult(path).thenApply { result ->
            if (result.isSuccessful) {
                JmlLintingFacade(JmlLintingConfig()).lint(listOf(result.result.get())).map {
                    Diagnostic(it.location.asRange, it.message, DiagnosticSeverity.Error, "jmltk-lint")
                }.toMutableList()
            } else {
                result.problems.map {
                    Diagnostic(it.location.asRange, it.verboseMessage, DiagnosticSeverity.Error, "jmltk-parse")
                }.toMutableList()
            }
        }

    fun getParseResult(path: Uri): CompletableFuture<ParseResult<CompilationUnit>> {
        synchronized(inParsing) {
            inParsing[path]?.let { return it }
        }
        return parse(path)
    }

    operator fun get(path: Uri): CompletableFuture<CompilationUnit> = getParseResult(path).unpack()
    operator fun get(path: TextDocumentIdentifier) = get(Uri(path.uri))

    fun <T> reportOnParseError(uri: Uri, it: ParseResult<T>) {
        if (!it.isSuccessful) {
            server.client.publishDiagnostics(
                PublishDiagnosticsParams(
                    uri.toString(),
                    it.problems.map {
                        Diagnostic(it.location.asRange, it.verboseMessage, DiagnosticSeverity.Error, "jmltk-parse")
                    }.toMutableList()
                )
            )
        }
    }

    fun invalidate(uri: Uri) {
        synchronized(inParsing) {
            inParsing[uri]?.let { it.cancel(true) }
            cached.remove(uri)
        }
    }
}

/**
 * Represents URIs send from the client. These are different than Java URI class!
 * Incoming URIs have full protocol specifiers: `file://<path with root>`.
 */
@JvmInline
value class Uri(val value: String) {
    val isKeyFile: Boolean
        get() = file.extension == "key"

    val localFilePath: String
        get() = value.removePrefix("file://")
    val path: Path
        get() = Paths.get(localFilePath)
    val file: File
        get() = File(localFilePath)
}

class JmlTextDocumentService(private val server: JmlLanguageServer) : TextDocumentService {
    val repo = AstRepository(server)

    val jmlDocumentHighlighter = JmlDocumentHighlighter()
    val keyDocumentHighlighter = KeyDocumentHighlighter()
    val highlighters = listOf(jmlDocumentHighlighter, keyDocumentHighlighter)

    override fun didOpen(params: DidOpenTextDocumentParams) {
        Logger.info("didOpen: {}", params)
        Logger.info(params.textDocument.languageId)
        if (params.textDocument.languageId != "text/java" || params.textDocument.languageId != "java") {
            // Already open and parse file. The next requests are coming.
            repo[Uri(params.textDocument.uri)].get()
        }
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        Logger.info("didChange: {}", params)
        repo.invalidate(Uri(params.textDocument.uri))
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        Logger.info("didClose: {}", params)
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
        Logger.info("didSave: {}", params)
        repo.invalidate(Uri(params.textDocument.uri))
    }

    override fun hover(params: HoverParams): CompletableFuture<Hover?> = repo[params.textDocument]
        .thenApplyAsync {
            val symbol = findSymbol(params.position, it)
            if (symbol == null) {
                findKeyword(params, it)
            } else {
                val text = try {
                    val r = symbol.resolve()
                    getHoverMessage(r)
                } catch (e: UnresolvedReferenceException) {
                    try {
                        val type = symbol.calculateResolvedType()
                        getHoverMessage(type)
                    } catch (e: UnresolvedReferenceException) {
                        """---\nType and name unresolved"""
                    }
                }
                Hover(
                    MarkupContent(
                        "markdown",
                        """Symbol ${symbol.nameAsString}
                        ---
                        $text """".trimIndent()
                    )
                )
            }
        }.exceptionally { null }

    private fun getHoverMessage(r: ResolvedDeclaration?): String = if (r is AssociableToAST && r.toAst().isPresent) {
        getHoverMessage(r.toAst().get())
    } else {
        """---\n${r.toString()}"""
    }

    private fun getHoverMessage(ast: Node): String {
        val targetUri = ast.findCompilationUnit().get().storage.get().path
        val targetRange = ast.asRange
        val targetSelectionRange = targetRange.start.line
        return """---
                  $targetUri:$targetSelectionRange
                  """.trimIndent()
    }

    private fun getHoverMessage(r: ResolvedType) = if (r is AssociableToAST && r.toAst().isPresent) {
        getHoverMessage(r.toAst().get())
    } else {
        """---\n$r"""
    }


    private fun findKeyword(params: HoverParams, cu: CompilationUnit): Hover? {
        val node = findTopMostJmlishNode(params.position, cu)
        if (node != null) {
            val tokenRange = node.tokenRange.get()
            val pos = params.position.asPosition
            val javaToken = tokenRange.find { it.range.get().contains(pos) }
            return javaToken?.text?.let { retrieveDocumentation(it) }
        }
        return null
    }

    private val jmlDocumentationIndex by lazy { JmlDocumentationIndex() }

    private fun retrieveDocumentation(tokenText: String): Hover =
        jmlDocumentationIndex.get(tokenText)
            ?.let { text -> Hover(MarkupContent("markdown", text)) }
            ?: Hover(MarkupContent("markdown", "No documentation given for keyword `$tokenText`"))

    override fun signatureHelp(params: SignatureHelpParams): CompletableFuture<SignatureHelp> {
        /*val path = Uri(params.textDocument.uri)
        val pos = params.position
        val active =
            params.context.activeSignatureHelp.signatures[params.context.activeSignatureHelp.activeSignature]
         */
        return CompletableFuture.supplyAsync {
            SignatureHelp()
        }
    }

    override fun declaration(params: DeclarationParams): CompletableFuture<Either<MutableList<out Location>, MutableList<out LocationLink>>> {
        return repo[params.textDocument]
            .thenApplyAsync { findSymbol(params.position, it) }
            .thenApplyAsync { resolveSymbolInDocument(it) }
    }

    private fun resolveSymbolInDocument(nameExpr: NameExpr?): Either<MutableList<out Location>, MutableList<out LocationLink>> {
        if (nameExpr != null) {
            val r = nameExpr.resolve()
            if (r is AssociableToAST && r.toAst().isPresent) {
                val ast = r.toAst().get()
                val targetUri = "file://${ast.findCompilationUnit().get().storage.get().path.toFile()}"
                val targetRange = ast.asRange
                val targetSelectionRange = targetRange
                // TODO be more specific dependening targeted type
                return Either.forRight(arrayListOf(LocationLink(targetUri, targetRange, targetSelectionRange)))
            }
        }
        return Either.forLeft(arrayListOf())
    }

    private fun findSymbol(position: Position, it: CompilationUnit): NameExpr? {
        val p = position.toJavaParser()
        val queue: Queue<Node> = LinkedList()
        queue.add(it)
        while (queue.isNotEmpty()) {
            val n = queue.poll()
            val range = n.range.get()
            val contains = range.contains(p)
            if (contains && n is NameExpr) {
                return n
            }
            if (contains) {
                queue.addAll(n.childNodes)
            }
        }
        return null
    }

    private fun findSymbolByRange(position: Position, it: ParseResult<CompilationUnit>): NameExpr? {
        if (!it.result.isPresent) return null
        var current: Node = it.result.get()
        val pos = position.toJavaParser()

        next@ while (current.range.get().contains(pos)) {
            if (current is NameExpr) {
                return current
            }

            for (child in current.childNodes) {
                if (child.range.get().contains(pos)) {
                    current = child
                    continue@next
                }
            }
        }
        return null
    }

    override fun documentSymbol(params: DocumentSymbolParams): CompletableFuture<MutableList<Either<SymbolInformation, DocumentSymbol>>> {
        Logger.info("params: {}", params)
        val uri = Uri(params.textDocument.uri)
        return if (uri.isKeyFile) {
            CompletableFuture.supplyAsync { KeyCatchSymbols(uri).run() }
        } else {
            repo.getParseResult(uri).thenApply {
                Logger.info("Parse: {}", it)
                if (!it.result.isPresent) {
                    mutableListOf()
                } else {
                    resolveSymbol(it.result.get())
                }
            }
        }
    }

    private fun resolveSymbol(compilationUnit: CompilationUnit): MutableList<Either<SymbolInformation, DocumentSymbol>> {
        Logger.info("Resolve symbols for compiluation unit: {}", compilationUnit.storage.get().path)
        val visitor = JmlCatchSymbols()
        val a = compilationUnit.accept(visitor, null) ?: arrayListOf()
        Logger.info("Symbols caught: {}", a.size)
        val b = a.map { Either.forRight<SymbolInformation, DocumentSymbol>(it) }
        return b.toMutableList()
    }

    override fun codeAction(params: CodeActionParams): CompletableFuture<MutableList<Either<Command, CodeAction>>> {
        Logger.info("codeAction: {}", params)
        val codeActions = repo.getParseResult(Uri(params.textDocument.uri))
            .applyOn(CodeActionCollector(params.context, params.range.asRange), arrayListOf())
        return codeActions.thenApply {
            it.addAll(universalCommands())
            it
        }
    }

    internal fun universalCommands(): List<Either<Command, CodeAction>> {
        return listOf(
            Either.forLeft(CreateKeyProjectFile().command()),
            Either.forLeft(StartKey().command())
        )
    }

    override fun codeLens(params: CodeLensParams): CompletableFuture<MutableList<out CodeLens>> {
        Logger.info("codeLens: {}", params)
        return repo.getParseResult(Uri(params.textDocument.uri)).applyOn(CodeLensCollector(), arrayListOf())
    }

    override fun resolveCodeLens(unresolved: CodeLens): CompletableFuture<CodeLens> {
        Logger.info("codeLens: {}", unresolved)
        return CompletableFuture.completedFuture(CodeLens())
    }

    private fun findTopMostJmlishNode(position: Position, cu: CompilationUnit): Node? =
        findTopMostJmlishNode(position.toJavaParser(), cu)

    private fun findTopMostJmlishNode(position: JPosition, cu: CompilationUnit): Node? =
        findNode(position, cu) {
            it is Jmlish
                || (it is Modifier && it.keyword.toString().startsWith("JML_"))
        }

    private fun findNode(
        p: Position, it: Node,
        pred: (Node) -> Boolean = { it.childNodes.isEmpty() }
    ) = findNode(p.toJavaParser(), it, pred)

    private fun findNode(
        p: JPosition, it: Node,
        pred: (Node) -> Boolean = { it.childNodes.isEmpty() }
    ): Node? {
        val queue: Queue<Node> = LinkedList()
        queue.add(it)
        while (queue.isNotEmpty()) {
            val n = queue.poll()
            val contains = n.range.get().contains(p)
            if (contains && pred(n)) {
                return n
            }

            for (node in n.childNodes) {
                if (node.range.get().contains(p)) {
                    queue.add(node)
                }
            }
        }
        return null
    }

    override fun selectionRange(params: SelectionRangeParams): CompletableFuture<MutableList<SelectionRange>> =
        repo[params.textDocument]
            .thenApplyAsync { it ->
                val nodes = params.positions.map { p -> findNode(p, it) }
                nodes.mapNotNull { it.parentalSelectionRange }
                    .toMutableList()
            }.exceptionally {
                if (it !is NullPointerException) {
                    server.client.logMessage(
                        MessageParams(
                            MessageType.Error,
                            it.toString() + "\n" + it.stackTraceToString()

                        )
                    )
                }
                mutableListOf()
            }

    override fun semanticTokensFull(params: SemanticTokensParams): CompletableFuture<SemanticTokens> =
        CompletableFuture.supplyAsync {
            val doc = Uri(params.textDocument.uri)
            val text = doc.file.readText()
            when (doc.file.extension) {
                "java" -> jmlDocumentHighlighter.analyzeToken(text)
                "key" -> keyDocumentHighlighter.analyzeToken(text)
                else -> SemanticTokens()
            }
        }

    override fun diagnostic(params: DocumentDiagnosticParams): CompletableFuture<DocumentDiagnosticReport> {
        Logger.info("diagnostic: {}", params)
        val path = Uri(params.textDocument.uri)
        return repo.getDiagnostics(path).thenApply {
            Logger.info("Found errors: {}", it)
            DocumentDiagnosticReport(RelatedFullDocumentDiagnosticReport(it))
        }
    }

    override fun foldingRange(params: FoldingRangeRequestParams): CompletableFuture<List<FoldingRange>> =
        repo.getParseResult(Uri(params.textDocument.uri))
            .thenApplyAsync {
                listOf<FoldingRange>()
            }.exceptionally {
                server.client.logMessage(
                    MessageParams(
                        MessageType.Error,
                        it.toString() + "\n" + it.stackTraceToString()

                    )
                )
                listOf()
            }
}

fun Position.toJavaParser() = JPosition(line + 1, character)
fun JPosition.toLsp() = Position(line - 1, column)

private fun <T> CompletableFuture<ParseResult<CompilationUnit>>.applyOn(
    collector: ResultingVisitor<T>,
    default: T
): CompletableFuture<T> =
    this.thenApplyAsync {
        if (it.result.isPresent) {
            it.result.get().accept(collector, null)
            val r = collector.result
            Logger.info("Result: {}", r)
            r
        } else {
            default
        }
    }

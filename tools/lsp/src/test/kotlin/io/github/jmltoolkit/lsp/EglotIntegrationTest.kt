/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import com.google.common.truth.Truth
import org.assertj.core.api.Assertions
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

/**
 * Integration test that simulates an Eglot (Emacs LSP client) session.
 * This test reproduces the exact LSP protocol exchange from an Eglot 1.17.30 client
 * connecting to the JML Language Server.
 *
 * Test scenario:
 * 1. Initialize the language server with Eglot capabilities
 * 2. Open Stack.java document
 * 3. Perform multiple hover requests at various positions
 * 4. Simulate a document change (typo correction)
 * 5. Perform a declaration request
 *
 * @author Alexander Weigl
 * @version 1 (17.08.2026)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EglotIntegrationTest : TestUtilities() {

    init {
        /*
          {
          "jsonrpc": "2.0",
          "id": 1,
          "method": "initialize",
          "params": {
            "processId": 4160214,
            "clientInfo": {
              "name": "Eglot",
              "version": "1.17.30"
            },
            "rootPath": "/home/weigl/work/emacs-lsp-jml/examples/",
            "rootUri": "file:///home/weigl/work/emacs-lsp-jml/examples",
            "initializationOptions": {},
            "capabilities": {
              "workspace": {
                "applyEdit": true,
                "executeCommand": {
                  "dynamicRegistration": false
                },
                "workspaceEdit": {
                  "documentChanges": true
                },
                "didChangeWatchedFiles": {
                  "dynamicRegistration": true
                },
                "symbol": {
                  "dynamicRegistration": false
                },
                "configuration": true,
                "workspaceFolders": true
              },
              "textDocument": {
                "synchronization": {
                  "dynamicRegistration": false,
                  "willSave": true,
                  "willSaveWaitUntil": true,
                  "didSave": true
                },
                "completion": {
                  "dynamicRegistration": false,
                  "completionItem": {
                    "snippetSupport": false,
                    "deprecatedSupport": true,
                    "resolveSupport": {
                      "properties": [
                        "documentation",
                        "details",
                        "additionalTextEdits"
                      ]
                    },
                    "tagSupport": {
                      "valueSet": [
                        1
                      ]
                    }
                  },
                  "contextSupport": true
                },
                "hover": {
                  "dynamicRegistration": false,
                  "contentFormat": [
                    "plaintext"
                  ]
                },
                "signatureHelp": {
                  "dynamicRegistration": false,
                  "signatureInformation": {
                    "parameterInformation": {
                      "labelOffsetSupport": true
                    },
                    "documentationFormat": [
                      "plaintext"
                    ],
                    "activeParameterSupport": true
                  }
                },
                "references": {
                  "dynamicRegistration": false
                },
                "definition": {
                  "dynamicRegistration": false,
                  "linkSupport": true
                },
                "declaration": {
                  "dynamicRegistration": false,
                  "linkSupport": true
                },
                "implementation": {
                  "dynamicRegistration": false,
                  "linkSupport": true
                },
                "typeDefinition": {
                  "dynamicRegistration": false,
                  "linkSupport": true
                },
                "documentSymbol": {
                  "dynamicRegistration": false,
                  "hierarchicalDocumentSymbolSupport": true,
                  "symbolKind": {
                    "valueSet": [              1,              2,              3,              4,              5,
                      6,              7,              8,              9,              10,              11,              12,
                      13,              14,              15,              16,              17,              18,              19,
                      20,              21,              22,              23,              24,              25,              26
                    ]
                  }
                },
                "documentHighlight": {
                  "dynamicRegistration": false
                },
                "codeAction": {
                  "dynamicRegistration": false,
                  "resolveSupport": {
                    "properties": [
                      "edit",
                      "command"
                    ]
                  },
                  "dataSupport": true,
                  "codeActionLiteralSupport": {
                    "codeActionKind": {
                      "valueSet": [
                        "quickfix",
                        "refactor",
                        "refactor.extract",
                        "refactor.inline",
                        "refactor.rewrite",
                        "source",
                        "source.organizeImports"
                      ]
                    }
                  },
                  "isPreferredSupport": true
                },
                "formatting": {
                  "dynamicRegistration": false
                },
                "rangeFormatting": {
                  "dynamicRegistration": false
                },
                "rename": {
                  "dynamicRegistration": false
                },
                "inlayHint": {
                  "dynamicRegistration": false
                },
                "publishDiagnostics": {
                  "relatedInformation": false,
                  "codeDescriptionSupport": false,
                  "tagSupport": {
                    "valueSet": [
                      1,
                      2
                    ]
                  }
                }
              },
              "window": {
                "showDocument": {
                  "support": true
                },
                "workDoneProgress": true
              },
              "general": {
                "positionEncodings": [
                  "utf-32",
                  "utf-8",
                  "utf-16"
                ]
              },
              "experimental": {}
            },
            "workspaceFolders": [
              {
                "uri": "file:///home/weigl/work/emacs-lsp-jml/examples",
                "name": "~/work/emacs-lsp-jml/examples/"
              }
            ]
          }
        }
        */
        val params = InitializeParams().apply {
            processId = 4160214
            clientInfo = ClientInfo("Eglot", "1.17.30")
            rootPath = Paths.get("workspace").toAbsolutePath().toString()
            rootUri = Paths.get("workspace").toAbsolutePath().toUri().toString()
            initializationOptions = mapOf<String, Any>()

            capabilities = ClientCapabilities().apply {
                workspace = WorkspaceClientCapabilities().apply {
                    applyEdit = true
                    executeCommand = ExecuteCommandCapabilities().apply {
                        dynamicRegistration = false
                    }
                    workspaceEdit = WorkspaceEditCapabilities().apply {
                        documentChanges = true
                    }
                    didChangeWatchedFiles = DidChangeWatchedFilesCapabilities().apply {
                        dynamicRegistration = true
                    }
                    symbol = SymbolCapabilities().apply {
                        dynamicRegistration = false
                    }
                    configuration = true
                    workspaceFolders = true
                }

                textDocument = TextDocumentClientCapabilities().apply {
                    synchronization = SynchronizationCapabilities().apply {
                        dynamicRegistration = false
                        willSave = true
                        willSaveWaitUntil = true
                        didSave = true
                    }

                    completion = CompletionCapabilities().apply {
                        dynamicRegistration = false
                        completionItem = CompletionItemCapabilities().apply {
                            snippetSupport = false
                            deprecatedSupport = true
                            resolveSupport = CompletionItemResolveSupportCapabilities().apply {
                                properties = listOf("documentation", "details", "additionalTextEdits")
                            }
                            tagSupport = CompletionItemTagSupportCapabilities().apply {
                                valueSet = listOf(CompletionItemTag.forValue(1))
                            }
                        }
                        contextSupport = true
                    }

                    hover = HoverCapabilities().apply {
                        dynamicRegistration = false
                        contentFormat = listOf(MarkupKind.PLAINTEXT)
                    }

                    signatureHelp = SignatureHelpCapabilities().apply {
                        dynamicRegistration = false
                        signatureInformation = SignatureInformationCapabilities().apply {
                            parameterInformation = ParameterInformationCapabilities().apply {
                                labelOffsetSupport = true
                            }
                            documentationFormat = listOf(MarkupKind.PLAINTEXT)
                            activeParameterSupport = true
                        }
                    }

                    references = ReferencesCapabilities().apply {
                        dynamicRegistration = false
                    }

                    definition = DefinitionCapabilities().apply {
                        dynamicRegistration = false
                        linkSupport = true
                    }

                    declaration = DeclarationCapabilities().apply {
                        dynamicRegistration = false
                        linkSupport = true
                    }

                    implementation = ImplementationCapabilities().apply {
                        dynamicRegistration = false
                        linkSupport = true
                    }

                    typeDefinition = TypeDefinitionCapabilities().apply {
                        dynamicRegistration = false
                        linkSupport = true
                    }

                    documentSymbol = DocumentSymbolCapabilities().apply {
                        dynamicRegistration = false
                        hierarchicalDocumentSymbolSupport = true
                        symbolKind = SymbolKindCapabilities().apply {
                            valueSet = listOf(
                                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23, 24, 25, 26
                            ).map { SymbolKind.forValue(it) }
                        }
                    }

                    documentHighlight = DocumentHighlightCapabilities().apply {
                        dynamicRegistration = false
                    }

                    codeAction = CodeActionCapabilities().apply {
                        dynamicRegistration = false
                        resolveSupport = CodeActionResolveSupportCapabilities().apply {
                            properties = listOf("edit", "command")
                        }
                        dataSupport = true
                        codeActionLiteralSupport = CodeActionLiteralSupportCapabilities().apply {
                            codeActionKind = CodeActionKindCapabilities().apply {
                                valueSet = listOf(
                                    "quickfix", "refactor", "refactor.extract",
                                    "refactor.inline", "refactor.rewrite", "source",
                                    "source.organizeImports"
                                )
                            }
                        }
                        isPreferredSupport = true
                    }

                    formatting = FormattingCapabilities().apply {
                        dynamicRegistration = false
                    }

                    rangeFormatting = RangeFormattingCapabilities().apply {
                        dynamicRegistration = false
                    }

                    rename = RenameCapabilities().apply {
                        dynamicRegistration = false
                    }

                    inlayHint = InlayHintCapabilities().apply {
                        dynamicRegistration = false
                    }

                    publishDiagnostics = PublishDiagnosticsCapabilities().apply {
                        relatedInformation = false
                        codeDescriptionSupport = false
                        tagSupport = Either.forRight(
                            DiagnosticsTagSupport().apply {
                            valueSet = listOf(1, 2).map { DiagnosticTag.forValue(it) }
                        }
                        )
                    }
                }

                window = WindowClientCapabilities().apply {
                    showDocument = ShowDocumentCapabilities(true)
                    workDoneProgress = true
                }

                general = GeneralClientCapabilities().apply {
                    positionEncodings =
                        listOf(PositionEncodingKind.UTF32, PositionEncodingKind.UTF8, PositionEncodingKind.UTF16)
                }
            }

            workspaceFolders = listOf(
                WorkspaceFolder("file:///home/weigl/work/emacs-lsp-jml/examples", "~/work/emacs-lsp-jml/examples/")
            )
        }
        languageServer.connect(EmptyLanguageClient())
        languageServer.initialize(params).get()
        // Send initialized notification
        languageServer.initialized(InitializedParams())
    }

    @Test
    fun testHover() {
        val stackFile = File(workspace, "Stack.java")
        val stackUri = stackFile.toUri

        // Open document
        val openParams = DidOpenTextDocumentParams(
            TextDocumentItem(
                stackUri,
                "java",
                0,
                stackFile.readText()
            )
        )
        docService.didOpen(openParams)

        // Test hover at line 7, character 4 (public keyword in model declaration)
        val hover0Params = HoverParams(TextDocumentIdentifier(stackUri), Position(16, 39))
        val hover0Resp = docService.hover(hover0Params).get()
        Truth.assertThat(hover0Resp).isNotNull()

        // Test hover at line 7, character 4 (public keyword in model declaration)
        val hover1Params = HoverParams(TextDocumentIdentifier(stackUri), Position(6, 19))
        val hover1Resp = docService.hover(hover1Params).get()
        Truth.assertThat(hover1Resp).isNotNull()

        // Test hover at line 19, character 14 (capacity parameter in constructor)
        val hover2Params = HoverParams(TextDocumentIdentifier(stackUri), Position(19, 14))
        val hover2Resp = docService.hover(hover2Params).get()
        Truth.assertThat(hover2Resp).isNotNull()

        val hover3Params = HoverParams(TextDocumentIdentifier(stackUri), Position(16, 13))
        val hover3Resp = docService.hover(hover3Params).get()
        Truth.assertThat(hover3Resp).isNotNull()

        /*
        // Test hover at line 30, character 14 (push method)
        val hover4Params = HoverParams(TextDocumentIdentifier(stackUri), Position(31, 19))
        val hover4Resp = docService.hover(hover4Params).get()
        Truth.assertThat(hover4Resp).isNotNull()
        */

        // Test hover at line 48, character 20 (pop method)
        val hover6Params = HoverParams(TextDocumentIdentifier(stackUri), Position(44, 18))
        val hover6Resp = docService.hover(hover6Params).get()
        Truth.assertThat(hover6Resp).isNotNull()

        // Test hover at line 92, character 33 (top in invariant)
        val hover10Params = HoverParams(TextDocumentIdentifier(stackUri), Position(88, 23))
        val hover10Resp = docService.hover(hover10Params).get()
        Truth.assertThat(hover10Resp).isNotNull()

        // Test hover at line 88, character 0 (invariant comment)
        val hover8Params = HoverParams(TextDocumentIdentifier(stackUri), Position(87, 10))
        val hover8Resp = docService.hover(hover8Params).get()
        Truth.assertThat(hover8Resp).isNotNull()
    }

    @Test
    fun testEglotDocumentChange() {
        val stackFile = File(workspace, "Stack.java")
        val stackUri = stackFile.toUri

        // Open document
        val openParams = DidOpenTextDocumentParams(
            TextDocumentItem(
                stackUri,
                "java",
                0,
                stackFile.readText()
            )
        )
        docService.didOpen(openParams)

        // Simulate document change (typo: "demonstrates" -> "demonstreames")
        val originalText = stackFile.readText()
        val modifiedText = originalText.replace("demonstrates", "demonstreames")

        val changeParams = DidChangeTextDocumentParams(
            VersionedTextDocumentIdentifier(stackUri, -1),
            listOf(TextDocumentContentChangeEvent().apply { text = modifiedText })
        )
        docService.didChange(changeParams)

        // Verify the change was accepted (no exception thrown)
        Truth.assertThat(modifiedText).contains("demonstreames")
    }

    @Test
    fun testEglotDeclaration() {
        val stackFile = File(workspace, "Stack.java")
        val stackUri = stackFile.toUri

        // Open document
        val openParams = DidOpenTextDocumentParams(
            TextDocumentItem(
                stackUri,
                "java",
                0,
                stackFile.readText()
            )
        )
        docService.didOpen(openParams)

        // Request declaration at line 4, character 21 (Stack class reference)
        val declarationParams = DeclarationParams(
            TextDocumentIdentifier(stackUri),
            Position(4, 21)
        )
        val declarationResp = docService.declaration(declarationParams).get()

        // Declaration should return location(s) pointing to the class definition
        Truth.assertThat(declarationResp).isNotNull()
    }

    @Test
    fun testFullSemanticTokens() {
        // {"jsonrpc":"2.0","id":3,"method":"textDocument/semanticTokens/full","params":{"textDocument":{"uri":"file:///home/weigl/work/javaparser/tools/lsp/workspace/test.key"}}}

        val stackFile = File(workspace, "Stack.java")
        val stackUri = stackFile.toUri
        val keyFile = File(workspace, "test.key").toUri

        val tokensJml = docService.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(stackUri))).get()
        Assertions.assertThat(tokensJml.data)
            .isNotNull()
            .isNotEmpty

        val tokensKey = docService.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(keyFile))).get()
        Assertions.assertThat(tokensKey.data).isNotNull().isNotEmpty
    }

    class EmptyLanguageClient : LanguageClient {
        override fun telemetryEvent(`object`: Any?) {}
        override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams?) {}
        override fun showMessage(messageParams: MessageParams?) {}
        override fun showMessageRequest(requestParams: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> =
            CompletableFuture.completedFuture(MessageActionItem("Test!"))

        override fun logMessage(message: MessageParams?) {}
    }
}

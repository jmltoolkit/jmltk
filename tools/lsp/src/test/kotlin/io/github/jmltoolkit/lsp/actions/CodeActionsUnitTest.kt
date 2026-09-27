/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.actions

import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.jml.body.JmlClassExprDeclaration
import com.github.javaparser.ast.jml.clauses.JmlBehaviorKeyword
import com.github.javaparser.ast.jml.clauses.JmlContract
import com.google.common.truth.Truth.assertThat
import io.github.jmltoolkit.lsp.JmlLanguageServer
import io.github.jmltoolkit.lsp.JmlTestSupport
import io.github.jmltoolkit.lsp.asRange
import io.github.jmltoolkit.lsp.asLeft
import io.github.jmltoolkit.lsp.asRight
import io.github.jmltoolkit.utils.JMLUtils.isInJML
import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.WorkspaceFolder
import org.eclipse.lsp4j.services.LanguageClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/** Minimal language client, mirroring [io.github.jmltoolkit.lsp.TestUtilities.EmptyLanguageClient]. */
private class StubLanguageClient : LanguageClient {
    override fun telemetryEvent(`object`: Any?) {}
    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams?) {}
    override fun showMessage(messageParams: MessageParams?) {}
    override fun showMessageRequest(requestParams: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> =
        CompletableFuture.completedFuture(MessageActionItem("Test!"))
    override fun logMessage(message: MessageParams?) {}
}

private fun initializedServer(rootUri: String): JmlLanguageServer =
    JmlLanguageServer().also {
        it.connect(StubLanguageClient())
        val params = InitializeParams().also { p ->
            p.rootUri = rootUri
            p.workspaceFolders = listOf(WorkspaceFolder(rootUri, "Test Bed"))
            p.rootPath = rootUri.removePrefix("file://")
            p.capabilities = org.eclipse.lsp4j.ClientCapabilities(
                org.eclipse.lsp4j.WorkspaceClientCapabilities(),
                org.eclipse.lsp4j.TextDocumentClientCapabilities(),
                org.eclipse.lsp4j.WindowClientCapabilities(),
                false
            )
        }
        it.initialize(params).get()
    }

private fun snippetTexts(edit: org.eclipse.lsp4j.WorkspaceEdit): List<String> =
    edit.documentChanges
        .mapNotNull { it.left }
        .flatMap { it.edits }
        .mapNotNull { it.left?.newText ?: it.right?.snippet?.value }

class AddBehaviorKeywordTest {

    private fun contract(source: String): JmlContract {
        val cu = JmlTestSupport.parse(source)
        return cu.findAll(JmlContract::class.java).first()
    }

    @Test
    fun `is callable only for contracts`() {
        val action = AddBehaviorKeyword()
        val cu = JmlTestSupport.parse(
            """
            class A {
                int size;
                /*@ requires size > 0; @*/
                int getSize() { return size; }
            }
            """.trimIndent()
        )
        assertThat(action.isCallableForNode(cu.findAll(JmlContract::class.java).first())).isTrue()
        assertThat(action.isCallableForNode(NameExpr("x"))).isFalse()
        assertThat(action.isCallableForNode(FieldDeclaration())).isFalse()
    }

    @Test
    fun `offers action for contract without behavior keyword`() {
        val contract = contract(
            """
            class A {
                /*@ requires size > 0; @*/
                int getSize() { return 0; }
                int size;
            }
            """.trimIndent()
        )
        assertThat(contract.behavior()).isNull()
        val action = AddBehaviorKeyword().createCodeAction("file:///A.java", contract)
        assertThat(action).isNotNull()
        assertThat(action!!.right.kind).isEqualTo(AddBehaviorKeyword().kind)
        val snippet = snippetTexts(action.right.edit).joinToString("")
        assertThat(snippet).contains("normal_behavior")
        assertThat(snippet).contains(JmlBehaviorKeyword.NORMAL.jmlSymbol())
    }

    @Test
    fun `does not offer action for contract that already has a behavior keyword`() {
        val contract = contract(
            """
            class A {
                /*@ model_behavior requires size > 0; @*/
                int getSize() { return 0; }
                int size;
            }
            """.trimIndent()
        )
        assertThat(contract.behavior()?.value).isEqualTo(JmlBehaviorKeyword.MODEL)
        assertThat(AddBehaviorKeyword().createCodeAction("file:///A.java", contract)).isNull()
    }
}

class AddNameToClauseOrContractTest {

    @Test
    fun `offers action for contract without a name`() {
        val cu = JmlTestSupport.parse(
            """
            class A {
                /*@ requires size > 0; @*/
                int getSize() { return 0; }
                int size;
            }
            """.trimIndent()
        )
        val contract = cu.findAll(JmlContract::class.java).first()
        assertThat(contract.name.isPresent).isFalse()
        val action = AddNameToClauseOrContract().createCodeAction("file:///A.java", contract)
        assertThat(action).isNotNull()
        val snippet = snippetTexts(action!!.right.edit).joinToString("")
        assertThat(snippet).contains("name")
    }

    @Test
    fun `does not offer action for a named contract`() {
        val cu = JmlTestSupport.parse(
            """
            class A {
                /*@ normal_behavior myName: requires size > 0; @*/
                int getSize() { return 0; }
                int size;
            }
            """.trimIndent()
        )
        val contract = cu.findAll(JmlContract::class.java).first()
        assertThat(contract.name.map { it.asString() }.orElse("")).isEqualTo("myName")
        assertThat(AddNameToClauseOrContract().createCodeAction("file:///A.java", contract)).isNull()
    }

    @Test
    fun `offers and withholds action for clauses depending on their name`() {
        val named = JmlTestSupport.parse(
            """
            class A {
                /*@ requires r1: size > 0; @*/
                int getSize() { return 0; }
                int size;
            }
            """.trimIndent()
        )
        val action = AddNameToClauseOrContract()
        val namedClause = named.findAll(com.github.javaparser.ast.jml.clauses.JmlClause::class.java).first()
        assertThat(namedClause.name.isPresent).isTrue()
        assertThat(action.createCodeAction("file:///A.java", namedClause)).isNull()

        val unnamed = JmlTestSupport.parse(
            """
            class A {
                /*@ requires size > 0; @*/
                int getSize() { return 0; }
                int size;
            }
            """.trimIndent()
        )
        val unnamedClause = unnamed.findAll(com.github.javaparser.ast.jml.clauses.JmlClause::class.java).first()
        assertThat(action.createCodeAction("file:///A.java", unnamedClause)).isNotNull()
    }

    @Test
    fun `offers action for class invariants and detects their names`() {
        val action = AddNameToClauseOrContract()
        val unnamed = JmlTestSupport.parse(
            """
            class A {
                //@ invariant size >= 0;
                int size;
            }
            """.trimIndent()
        )
        val unnamedInvariant = unnamed.findAll(JmlClassExprDeclaration::class.java).first()
        assertThat(action.createCodeAction("file:///A.java", unnamedInvariant)).isNotNull()

        // The parser currently drops the name of named class level declarations
        // (see the `//TODO weigl` in the JmlClassExpr grammar rule), so the parsed
        // node reports no name. Construct the node by hand to cover hasName().
        assertThat(action.hasName(unnamedInvariant)).isFalse()
        assertThat(action.hasName(JmlClassExprDeclaration().setName(SimpleName("inv1")))).isTrue()
        assertThat(action.hasName(NameExpr("x"))).isFalse()
    }

    @Test
    fun `not callable for unrelated nodes`() {
        assertThat(AddNameToClauseOrContract().isCallableForNode(NameExpr("x"))).isFalse()
        assertThat(AddNameToClauseOrContract().isCallableForNode(FieldDeclaration())).isFalse()
    }
}

class AddQuantificationTest {

    @Test
    fun `offers action for names inside JML clauses`() {
        val cu = JmlTestSupport.parse(
            """
            class A {
                int size;
                /*@ requires size > 0; @*/
                int getSize() { return size; }
            }
            """.trimIndent()
        )
        val nameInJml = cu.findAll(NameExpr::class.java).first { it.nameAsString == "size" && it.isInJML() }
        val action = AddQuantification().createCodeAction("file:///A.java", nameInJml)
        assertThat(action).isNotNull()
        val snippets = snippetTexts(action!!.right.edit)
        // The action wraps the innermost JML expression into a universal quantifier.
        assertThat(snippets.joinToString(" ")).contains("\\forall")
        assertThat(snippets.joinToString(" ")).contains("int")
    }

    @Test
    fun `no action for names outside JML context`() {
        val cu = JmlTestSupport.parse(
            """
            class A {
                int size;
                int getSize() { return size; }
            }
            """.trimIndent()
        )
        val nameInCode = cu.findAll(NameExpr::class.java).first { it.nameAsString == "size" }
        assertThat(AddQuantification().createCodeAction("file:///A.java", nameInCode)).isNull()
    }
}

class AddStubsTest {

    @Test
    fun `creates code lens for type declarations`() {
        val cu = JmlTestSupport.parse("public class A { }")
        val type = cu.types.first() as TypeDeclaration<*>
        val lens = AddStubs().createCodeLens("file:///A.java", type)
        assertThat(lens).isNotNull()
        assertThat(lens!!.range).isEqualTo(type.name.asRange)
        assertThat(lens.command.title).isEqualTo("Add stub contracts to type if missing")
        assertThat(lens.command.arguments).containsExactly("file:///A.java", type.name.range.get().asRange)
    }

    @Test
    fun `no code lens for non type declarations`() {
        val name = JmlTestSupport.parse("class A { int size; }").findAll(FieldDeclaration::class.java).first()
        assertThat(AddStubs().createCodeLens("file:///A.java", name)).isNull()
    }

    @Test
    fun `command references the target's range`() {
        val cu = JmlTestSupport.parse("package foo.bar; public class A { }")
        val pkg = cu.packageDeclaration.orElseThrow()
        val command = AddStubs().command("file:///A.java", pkg)
        assertThat(command.command).isEqualTo(AddStubs::class.java.name)
        assertThat(command.arguments).containsExactly("file:///A.java", pkg.name.range.get().asRange)
    }

    @Test
    fun `execute fails when no parameters are given`() {
        assertThrows<IllegalStateException> {
            AddStubs().execute(JmlLanguageServer(), null)
        }
    }
}

class GsonHelperTest {
    @Test
    fun `unwrap round trips a range`() {
        val range = Range(org.eclipse.lsp4j.Position(3, 7), org.eclipse.lsp4j.Position(3, 11))
        val unwrapped = GsonHelper.unwrap<Range>(range)
        assertThat(unwrapped).isEqualTo(range)
    }
}

class WellDefinednessCheckTest {

    @Test
    fun `is callable for JML clause and statement nodes`() {
        val action = WellDefinednessCheck()
        assertThat(
            action.isCallableForNode(
                JmlTestSupport.parse(
                    """
                    class A {
                        int size;
                        /*@ requires size > 0; @*/
                        int getSize() { return size; }
                    }
                    """.trimIndent()
                ).findAll(com.github.javaparser.ast.jml.clauses.JmlSimpleExprClause::class.java).first()
            )
        ).isTrue()
        assertThat(action.isCallableForNode(NameExpr("x"))).isFalse()
        assertThat(action.isCallableForNode(FieldDeclaration())).isFalse()
    }

    @Test
    fun `creates quick fix command`() {
        val clause = JmlTestSupport.parse(
            """
            class A {
                int size;
                /*@ requires size > 0; @*/
                int getSize() { return size; }
            }
            """.trimIndent()
        ).findAll(com.github.javaparser.ast.jml.clauses.JmlSimpleExprClause::class.java).first()

        val action = WellDefinednessCheck().createCodeAction("file:///A.java", clause)
        assertThat(action.right.kind).isEqualTo(CodeActionKind.QuickFix)
        assertThat(action.right.command.command).isEqualTo("jml.welldefinedness-check")
        val args = action.right.command.arguments
        assertThat(args).hasSize(2)
        assertThat(args[0]).isEqualTo("file:///A.java")
    }

    @Test
    fun `execute fails when no parameters are given`() {
        assertThrows<IllegalStateException> {
            WellDefinednessCheck().execute(JmlLanguageServer(), null)
        }
    }
}

class VerifyAgainstParentTest {
    @Test
    fun `creates command carrying node hash for contracts`() {
        val cu = JmlTestSupport.parse(
            """
            class A {
                int size;
                /*@ requires size > 0; @*/
                int getSize() { return size; }
            }
            """.trimIndent()
        )
        val contract = cu.findAll(JmlContract::class.java).first()
        assertThat(VerifyAgainstParent().isCallableForNode(contract)).isTrue()
        val result = VerifyAgainstParent().createCodeAction("file:///A.java", contract)
        assertThat(result!!.left.command).isEqualTo("jml.verify.liskov")
        assertThat(result.left.arguments).containsExactly(contract.hashCode())
    }

    @Test
    fun `no action for non contract nodes`() {
        val action = VerifyAgainstParent()
        assertThat(action.createCodeAction("file:///A.java", NameExpr("x"))).isNull()
        assertThat(action.isCallableForNode(NameExpr("x"))).isFalse()
    }

    @Test
    fun `executes without value`() {
        val result = VerifyAgainstParent().execute(JmlLanguageServer(), null).get()
        assertThat(result).isNull()
    }

    @Test
    fun `executes with value`() {
        val result = VerifyAgainstParent().execute(JmlLanguageServer(), listOf(42)).get()
        assertThat(result).isEqualTo("")
    }
}

class CreateKeyProjectFileTest {
    @Test
    fun `creates project file once`(@TempDir dir: Path) {
        val rootUri = dir.toUri().toString()
        val server = initializedServer(rootUri)
        val action = CreateKeyProjectFile()
        assertThat(action.execute(server, null).get()).isEqualTo("")
        assertThat(dir.resolve("project.key").toFile().exists()).isTrue()
        assertThat(action.execute(server, null).get()).isEqualTo("Project key already exists")
    }
}

class LspActionDefaultsTest {
    private val action = object : LspAction {
        override val title: String = "defaults"
        override fun execute(server: JmlLanguageServer, value: List<Any>?): CompletableFuture<Any> =
            CompletableFuture.completedFuture("")
    }

    @Test
    fun `default members behave as documented`() {
        assertThat(action.id).isEqualTo(action.javaClass.name)
        assertThat(action.kind).isNull()
        assertThat(action.isCallableForNode(NameExpr("x"))).isTrue()
        assertThat(action.command()).isEqualTo(org.eclipse.lsp4j.Command("defaults", action.id, null))
        assertThat(action.command(listOf("a"))).isEqualTo(org.eclipse.lsp4j.Command("defaults", action.id, listOf("a")))
    }

    @Test
    fun `default action and lens are absent`() {
        assertThat(action.createCodeAction("file:///A.java", NameExpr("x"))).isNull()
        assertThat(action.createCodeLens("file:///A.java", NameExpr("x"))).isNull()
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.jml2java

import com.github.javaparser.*
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.TypeSolverBuilder
import com.github.javaparser.utils.SourceRoot
import com.google.common.truth.Truth
import jjbmc.JJBMCOptions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

internal class Jml2JavaExpressionTranslatorTest {
    @ParameterizedTest
    @MethodSource("findCompleteTranslationTests")
    @Throws(IOException::class)
    fun testTranslation(check: ParseResult<CompilationUnit>) {
        if (!check.isSuccessful()) {
            check.getProblems().forEach(Consumer { x: Problem? -> System.err.println(x) })
            error("Error during parsing")
        }

        val cu = check.getResult().get()
        val actual = Jml2JavaFacade.translate(cu, JJBMCOptions())
        println(actual)

        val originalPath = cu.getStorage().get().getPath().toAbsolutePath()
        val path: Path = expectedSources.resolve(source.relativize(originalPath))

        println(path)
        val text = Jml2JavaFacade.pprint(actual)

        val tmp: Path = actualSources.resolve(source.relativize(originalPath))
        Files.createDirectories(tmp.getParent())
        Files.writeString(tmp, text)
        Assertions.assertEquals(Files.readString(path), text)
    }

    @ParameterizedTest
    @MethodSource("readExpressionTests")
    fun testTranslation(expr: String, expected: String, mode: TranslationMode) {
        val e = StaticJavaParser.parseJmlExpression<Expression>(expr)
        parent.addAndGetStatement(e)
        Jml2JavaExpressionTranslator.counter.set(0)
        val r = Jml2JavaFacade.translate(e, mode)
        val actual = (
            r.necessaryVars.stream().map<String?> { o: Statement? -> Objects.toString(o) }
            .collect(Collectors.joining("\n")) + "\n" +
            BlockStmt(r.statements) + "\n" + r.value
        )
        Truth.assertThat(actual.replace("\\s+".toRegex(), " ").trim { it <= ' ' })
            .isEqualTo(expected.replace("\\s+".toRegex(), " ").trim { it <= ' ' })
    }

    companion object {
        private val base: Path = Paths.get("src", "test", "resources", "unit-tests")
        private val source: Path = base.resolve("input").toAbsolutePath()
        private val expectedSources: Path = base.resolve("expected").toAbsolutePath()
        private val actualSources: Path = base.resolve("actual").toAbsolutePath()

        @Throws(IOException::class)
        fun readExpressionTests(): Sequence<Arguments> {
            val yaml: Yaml = Yaml()
            Files.newBufferedReader(base.resolve("expr-translation-tests.yml")).use { fw ->
                val obj: MutableList<MutableMap<String, String>> = yaml.load(fw)
                return obj.asSequence().map {
                    val mode = TranslationMode.valueOf(it!!.getOrDefault("mode", TranslationMode.ASSERT.toString())!!)
                    Arguments.of(it["input"], it["expected"], mode)
                }
            }
        }

        var parent: BlockStmt

        init {
            // Using an own JavaParser to configure a SymbolSolver. This is necessary, because for type resolution
            // of an expression, it is required that a symbolsolver is reachable.

            val config = ParserConfiguration()
            config.setSymbolResolver(
                JavaSymbolSolver(TypeSolverBuilder().withCurrentJRE().build())
            )
            val jp = JavaParser(config)
            val cu =
                jp.parse(" public class A { void foo() {} } ").getResult().get()
            parent = cu.getType(0)
                .asClassOrInterfaceDeclaration()
                .getMethodsByName("foo")[0]
                .getBody()
                .get()
        }

        @Throws(IOException::class)
        fun findCompleteTranslationTests(): Sequence<Arguments> {
            val config = ParserConfiguration()
            config.setProcessJml(true)
            config.setJmlKeys(listOf(listOf("jjbmc")))

            config.setSymbolResolver(
                JavaSymbolSolver(
                    TypeSolverBuilder().withSourceCode(source).withCurrentJRE().build()
                )
            )

            val sourceRoot = SourceRoot(source, config)
            return sourceRoot.tryToParse().asSequence().map { Arguments.of(it) }
        }
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.symbols

import com.google.common.truth.Truth.assertThat
import io.github.jmltoolkit.lsp.JmlTestSupport
import org.eclipse.lsp4j.DocumentSymbol
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths

class JmlCatchSymbolsTest {

    private fun symbolsOf(code: String, fileName: String = "Test.java"): List<DocumentSymbol> {
        val cu = JmlTestSupport.parse(code)
        cu.setStorage(Paths.get(fileName))
        return cu.accept(JmlCatchSymbols(), null) ?: emptyList()
    }

    private fun symbolsOfFile(file: File): List<DocumentSymbol> {
        val result = JmlTestSupport.javaParser().parse(file)
        check(result.isSuccessful) {
            "Parsing failed:\n" + result.problems.joinToString("\n") { it.verboseMessage }
        }
        val cu = result.result.get()
        cu.setStorage(file.toPath())
        return cu.accept(JmlCatchSymbols(), null) ?: emptyList()
    }

    private fun allNames(symbols: List<DocumentSymbol>, acc: MutableList<String> = mutableListOf()): List<String> {
        for (symbol in symbols) {
            acc.add(symbol.name)
            symbol.children?.let { allNames(it, acc) }
        }
        return acc
    }

    @Test
    fun `root symbol is the file`() {
        val symbols = symbolsOf("public class A { }")
        assertThat(symbols.map { it.name }).containsExactly("Test.java")
        assertThat(symbols.first().kind).isEqualTo(org.eclipse.lsp4j.SymbolKind.File)
    }

    @Test
    fun `collects class method constructor and contract symbols`() {
        val symbols = symbolsOf(
            """
            public class A {
                public A() {
                    //@ assert true;
                }

                /*@ public normal_behavior
                    requires size > 0;
                    ensures \result == size;
                    assignable \nothing;
                @*/
                public int getSize() { return size; }

                int size;
            }
            """.trimIndent()
        )
        val names = allNames(symbols)
        assertThat(names).containsAtLeast("A", "getSize", "size")
        assertThat(names).contains("Contract: normal_behavior")
        assertThat(names).contains("Clause requires")
        assertThat(symbols.first().children.map { it.name }).contains("A")
    }

    @Test
    fun `collects symbols from workspace fixtures`() {
        val stack = symbolsOfFile(File("workspace/Stack.java"))
        val stackNames = allNames(stack)
        assertThat(stackNames).contains("Stack.java")
        assertThat(stackNames)
            .containsAtLeast("Stack", "push", "pop", "peek", "isEmpty", "isFull", "getSize")

        val declarations = symbolsOfFile(File("workspace/Declarations.java"))
        val declarationNames = allNames(declarations)
        assertThat(declarationNames).containsAtLeast("Declarations.java", "foo", "abc")
    }

    @Test
    fun `collects enum symbols`() {
        val symbols = symbolsOf(
            """
            public enum Color {
                RED, GREEN;
            }
            """.trimIndent()
        )
        val names = allNames(symbols)
        assertThat(names).containsAtLeast("Color", "RED", "GREEN")
    }

    @Test
    fun `collects annotation symbols`() {
        val symbols = symbolsOf(
            """
            public @interface Marker {
                int priority() default 0;
            }
            """.trimIndent()
        )
        val names = allNames(symbols)
        assertThat(names).containsAtLeast("Marker", "priority")
    }

    @Test
    fun `collects interface symbols`() {
        val symbols = symbolsOf(
            """
            public interface Shape {
                /*@ requires size > 0; @*/
                int area();
            }
            """.trimIndent()
        )
        val names = allNames(symbols)
        assertThat(names).containsAtLeast("Shape", "area", "Contract: null")
    }
}

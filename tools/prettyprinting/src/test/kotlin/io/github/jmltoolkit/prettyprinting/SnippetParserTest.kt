/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.stmt.Statement
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SnippetParserTest {
    private val parser = SnippetParser()

    @Test
    fun `detects a compilation unit`() {
        val result = parser.parse("class A { int f() { return 1; } }")
        assertThat(result.kind).isEqualTo(ParsedKind.COMPILATION_UNIT)
        assertThat(result.nodes.single()).isInstanceOf(CompilationUnit::class.java)
    }

    @Test
    fun `detects a member`() {
        val result = parser.parse("int f() { return 1; }")
        assertThat(result.kind).isEqualTo(ParsedKind.MEMBER)
        assertThat(result.nodes.single().toString()).contains("int f()")
    }

    @Test
    fun `field wins over local variable`() {
        assertThat(parser.parse("int x = 1;").kind).isEqualTo(ParsedKind.MEMBER)
    }

    @Test
    fun `detects a statement`() {
        val result = parser.parse("return 1;")
        assertThat(result.kind).isEqualTo(ParsedKind.STATEMENT)
        assertThat(result.nodes.single()).isInstanceOf(Statement::class.java)
    }

    @Test
    fun `detects an expression`() {
        val result = parser.parse("1 + 2")
        assertThat(result.kind).isEqualTo(ParsedKind.EXPRESSION)
        assertThat(result.nodes.single()).isInstanceOf(Expression::class.java)
    }

    @Test
    fun `respects an explicit snippet kind`() {
        assertThat(parser.parse("1 + 2", kind = SnippetKind.CU).kind).isEqualTo(ParsedKind.PLAIN)
        assertThat(parser.parse("1 + 2", kind = SnippetKind.EXPR).kind).isEqualTo(ParsedKind.EXPRESSION)
    }

    @Test
    fun `parses jml expressions`() {
        val result = parser.parse("\\old(x) > 0", language = Language.JML)
        assertThat(result.kind).isEqualTo(ParsedKind.JML_EXPRESSION)
        assertThat(result.nodes.single().toString()).isEqualTo("\\old(x) > 0")
    }

    @Test
    fun `parses jml clauses`() {
        val result = parser.parse("requires x > 0;", language = Language.JML)
        assertThat(result.kind).isEqualTo(ParsedKind.JML_CLAUSE)
        assertThat(result.nodes.single().toString()).contains("requires x > 0;")
    }

    @Test
    fun `parses several jml clauses`() {
        val result = parser.parse("requires x > 0;\nensures x < 10;", language = Language.JML)
        assertThat(result.kind).isEqualTo(ParsedKind.JML_CLAUSE)
        assertThat(result.nodes).hasSize(2)
    }

    @Test
    fun `falls back to plain for invalid input`() {
        val result = parser.parse("class {")
        assertThat(result.kind).isEqualTo(ParsedKind.PLAIN)
        assertThat(result.nodes).isEmpty()
        assertThat(result.tokenRanges).isEmpty()
    }

    @Test
    fun `works out the exact snippet token range`() {
        val text = "/*@ requires x > 0; @*/\nint f() { return 1; }"
        val result = parser.parse(text)
        assertThat(result.kind).isEqualTo(ParsedKind.MEMBER)
        assertThat(result.stripWrapperNewlines).isTrue()
        val tokens = result.tokenRanges.single().iterator().asSequence().joinToString("") { it.text }
        assertThat(tokens.trim()).isEqualTo(text.trim())
    }
}

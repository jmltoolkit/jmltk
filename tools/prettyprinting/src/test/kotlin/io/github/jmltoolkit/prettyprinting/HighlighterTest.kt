/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HighlighterTest {
    private val parser = SnippetParser()
    private val highlighter = Highlighter()

    private fun spans(src: String): List<Span> = highlighter.highlight(parser.parse(src))

    private fun styleOf(src: String, text: String): StyleClass = spans(src).first { it.text == text }.style

    @Test
    fun `concatenation preserves the source exactly`() {
        val src = "public class A { int f() { return 1; } }"
        assertThat(spans(src).joinToString("") { it.text }).isEqualTo(src)
    }

    @Test
    fun `classifies java tokens`() {
        val src = "public class A { int f() { return 1; } }"
        assertThat(styleOf(src, "public")).isEqualTo(StyleClass.KEYWORD_DECLARATION)
        assertThat(styleOf(src, "class")).isEqualTo(StyleClass.KEYWORD_DECLARATION)
        assertThat(styleOf(src, "A")).isEqualTo(StyleClass.NAME_CLASS)
        assertThat(styleOf(src, "int")).isEqualTo(StyleClass.KEYWORD_TYPE)
        assertThat(styleOf(src, "f")).isEqualTo(StyleClass.NAME_FUNCTION)
        assertThat(styleOf(src, "return")).isEqualTo(StyleClass.KEYWORD)
        assertThat(styleOf(src, "1")).isEqualTo(StyleClass.NUMBER_INTEGER)
    }

    @Test
    fun `classifies names after dots and annotations`() {
        val src = "class A { @Override public String toString() { return this.x.y; } }"
        assertThat(styleOf(src, "@")).isEqualTo(StyleClass.NAME_DECORATOR)
        assertThat(styleOf(src, "Override")).isEqualTo(StyleClass.NAME_DECORATOR)
        assertThat(styleOf(src, "y")).isEqualTo(StyleClass.NAME_ATTRIBUTE)
    }

    @Test
    fun `classifies number bases`() {
        val src = "class A { int a = 0xFF; int b = 0b101; int c = 0755; float d = 1.5f; }"
        assertThat(styleOf(src, "0xFF")).isEqualTo(StyleClass.NUMBER_HEX)
        assertThat(styleOf(src, "0b101")).isEqualTo(StyleClass.NUMBER_BINARY)
        assertThat(styleOf(src, "0755")).isEqualTo(StyleClass.NUMBER_OCT)
        assertThat(styleOf(src, "1.5f")).isEqualTo(StyleClass.NUMBER_FLOAT)
    }

    @Test
    fun `splits string escapes`() {
        val src = "class A { String s = \"a\\nb\"; }"
        val spans = spans(src)
        assertThat(spans.joinToString("") { it.text }).isEqualTo(src)
        assertThat(spans.filter { it.style == StyleClass.STRING_ESCAPE }.map { it.text })
            .containsExactly("\\n")
    }

    @Test
    fun `classifies comments`() {
        val src = "// one\nclass A { /** doc */ /* block */ int f() { return 1; } }"
        assertThat(styleOf(src, "// one")).isEqualTo(StyleClass.COMMENT_SINGLE)
        assertThat(styleOf(src, "/** doc */")).isEqualTo(StyleClass.COMMENT_SPECIAL)
        assertThat(styleOf(src, "/* block */")).isEqualTo(StyleClass.COMMENT_MULTILINE)
    }

    @Test
    fun `lexes jml inside java comments`() {
        val src = "class A { /*@ requires x > 0; @*/ int f() { return 1; } }"
        val spans = spans(src)
        assertThat(spans.joinToString("") { it.text }).isEqualTo(src)
        assertThat(styleOf(src, "/*@")).isEqualTo(StyleClass.COMMENT_MULTILINE)
        assertThat(styleOf(src, "@*/")).isEqualTo(StyleClass.COMMENT_MULTILINE)
        assertThat(styleOf(src, "requires")).isEqualTo(StyleClass.KEYWORD_DECLARATION)
        assertThat(styleOf(src, ">")).isEqualTo(StyleClass.OPERATOR)
        assertThat(styleOf(src, "0")).isEqualTo(StyleClass.NUMBER_INTEGER)
    }

    @Test
    fun `keeps a leading jml line comment in a member snippet`() {
        val src = "//@ requires x > 0;\nint f() { return 1; }"
        val spans = spans(src)
        assertThat(spans.joinToString("") { it.text }).isEqualTo(src)
        assertThat(spans.first().style).isEqualTo(StyleClass.COMMENT_SINGLE)
        assertThat(styleOf(src, "requires")).isEqualTo(StyleClass.KEYWORD_DECLARATION)
    }

    @Test
    fun `lexes jml snippets with jml keywords`() {
        val src = "requires \\old(x) > 0;"
        val spans = highlighter.highlight(parser.parse(src, language = Language.JML))
        assertThat(spans.joinToString("") { it.text }).isEqualTo(src)
        assertThat(spans.first { it.text == "requires" }.style).isEqualTo(StyleClass.KEYWORD_DECLARATION)
        assertThat(spans.first { it.text == "\\old" }.style).isEqualTo(StyleClass.KEYWORD)
    }

    @Test
    fun `falls back to one plain span`() {
        val src = "class {"
        val spans = spans(src)
        assertThat(spans).containsExactly(Span(src, StyleClass.TEXT))
    }
}

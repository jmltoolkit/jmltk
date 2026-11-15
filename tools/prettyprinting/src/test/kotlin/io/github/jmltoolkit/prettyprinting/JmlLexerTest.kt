/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JmlLexerTest {
    @Test
    fun `lexes clause keywords operators and backslash names`() {
        val input = "requires x > 0 && \\old(a) != null;"
        val spans = JmlLexer().lex(input)

        assertThat(spans.joinToString("") { it.text }).isEqualTo(input)
        assertThat(styled(spans)).containsExactly(
            "requires" to StyleClass.KEYWORD_DECLARATION,
            "x" to StyleClass.NAME,
            ">" to StyleClass.OPERATOR,
            "0" to StyleClass.NUMBER_INTEGER,
            "&&" to StyleClass.OPERATOR,
            "\\old" to StyleClass.KEYWORD,
            "(" to StyleClass.PUNCTUATION,
            "a" to StyleClass.NAME,
            ")" to StyleClass.PUNCTUATION,
            "!=" to StyleClass.OPERATOR,
            "null" to StyleClass.KEYWORD_CONSTANT,
            ";" to StyleClass.PUNCTUATION,
        )
    }

    @Test
    fun `lexes jml specific operators and numbers`() {
        val spans = JmlLexer().lex("\\forall int i; i < n ==> a[i] == 0x10;")
        assertThat(styled(spans)).contains(
            "\\forall" to StyleClass.KEYWORD,
            "int" to StyleClass.KEYWORD_TYPE,
            "==>" to StyleClass.OPERATOR,
            "0x10" to StyleClass.NUMBER_HEX,
            "[" to StyleClass.OPERATOR,
            "]" to StyleClass.OPERATOR,
        )
    }

    @Test
    fun `splits string escapes`() {
        val input = "\"a\\nb\""
        val spans = JmlLexer().lex(input)
        assertThat(spans.joinToString("") { it.text }).isEqualTo(input)
        assertThat(spans.filter { it.style == StyleClass.STRING_ESCAPE }.map { it.text })
            .containsExactly("\\n")
    }

    @Test
    fun `lexes comments`() {
        val spans = JmlLexer().lex("// note\nrequires true; /* done */")
        assertThat(spans.filter { it.style == StyleClass.COMMENT_SINGLE }.map { it.text })
            .containsExactly("// note")
        assertThat(spans.filter { it.style == StyleClass.COMMENT_MULTILINE }.map { it.text })
            .containsExactly("/* done */")
    }

    private fun styled(spans: List<Span>) = spans.filter { it.style != StyleClass.TEXT }.map { it.text to it.style }
}

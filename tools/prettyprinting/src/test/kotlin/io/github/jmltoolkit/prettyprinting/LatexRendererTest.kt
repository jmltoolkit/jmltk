/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LatexRendererTest {
    @Test
    fun `minted mode emits PYG macros and groups equal styles`() {
        val spans = listOf(
            Span("public", StyleClass.KEYWORD_DECLARATION),
            Span(" "),
            Span("class", StyleClass.KEYWORD_DECLARATION),
        )
        assertThat(LatexRenderer(raw = false).render(spans))
            .isEqualTo("\\PYG{kd}{public} \\PYG{kd}{class}")
    }

    @Test
    fun `minted mode escapes latex specials`() {
        val spans = listOf(
            Span("{", StyleClass.PUNCTUATION),
            Span("a_b", StyleClass.NAME),
            Span("\\", StyleClass.STRING),
            Span("<", StyleClass.OPERATOR),
        )
        assertThat(LatexRenderer(raw = false).render(spans))
            .isEqualTo("\\PYG{p}{\\PYGZob{}}\\PYG{n}{a\\PYGZus{}b}\\PYG{s}{\\PYGZbs{}}\\PYG{o}{\\PYGZlt{}}")
    }

    @Test
    fun `raw mode emits semantic macros`() {
        val spans = listOf(
            Span("public", StyleClass.KEYWORD_DECLARATION),
            Span(" "),
            Span("// x", StyleClass.COMMENT_SINGLE),
        )
        assertThat(LatexRenderer(raw = true).render(spans))
            .isEqualTo("\\jmlkd{public} \\jmlcOne{// x}")
    }

    @Test
    fun `raw mode escapes latex specials`() {
        val spans = listOf(Span("{a_b}", StyleClass.NAME))
        assertThat(LatexRenderer(raw = true).render(spans))
            .isEqualTo("\\jmln{\\{a\\_b\\}}")
    }

    @Test
    fun `raw preamble defines colors and macros`() {
        val preamble = LatexRenderer(raw = true).preamble()
        assertThat(preamble).contains("\\definecolor{jmltkKd}{HTML}{008000}")
        assertThat(preamble).contains("\\newcommand{\\jmlkd}[1]{\\textcolor{jmltkKd}{\\textbf{#1}}}")
        assertThat(preamble).contains("\\definecolor{jmltkKt}{HTML}{B00040}")
        assertThat(preamble).contains("\\newcommand{\\jmlcOne}[1]{\\textcolor{jmltkCOne}{\\textit{#1}}}")
    }
}

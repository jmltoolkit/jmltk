/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TextHtmlRendererTest {
    @Test
    fun `text adds exactly one trailing newline`() {
        assertThat(TextRenderer().render("a\nb")).isEqualTo("a\nb\n")
        assertThat(TextRenderer().render("a\n")).isEqualTo("a\n")
        assertThat(TextRenderer().render("a\r\nb")).isEqualTo("a\r\nb\n")
    }

    @Test
    fun `html fragment uses pygments classes and escapes`() {
        val spans = listOf(
            Span("public", StyleClass.KEYWORD_DECLARATION),
            Span(" "),
            Span("<A>", StyleClass.NAME),
            Span("&", StyleClass.OPERATOR),
        )
        assertThat(HtmlRenderer().renderFragment(spans)).isEqualTo(
            "<pre class=\"highlight\"><code><span class=\"kd\">public</span> <span class=\"n\">&lt;A&gt;</span><span class=\"o\">&amp;</span></code></pre>\n"
        )
    }

    @Test
    fun `html keeps unicode intact`() {
        val spans = listOf(Span("Grüße", StyleClass.STRING), Span(" = "), Span("λ", StyleClass.NAME))
        val html = HtmlRenderer().renderFragment(spans)
        assertThat(html).contains("Grüße")
        assertThat(html).contains("λ")
    }

    @Test
    fun `html full document inlines user css`() {
        val html = HtmlRenderer().renderDocument("<pre/>", "code { color: red }")
        assertThat(html).startsWith("<!DOCTYPE html>")
        assertThat(html).contains("<style>\ncode { color: red }\n</style>")
        assertThat(html).contains("<pre/>")
    }
}

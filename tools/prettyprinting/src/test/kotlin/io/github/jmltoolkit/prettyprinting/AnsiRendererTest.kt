/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AnsiRendererTest {
    @Test
    fun `emits bold 256-color escapes`() {
        val out = AnsiRenderer().render(listOf(Span("public", StyleClass.KEYWORD_DECLARATION)))
        assertThat(out).isEqualTo("\u001B[1;38;5;2mpublic\u001B[0m")
    }

    @Test
    fun `emits non-bold and italic escapes`() {
        val type = AnsiRenderer().render(listOf(Span("int", StyleClass.KEYWORD_TYPE)))
        assertThat(type).isEqualTo("\u001B[38;5;125mint\u001B[0m")

        val comment = AnsiRenderer().render(listOf(Span("// x", StyleClass.COMMENT_SINGLE)))
        assertThat(comment).isEqualTo("\u001B[3;38;5;66m// x\u001B[0m")
    }

    @Test
    fun `leaves whitespace and text unescaped`() {
        assertThat(AnsiRenderer().render(listOf(Span(" "), Span("plain")))).isEqualTo(" plain")
    }

    @Test
    fun `disabling color returns plain text`() {
        assertThat(AnsiRenderer(color = false).render(listOf(Span("public", StyleClass.KEYWORD))))
            .isEqualTo("public")
    }

    @Test
    fun `honours no color flag and environment`() {
        assertThat(ColorSupport.enabled(noColorFlag = false) { null }).isTrue()
        assertThat(ColorSupport.enabled(noColorFlag = false) { "1" }).isFalse()
        assertThat(ColorSupport.enabled(noColorFlag = true) { null }).isFalse()
    }
}

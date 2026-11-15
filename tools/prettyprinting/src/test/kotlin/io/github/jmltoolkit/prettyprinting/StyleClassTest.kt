/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StyleClassTest {
    @Test
    fun `exposes pygments css names`() {
        assertThat(StyleClass.KEYWORD_DECLARATION.cssClass).isEqualTo("kd")
        assertThat(StyleClass.KEYWORD_TYPE.cssClass).isEqualTo("kt")
        assertThat(StyleClass.NAME_FUNCTION.cssClass).isEqualTo("nf")
        assertThat(StyleClass.COMMENT_SINGLE.cssClass).isEqualTo("c1")
        assertThat(StyleClass.TEXT.cssClass).isEmpty()
    }

    @Test
    fun `renames digit classes for latex`() {
        assertThat(StyleClass.COMMENT_SINGLE.latexMacro).isEqualTo("cOne")
        assertThat(StyleClass.KEYWORD_DECLARATION.latexMacro).isEqualTo("kd")
        assertThat(StyleClass.STRING.latexMacro).isEqualTo("s")
    }

    @Test
    fun `maps pygments default colors to xterm 256`() {
        assertThat(xterm256(0x008000)).isEqualTo(2)
        assertThat(xterm256(0x666666)).isEqualTo(241)
        assertThat(xterm256(0xB00040)).isEqualTo(125)
        assertThat(xterm256(0x3D7B7B)).isEqualTo(66)
    }

    @Test
    fun `has colors and attributes for java classes`() {
        assertThat(pygmentsDefault[StyleClass.KEYWORD]).isEqualTo(StyleAttributes(0x008000, bold = true))
        assertThat(pygmentsDefault[StyleClass.KEYWORD_TYPE]).isEqualTo(StyleAttributes(0xB00040))
        assertThat(pygmentsDefault[StyleClass.COMMENT_MULTILINE]).isEqualTo(
            StyleAttributes(0x3D7B7B, italic = true)
        )
    }
}

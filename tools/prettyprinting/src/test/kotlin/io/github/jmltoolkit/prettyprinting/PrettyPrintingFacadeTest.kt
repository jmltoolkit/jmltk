/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PrettyPrintingFacadeTest {
    private val facade = PrettyPrintingFacade()

    @Test
    fun `pretty prints a compilation unit`() {
        val out = facade.render(
            "public class A { int f(){return 1;} }",
            RenderOptions(prettyPrint = true),
        )
        val expected = """
            public class A {
              int f() {
                return 1;
              }
            }
        """.trimIndent() + "\n"
        assertThat(out).isEqualTo(expected)
    }

    @Test
    fun `renders html with pygments classes`() {
        val out = facade.render("class A {}", RenderOptions(format = OutputFormat.HTML))
        assertThat(out).contains("<span class=\"kd\">class</span>")
        assertThat(out).contains("<span class=\"nc\">A</span>")
    }

    @Test
    fun `renders ansi`() {
        val out = facade.render("public class A {}", RenderOptions(format = OutputFormat.ANSI))
        assertThat(out).contains("\u001B[1;38;5;2mpublic\u001B[0m")
    }

    @Test
    fun `renders raw latex`() {
        val out = facade.render(
            "public class A {}",
            RenderOptions(format = OutputFormat.LATEX, latexRaw = true),
        )
        assertThat(out).contains("\\jmlkd{public}")
        assertThat(out).contains("\\jmlkd{class}")
    }

    @Test
    fun `highlights jml snippets`() {
        val out = facade.render(
            "requires x > 0;",
            RenderOptions(language = Language.JML, format = OutputFormat.HTML),
        )
        assertThat(out).contains("<span class=\"kd\">requires</span>")
        assertThat(out).contains("<span class=\"o\">&gt;</span>")
        assertThat(out).contains("<span class=\"mi\">0</span>")
    }

    @Test
    fun `falls back to plain text for invalid input`() {
        val out = facade.render("class {", RenderOptions(format = OutputFormat.HTML))
        assertThat(out).contains("class {")
    }

    @Test
    fun `pretty printing invalid input throws`() {
        assertThatThrownBy { facade.render("class {", RenderOptions(prettyPrint = true)) }
            .isInstanceOf(PrettyPrintingException::class.java)
    }

    @Test
    fun `style emits usemintedstyle in minted mode`() {
        val out = facade.render(
            "class A {}",
            RenderOptions(format = OutputFormat.LATEX, style = "monokai"),
        )
        assertThat(out).startsWith("\\usemintedstyle{monokai}\n")
    }
}

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
    fun `pretty prints every member of a snippet`() {
        val out = facade.render("int a = 1;\nint b = 2;", RenderOptions(prettyPrint = true))
        assertThat(out).contains("int a = 1;")
        assertThat(out).contains("int b = 2;")
    }

    @Test
    fun `pretty prints class level jml clauses with the member`() {
        val out = facade.render("/*@ invariant x > 0; @*/\nint x = 1;", RenderOptions(prettyPrint = true))
        assertThat(out).contains("invariant x > 0")
        assertThat(out).contains("int x = 1;")
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
    fun `warns on plain fallback`() {
        val warnings = mutableListOf<String>()
        val warningFacade = PrettyPrintingFacade(warning = { warnings.add(it) })
        val out = warningFacade.render("class {", RenderOptions(format = OutputFormat.HTML))
        assertThat(out).contains("class {")
        assertThat(warnings).hasSize(1)
    }

    @Test
    fun `html full with several inputs has one document`() {
        val out = facade.renderAll(
            listOf("class A {}", "class B {}"),
            RenderOptions(format = OutputFormat.HTML, htmlFull = true),
        )
        assertThat(out).containsOnlyOnce("<!DOCTYPE html>")
        assertThat(out).containsOnlyOnce("<html>")
        assertThat(out).contains("<span class=\"nc\">A</span>")
        assertThat(out).contains("<span class=\"nc\">B</span>")
    }

    @Test
    fun `renderAll separates chunks with a blank line`() {
        val out = facade.renderAll(
            listOf("class A {}", "class B {}"),
            RenderOptions(format = OutputFormat.ANSI, color = false),
        )
        assertThat(out).contains("}\n\n")
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

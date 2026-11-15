/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class InputReaderTest {
    @Test
    fun `reads stdin when no files are given`() {
        val reader = InputReader(stdin = { "from stdin" }, terminal = { false })
        assertThat(reader.read(emptyList())).containsExactly("from stdin")
    }

    @Test
    fun `rejects empty input from a terminal`() {
        val reader = InputReader(stdin = { "unused" }, terminal = { true })
        assertThatThrownBy { reader.read(emptyList()) }.isInstanceOf(InputException::class.java)
    }

    @Test
    fun `reads files in order and dash once`(@TempDir dir: Path) {
        val a = dir.resolve("A.java").toFile().apply { writeText("class A {}") }
        val b = dir.resolve("B.java").toFile().apply { writeText("class B {}") }
        val reader = InputReader(stdin = { "class S {}" }, terminal = { false })
        assertThat(reader.read(listOf(a, b))).containsExactly("class A {}", "class B {}")
        assertThat(reader.read(listOf(a, File("-"), b)))
            .containsExactly("class A {}", "class S {}", "class B {}")
    }

    @Test
    fun `rejects repeated stdin`() {
        val reader = InputReader(stdin = { "x" }, terminal = { false })
        assertThatThrownBy { reader.read(listOf(File("-"), File("-"))) }
            .isInstanceOf(InputException::class.java)
    }
}

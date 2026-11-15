/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JmlFmtTest {
    @Test
    fun `formats a whole compilation unit`() {
        val out = JmlFmt().pp("public class A { int f(){return 1;} }", 100)
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
    fun `preserves jml comments`() {
        val out = JmlFmt().pp("public class A { /*@ requires x > 0; @*/ int f(){return 1;} }", 100)
        assertThat(out).contains("/*@ requires x > 0; @*/")
        assertThat(out).contains("int f()")
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CodeActionCollectorTest {

    @Test
    fun `collect gathers actions for all matching nodes`() {
        val cu = JmlTestSupport.parse(
            """
            class A {
                int size;

                /*@ requires size > 0; @*/
                int getSize() { return size; }
            }
            """.trimIndent()
        )
        val actions = CodeActionCollector.collect("file:///A.java", cu)
        val titles = actions.mapNotNull { it.left?.title ?: it.right?.title }
        assertThat(titles).contains("Add a behavior keyword")
        assertThat(titles).contains("Add a name to clause or contract")
        assertThat(titles).contains("Check expression for well-definedness")
        assertThat(titles).contains("Add Quantification")
    }

    @Test
    fun `collect ignores nodes with no matching action`() {
        val cu = JmlTestSupport.parse("public class A { }")
        assertThat(CodeActionCollector.collect("file:///A.java", cu)).isEmpty()
    }

    @Test
    fun `collect with null node returns empty list`() {
        assertThat(CodeActionCollector.collect("file:///A.java", null)).isEmpty()
    }

    @Test
    fun `createCodeAction filters actions per node`() {
        val cu = JmlTestSupport.parse(
            """
            class A {
                int size;

                /*@ requires size > 0; @*/
                int getSize() { return size; }
            }
            """.trimIndent()
        )
        val type = cu.types.first()
        val actions = CodeActionCollector.createCodeAction("file:///A.java", type)
        assertThat(actions).isEmpty()
    }
}

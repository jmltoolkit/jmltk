/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.symbols

import com.google.common.truth.Truth.assertThat
import io.github.jmltoolkit.lsp.Uri
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class KeyCatchSymbolsTest {

    @Test
    fun `gathers symbols from a key file`(@TempDir dir: Path) {
        val keyFile = dir.resolve("sorts.key").also {
            it.toFile().writeText(
                """
                \settings {
                    "keyVisibility=visible"
                }

                \sorts {
                    Nat;
                }

                \functions {
                    int f(Nat);
                }

                \predicates {
                    P(Nat);
                }

                \schemaVariables {
                    \term Nat n;
                }

                \rules (testChoice:testValue) {
                    test_rule {
                        \find(n)
                        \closegoal
                    }
                }
                """.trimIndent()
            )
        }

        val symbols = KeyCatchSymbols(Uri("file://${keyFile.toAbsolutePath()}")).run()
        assertThat(symbols).isNotEmpty()
        val names = symbols.mapNotNull { it.right?.name }
        assertThat(names).contains("Sorts")
        assertThat(names).contains("Functions")
        assertThat(names).contains("Preferences")
        assertThat(names.any { it.startsWith("Rules") }).isTrue()

        val sorts = symbols.mapNotNull { it.right }.first { it.name == "Sorts" }
        assertThat(sorts.children.map { it.name }).contains("Nat")
        val rules = symbols.mapNotNull { it.right }.first { it.name.startsWith("Rules") }
        assertThat(rules.children.map { it.name }).contains("test_rule")
    }
}

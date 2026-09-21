/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
import io.github.jmltoolkit.lint.JmlLintingConfig
import io.github.jmltoolkit.lint.JmlLintingFacade
import io.github.jmltoolkit.utils.TestWithJavaParser
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

/**
 * @author Alexander Weigl
 * @version 1 (14.10.22)
 */
internal class LinterTest : TestWithJavaParser() {
    @Test
    fun everythingWrong() {
        val result = parser.parse(javaClass.getResourceAsStream("EverythingWrong.java"))
        Assumptions.assumeTrue(result.isSuccessful)
        val actual = JmlLintingFacade(JmlLintingConfig()).lint(listOf(result.result.get()))

        for (lintProblem in actual) {
            println(lintProblem)
        }
    }

    @Test
    fun nullityDefaults() {
        val result = parser.parse(javaClass.getResourceAsStream("NullityDefaults.java"))
        result.problems.forEach { System.err.println(it) }
        Assumptions.assumeTrue(result.isSuccessful)
        val actual = JmlLintingFacade(JmlLintingConfig()).lint(listOf(result.result.get()))
        val messages = actual.map { it.message }

        // error: both default nullity declarations at once
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.any { it.contains("both non_null_by_default and nullable_by_default") },
            "Expected conflict error, got: $messages"
        )
        // error: default nullity modifiers only on classes
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.any { it.contains("only allowed on class declarations") },
            "Expected misplaced modifier error, got: $messages"
        )
        // hint: not inherited by derived classes
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.any { it.contains("not inherited by derived classes") },
            "Expected non-inheritance hint, got: $messages"
        )
    }
}

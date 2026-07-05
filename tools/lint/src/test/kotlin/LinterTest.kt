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
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.wd

import com.github.javaparser.JavaParser
import io.github.jmltoolkit.smt.Z3
import io.github.jmltoolkit.smt.solver.JavaSmtSolver
import io.github.jmltoolkit.wd.WdFacade.isWelldefined
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * @author Alexander Weigl
 * @version 1 (14.06.22)
 */
internal class WDVisitorExprTest {
    private val parser = JavaParser()

    @ParameterizedTest
    @ValueSource(
        strings = [
            "true",
            "false",
            "1 == 2",
            "5 * 2 == 2",
            "42 / 2",
            "'a' + 'c'",
            "x - 2 == 3",
            "'x' == 65",
            "'x' != 65",
            "'x' != 65",
            "new Object().equals(null)",
            "++(new Integer(0))",
            "i++",
            "\"a\" + \"c\""]
    )
    fun wdExpression(expr: String) {
        Assumptions.assumeTrue(Z3.z3Installed() || JavaSmtSolver.isAvailable)
        Assertions.assertTrue(isWelldefined(parser, expr))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "x / 0", "x / (1 - 1)"
        ]
    )
    fun wdExpressionError(expr: String) {
        Assumptions.assumeTrue(Z3.z3Installed() || JavaSmtSolver.isAvailable)
        Assertions.assertFalse(isWelldefined(parser, expr))
    }
}

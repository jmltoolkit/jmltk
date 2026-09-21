package io.github.jmltoolkit.wd

import com.github.javaparser.JavaParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Tests for the well-definedness checks using the java-smt backend, i.e.
 * without requiring a locally installed z3 binary (the native solver
 * libraries are bundled with java-smt).
 *
 * @author Alexander Weigl
 * @version 1 (20.09.26)
 */
internal class JavaSmtSolverTest {
    private val parser = JavaParser()

    @ParameterizedTest
    @ValueSource(
        strings = [
            "true", "false", "1 == 2", "5 * 2 == 2", "42 / 2",
            "x - 2 == 3", "'x' == 65", "'x' != 65",
            "new Object().equals(null)"
        ]
    )
    fun wellDefined(expr: String) {
        Assertions.assertTrue(WdFacade.isWelldefined(parser, expr), expr)
    }

    @ParameterizedTest
    @ValueSource(strings = ["x / 0", "x / (1 - 1)"])
    fun notWellDefined(expr: String) {
        Assertions.assertFalse(WdFacade.isWelldefined(parser, expr), expr)
    }
}

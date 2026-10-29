/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.jml.body.JmlPredicateDeclaration
import io.github.jmltoolkit.smt.BitVectorArithmeticTranslator
import io.github.jmltoolkit.smt.SmtQuery
import io.github.jmltoolkit.smt.Z3
import io.github.jmltoolkit.wd.WDVisitorExpr
import io.github.jmltoolkit.wd.WdFacade.isWelldefined
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource

/**
 * @author Alexander Weigl
 * @version 1 (14.06.22)
 */
internal class WDVisitorExprTest {
    private val parser = JavaParser()

    @ParameterizedTest
    @CsvFileSource(resources = ["wd-expr.csv"], delimiterString = "§")
    fun wdExpression(expr: String) {
        Assumptions.assumeTrue(Z3.z3Installed())
        Assertions.assertTrue(isWelldefined(parser, expr))
    }

    @ParameterizedTest
    @CsvFileSource(resources = ["not-wd-expr.csv"], delimiterString = "§")
    fun wdExpressionError(expr: String) {
        Assumptions.assumeTrue(Z3.z3Installed())
        Assertions.assertFalse(isWelldefined(parser, expr))
    }

    @Test
    fun predicateWellDefinednessDelegatesToBody() {
        val config = ParserConfiguration()
        config.isProcessJml = true
        val parser = JavaParser(config)
        val cu = parser.parse(
            """
            public class P {
                //@ instance predicate Pred(int i) = i > 0;
            }
            """.trimIndent()
        ).result.orElseThrow()
        val predicate = cu.findAll(JmlPredicateDeclaration::class.java).single()

        val query = SmtQuery()
        val visitor = WDVisitorExpr(query, BitVectorArithmeticTranslator(query))
        // The well-definedness of a predicate declaration is the well-definedness of its body.
        Assertions.assertEquals(
            predicate.body.accept(visitor, null).toString(),
            predicate.accept(visitor, null).toString()
        )
    }
}

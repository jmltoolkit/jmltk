/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver
import io.github.jmltoolkit.smt.SMTFacade
import io.github.jmltoolkit.smt.SmtQuery
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Element-domain quantifiers over {@code Iterable} (kind 4) domains have no
 * formal SMT model yet; the backend must fail loudly instead of silently
 * producing a wrong encoding. Kinds 2 (range) and 3 (array) are covered by the
 * {@code expr.yaml} fixtures.
 *
 * @author Alexander Weigl
 */
class ElementQuantifierSmtTest {
    private val parser: JavaParser
    private val parent: Node

    init {
        val config = ParserConfiguration()
        config.setSymbolResolver(JavaSymbolSolver(ClassLoaderTypeSolver(ClassLoader.getSystemClassLoader())))
        parser = JavaParser(config)
        val r = parser.parse(javaClass.getResourceAsStream("Test.java"))
        parent = r.result.get().getType(0)
    }

    @Test
    fun iterableDomainIsNotSupportedYet() {
        val e = parser.parseJmlExpression<Expression>("(\\forall String s : words; s.length() > 0)").result.get()
        e.setParentNode(parent)
        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            SMTFacade.toSmt(e, SmtQuery(), true)
        }
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.wd

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.expr.Expression
import io.github.jmltoolkit.smt.ArithmeticTranslator
import io.github.jmltoolkit.smt.BitVectorArithmeticTranslator
import io.github.jmltoolkit.smt.SmtQuery
import io.github.jmltoolkit.smt.SmtTermFactory.not
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.solver.SolverAnswer

/**
 * Facade to dive into *well-definedness* checks.
 *
 * @author Alexander Weigl
 * @version 1 (28.01.24)
 */
object WdFacade {
    fun isWelldefined(expr: String): Boolean {
        val config = ParserConfiguration()
        config.setProcessJml(false)
        val parser = JavaParser(config)
        return isWelldefined(parser, expr)
    }

    fun isWelldefined(parser: JavaParser, expr: String): Boolean {
        val e = parser.parseJmlExpression<Expression>(expr)
        return e.isSuccessful && e.result.isPresent && isWelldefined(e.result.get())
    }

    private fun isWelldefined(e: Expression): Boolean {
        val query = createQuery()
        val res: SExpr? = e.accept(createVisitor(query), null)
        if (res == null || "true" == res.toString()) {
            return true
        }
        query.addAssert(!res)
        query.checkSat()
        val ans: SolverAnswer = solve(query)
        println(query.toString())
        println(ans.toString())
        ans.consumeErrors()
        return ans.isSymbol("unsat")
    }

    /**
     * Solves the query with the java-smt backend, if available, and falls
     * back to the external z3 process otherwise.
     */
    private fun solve(query: SmtQuery): SolverAnswer =
        io.github.jmltoolkit.smt.solver.Solver().run(query)

    /**
     * Creates a fresh SMT query with the object formalization declared,
     * cf. [io.github.jmltoolkit.smt.SmtObjectModel].
     */
    fun createQuery(): SmtQuery {
        val query = SmtQuery()
        //query.defineObjectModel()
        return query
    }

    fun createVisitor(query: SmtQuery): WDVisitorExpr {
        val translator: ArithmeticTranslator = BitVectorArithmeticTranslator(query)
        return WDVisitorExpr(query, translator)
    }

    /**
     * Computes the well-definedness formula of the given expression. The
     * formula is true iff the expression is well-defined; useful for testing
     * and debugging.
     */
    fun wdFormula(e: Expression): SExpr = e.accept(createVisitor(createQuery()), null)!!
}


/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.stat

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.DataKey
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Modifier.DefaultKeyword.JML_GHOST
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.jml.body.JmlClassAccessibleDeclaration
import com.github.javaparser.ast.jml.body.JmlClassExprDeclaration
import com.github.javaparser.ast.jml.body.JmlMethodDeclaration
import com.github.javaparser.ast.jml.clauses.*
import com.github.javaparser.ast.jml.expr.JmlLetExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

/** Statistics are stored inside the AST */
val DATA_KEY_STATISTICS = object : DataKey<Statistics>() {}

/**
 * Computes the statistics resursively for each node of interest.
 *
 * @author Alexander Weigl
 * @version 1 (7/19/26)
 */
class JmlStatisticsReporter : VoidVisitorAdapter<Statistics>() {
    override fun visit(n: CompilationUnit, arg: Statistics) {
        super.visit(n, arg.subStat(n))
    }

    override fun visit(n: ClassOrInterfaceDeclaration, arg: Statistics) {
        val s = arg.subStat(n)
        s.inc(StatisticKey.TOTALCLASSES)
        super.visit(n, s)
    }

    override fun visit(n: MethodDeclaration, arg: Statistics) {
        val s = arg.subStat(n)
        s.inc(StatisticKey.TOTAL_METHODS)

        if (n.hasModifier(Modifier.DefaultKeyword.JML_MODEL)) {
            s.inc(StatisticKey.totalModelMethods)
        }

        if (n.hasModifier(Modifier.DefaultKeyword.JML_PURE)) {
            s.inc(StatisticKey.pureMethods)
        }

        if (n.contracts.isNotEmpty()) {
            s.inc(StatisticKey.SPECIFIED_METHODS)
        }

        super.visit(n, s)
    }

    override fun visit(n: FieldDeclaration, arg: Statistics) {
        val ghost = n.hasModifier(JML_GHOST)
        n.variables().forEach {
            arg.inc(StatisticKey.TOTAL_FIELDS)
            if (ghost) {
                arg.inc(StatisticKey.GHOST_FIELDS)
            }
        }
    }

    override fun visit(n: Modifier, arg: Statistics) {
        arg.inc(n)
    }

    override fun visit(n: JmlContract, arg: Statistics) {
        val s = arg.subStat(n)
        when (n.type) {
            ContractType.METHOD -> s.inc(StatisticKey.CONTRACT_METHOD)
            ContractType.LOOP -> s.inc(StatisticKey.CONTRACT_LOOP)
            ContractType.LOOP_INV -> s.inc(StatisticKey.CONTRACT_LOOPINV)
            ContractType.BLOCK -> s.inc(StatisticKey.CONTRACT_BLOCK)
            ContractType.STATEMENT -> s.inc(StatisticKey.CONTRACT_STATEMENT)
            ContractType.LAMBDA -> s.inc(StatisticKey.CONTRACT_LAMBDA)
        }
        s.inc(n.behavior)

        super.visit(n, s)
    }

    override fun visit(n: JmlLabeledClause, arg: Statistics) {
        arg.inc(n)
        super.visit(n, arg)
    }

    override fun visit(n: JmlSimpleExprClause, arg: Statistics) {
        arg.inc(n)
        super.visit(n, arg)
    }

    override fun visit(n: JmlSignalsClause, arg: Statistics) {
        arg.inc(n)
        super.visit(n, arg)
    }

    override fun visit(n: JmlSignalsOnlyClause, arg: Statistics) {
        arg.inc(n)
        super.visit(n, arg)
    }

    override fun visit(n: JmlCallableClause, arg: Statistics) {
        arg.inc(n)
        super.visit(n, arg)
    }

    override fun visit(n: JmlForallClause, arg: Statistics) {
        arg.inc(n)
        super.visit(n, arg)
    }

    override fun visit(n: JmlConditionalClause, arg: Statistics) {
        arg.inc(n)
        super.visit(n, arg)
    }

    override fun visit(n: JmlMultiExprClause, arg: Statistics) {
        arg.inc(n)

        super.visit(n, arg)
    }

    override fun visit(n: JmlOldClause, arg: Statistics) {
        arg.inc(n)
        super.visit(n, arg)
    }

    override fun visit(n: JmlInfFlowClause, arg: Statistics) {
        arg.inc(n)
        super.visit(n, arg)
    }

    override fun visit(n: JmlMethodDeclaration, arg: Statistics) {
        super.visit(n, arg.subStat(n))
    }

    override fun visit(n: JmlClassExprDeclaration, arg: Statistics) {
        arg.inc(n)
        super.visit(n, arg)
    }

    override fun visit(n: JmlClassAccessibleDeclaration, arg: Statistics) {
        arg.inc(n)
        super.visit(n, arg)
    }

    override fun visit(n: JmlQuantifiedExpr, arg: Statistics) {
        arg.inc(n.binder)
        super.visit(n, arg)
    }

    override fun visit(n: JmlLetExpr, arg: Statistics) {
        arg.inc(StatisticKey.LET)
        super.visit(n, arg)
    }
}

private fun countLines(node: com.github.javaparser.ast.Node): Int =
    node.toString().count { it == '\n' } + 1

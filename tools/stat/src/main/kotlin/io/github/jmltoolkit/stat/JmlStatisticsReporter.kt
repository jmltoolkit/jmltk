/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.stat

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.jml.clauses.*
import com.github.javaparser.ast.jml.doc.JmlDocDeclaration
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Computes and reports comprehensive JML statistics including coverage, complexity, and quality metrics.
 *
 * @author Alexander Weigl
 * @version 1 (7/19/26)
 */
class JmlStatisticsReporter {

    data class Statistics(
        val totalClasses: Int = 0,
        val totalMethods: Int = 0,
        val totalFields: Int = 0,
        val specifiedClasses: Int = 0,
        val specifiedMethods: Int = 0,
        val totalRequires: Int = 0,
        val totalEnsures: Int = 0,
        val totalAssigns: Int = 0,
        val totalInvariants: Int = 0,
        val totalGhostFields: Int = 0,
        val totalModelMethods: Int = 0,
        val pureMethods: Int = 0,
        val helperMethods: Int = 0,
        val methodsWithLoopInvariants: Int = 0,
        val methodsWithExceptionSpecs: Int = 0,
        val totalQuantifiers: Int = 0,
        val maxQuantifierDepth: Int = 0,
        val avgExpressionComplexity: Double = 0.0,
        val totalJmlLines: Int = 0,
        val totalJavaLines: Int = 0
    ) {
        val methodCoveragePercent: Double
            get() = if (totalMethods == 0) 0.0 else (specifiedMethods.toDouble() / totalMethods) * 100

        val classCoveragePercent: Double
            get() = if (totalClasses == 0) 0.0 else (specifiedClasses.toDouble() / totalClasses) * 100

        val assignsCoveragePercent: Double
            get() = if (specifiedMethods == 0) 0.0 else (totalAssigns.toDouble() / specifiedMethods) * 100

        val purityRatio: Double
            get() = if (totalMethods == 0) 0.0 else (pureMethods.toDouble() / totalMethods) * 100

        val specToCodeRatio: Double
            get() = if (totalJavaLines == 0) 0.0 else totalJmlLines.toDouble() / totalJavaLines
    }

    /**
     * Compute statistics from a list of CompilationUnits.
     */
    fun compute(compilationUnits: List<CompilationUnit>): Statistics {
        var totalClasses = 0
        var totalMethods = 0
        var totalFields = 0
        var specifiedClasses = 0
        var specifiedMethods = 0
        var totalRequires = 0
        var totalEnsures = 0
        var totalAssigns = 0
        var totalInvariants = 0
        var totalGhostFields = 0
        var totalModelMethods = 0
        var pureMethods = 0
        var helperMethods = 0
        var methodsWithLoopInvariants = 0
        var methodsWithExceptionSpecs = 0
        var totalQuantifiers = 0
        var maxQuantifierDepth = 0
        var sumExpressionComplexity = 0
        var expressionCount = 0
        var totalJmlLines = 0
        var totalJavaLines = 0

        for (cu in compilationUnits) {
            cu.storage.ifPresent { storage ->
                totalJavaLines += storage.lines.toInt()
            }

            cu.accept(object : com.github.javaparser.ast.visitor.VoidVisitorAdapter<Unit>() {
                override fun visit(n: ClassOrInterfaceDeclaration, arg: Unit?) {
                    totalClasses++
                    val hasClassSpecs = n.getJmlContracts().isNotEmpty() || 
                                       n.members.any { it is JmlDocDeclaration }
                    if (hasClassSpecs) specifiedClasses++

                    n.members.filterIsInstance<JmlInvariant>().forEach { inv ->
                        totalInvariants++
                        totalJmlLines += countJmlLines(inv)
                    }

                    n.members.filterIsInstance<FieldDeclaration>().forEach { field ->
                        if (field.modifiers.any { m -> m.keyword.toString() == "GHOST" }) {
                            totalGhostFields += field.variables.size
                        }
                        totalFields += field.variables.size
                    }
                    super.visit(n, arg)
                }

                override fun visit(n: MethodDeclaration, arg: Unit?) {
                    totalMethods++
                    val contracts = n.getJmlContracts()
                    if (contracts.isNotEmpty()) specifiedMethods++

                    contracts.filterIsInstance<JmlRequires>().forEach { req ->
                        totalRequires++
                        totalJmlLines += countJmlLines(req)
                        sumExpressionComplexity += computeComplexity(req.condition)
                        expressionCount++
                    }

                    contracts.filterIsInstance<JmlEnsures>().forEach { ens ->
                        totalEnsures++
                        totalJmlLines += countJmlLines(ens)
                        sumExpressionComplexity += computeComplexity(ens.result)
                        expressionCount++
                    }

                    contracts.filterIsInstance<JmlAssignable>().forEach { totalAssigns++ }
                    contracts.filterIsInstance<JmlSignals>().forEach { methodsWithExceptionSpecs++ }

                    checkLoopInvariants(n).let { methodsWithLoopInvariants += it }

                    if (n.modifiers.any { m -> m.keyword.toString() == "PURE" }) pureMethods++
                    if (n.modifiers.any { m -> m.keyword.toString() == "HELPER" }) helperMethods++

                    n.findAll(JmlQuantifiedExpr::class.java).forEach { qexpr ->
                        totalQuantifiers++
                        maxQuantifierDepth = maxOf(maxQuantifierDepth, computeQuantifierDepth(qexpr))
                    }
                    super.visit(n, arg)
                }

                override fun visit(n: JmlMethodDeclaration, arg: Unit?) {
                    if (n.isModel) totalModelMethods++
                    super.visit(n, arg)
                }
            }, null)
        }

        val avgComplexity = if (expressionCount == 0) 0.0 else sumExpressionComplexity.toDouble() / expressionCount
        return Statistics(totalClasses, totalMethods, totalFields, specifiedClasses, specifiedMethods,
            totalRequires, totalEnsures, totalAssigns, totalInvariants, totalGhostFields, totalModelMethods,
            pureMethods, helperMethods, methodsWithLoopInvariants, methodsWithExceptionSpecs,
            totalQuantifiers, maxQuantifierDepth, avgComplexity, totalJmlLines, totalJavaLines)
    }

    private fun checkLoopInvariants(method: MethodDeclaration): Int {
        var count = 0
        method.findAll(com.github.javaparser.ast.stmt.ForStmt::class.java).forEach { stmt ->
            if (stmt.comment.isPresent && stmt.comment.get().content.contains("invariant", ignoreCase = true)) count++
        }
        method.findAll(com.github.javaparser.ast.stmt.WhileStmt::class.java).forEach { stmt ->
            if (stmt.comment.isPresent && stmt.comment.get().content.contains("invariant", ignoreCase = true)) count++
        }
        return count
    }

    private fun countJmlLines(node: com.github.javaparser.ast.Node): Int {
        return node.comment.map { c -> c.content.count { it == '\n' } + 1 }.orElse(0)
    }

    private fun computeComplexity(expr: com.github.javaparser.ast.expr.Expression?): Int {
        if (expr == null) return 0
        return expr.accept(ExpressionComplexity(), ExpressionCosts())
    }

    private fun computeQuantifierDepth(expr: JmlQuantifiedExpr): Int {
        var depth = 1
        expr.findAll(JmlQuantifiedExpr::class.java).forEach { nested ->
            if (nested != expr) depth = maxOf(depth, 1 + computeQuantifierDepth(nested))
        }
        return depth
    }

    /**
     * Generate human-readable text report.
     */
    fun generateTextReport(stats: Statistics): String {
        val sb = StringBuilder()
        sb.appendLine("=== JML Specification Statistics ===")
        sb.appendLine()
        
        sb.appendLine("Coverage:")
        sb.appendLine("  Methods: ${stats.specifiedMethods}/${stats.totalMethods} (${String.format("%.1f", stats.methodCoveragePercent)}%)")
        sb.appendLine("  Classes: ${stats.specifiedClasses}/${stats.totalClasses} (${String.format("%.1f", stats.classCoveragePercent)}%)")
        sb.appendLine("  Assigns: ${String.format("%.1f", stats.assignsCoveragePercent)}% of specified methods")
        sb.appendLine()
        
        sb.appendLine("Contract Clauses:")
        sb.appendLine("  requires:   ${stats.totalRequires} (avg ${String.format("%.2f", avgPerMethod(stats.totalRequires, stats.specifiedMethods))} per method)")
        sb.appendLine("  ensures:    ${stats.totalEnsures} (avg ${String.format("%.2f", avgPerMethod(stats.totalEnsures, stats.specifiedMethods))} per method)")
        sb.appendLine("  assigns:    ${stats.totalAssigns}")
        sb.appendLine("  invariants: ${stats.totalInvariants}")
        sb.appendLine("  exception specs: ${stats.methodsWithExceptionSpecs}")
        sb.appendLine()
        
        sb.appendLine("Complexity:")
        sb.appendLine("  Avg expression complexity: ${String.format("%.2f", stats.avgExpressionComplexity)}")
        sb.appendLine("  Max quantifier depth: ${stats.maxQuantifierDepth}")
        sb.appendLine("  Total quantifiers: ${stats.totalQuantifiers}")
        sb.appendLine()
        
        sb.appendLine("Quality Indicators:")
        sb.appendLine("  Pure methods: ${stats.pureMethods} (${String.format("%.1f", stats.purityRatio)}%)")
        sb.appendLine("  Helper methods: ${stats.helperMethods}")
        sb.appendLine("  Model methods: ${stats.totalModelMethods}")
        sb.appendLine("  Ghost fields: ${stats.totalGhostFields}")
        sb.appendLine("  Methods with loop invariants: ${stats.methodsWithLoopInvariants}")
        sb.appendLine()
        
        sb.appendLine("Size:")
        sb.appendLine("  JML lines: ${stats.totalJmlLines}")
        sb.appendLine("  Java lines: ${stats.totalJavaLines}")
        sb.appendLine("  Spec/Code ratio: ${String.format("%.2f", stats.specToCodeRatio)}")
        
        return sb.toString()
    }

    private fun avgPerMethod(total: Int, methods: Int): Double {
        return if (methods == 0) 0.0 else total.toDouble() / methods
    }
}

/**
 * Extension function to get JML contracts from a method.
 */
fun MethodDeclaration.getJmlContracts(): List<com.github.javaparser.ast.jml.clauses.JmlClause> {
    val contracts = mutableListOf<com.github.javaparser.ast.jml.clauses.JmlClause>()
    this.jmlTags.forEach { tag ->
        if (tag is com.github.javaparser.ast.jml.clauses.JmlClause) {
            contracts.add(tag)
        }
    }
    return contracts
}

/**
 * Extension function to get JML contracts from a class.
 */
fun ClassOrInterfaceDeclaration.getJmlContracts(): List<com.github.javaparser.ast.jml.clauses.JmlClause> {
    val contracts = mutableListOf<com.github.javaparser.ast.jml.clauses.JmlClause>()
    this.jmlTags.forEach { tag ->
        if (tag is com.github.javaparser.ast.jml.clauses.JmlClause) {
            contracts.add(tag)
        }
    }
    return contracts
}
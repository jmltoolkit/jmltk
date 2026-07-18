/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import io.github.jmltoolkit.stat.JmlStatisticsReporter
import io.github.jmltoolkit.stat.Statistics
import io.github.jmltoolkit.stat.StatisticsTxtOutput
import java.io.PrintWriter

/**
 * Command for computing and printing JML statistics.
 *
 * Reports coverage, complexity, quality indicators, and size metrics.
 *
 * @author Alexander Weigl
 * @version 1 (7/19/26)
 */
class StatCommand : FileBasedCommand("stat") {
    enum class OutputFormat {
        TEXT,
        JSON,
        CSV,
        XML
    }

    override fun help(context: Context): String = "Print statistics on JML specifications"

    val textOutput by option(
        "-t", "--target",
        help = "Output in user specified format"
    ).enum<OutputFormat>(true).default(OutputFormat.TEXT)

    val selectedColumns by option(
        "--column", "-c",
        help = "Specifies the attribute to report on"
    ).multiple()

    override fun run() {
        val config = ParserConfiguration()
        config.setProcessJml(true)
        config.setKeepJmlDocs(true)

        for (activeJmlKey in activeJmlKeys) {
            val keys = activeJmlKey.split(",").dropLastWhile { it.isEmpty() }.toTypedArray()
            config.jmlKeys.add(listOf(*keys))
        }

        if (activeJmlKeys.isEmpty()) {
            config.jmlKeys.add(listOf("key"))
        }

        val nodes: Collection<CompilationUnit> = parse(files, config)

        // Compute statistics by visiting each compilation unit
        val rootStats = Statistics()
        val reporter = JmlStatisticsReporter()
        for (node in nodes) {
            reporter.visit(node, rootStats)
        }

        when (textOutput) {
            OutputFormat.TEXT -> {
                // Use StatisticsTxtOutput for text format
                val txtOutput = StatisticsTxtOutput(PrintWriter(System.out), null)
                txtOutput.start()
                txtOutput.run(rootStats, nodes.toList())
                txtOutput.end()
            }

            OutputFormat.XML -> {
                // Generate XML output using existing StatVisitor for compatibility
                generateXmlOutput(nodes, config)
            }

            else -> error("Unsupported")
        }
    }

    private fun generateXmlOutput(
        nodes: Collection<CompilationUnit>,
        config: ParserConfiguration
    ) {
        val builderFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        val builder = builderFactory.newDocumentBuilder()
        val xmlDocument = builder.newDocument()
        val xmlRoot = xmlDocument.createElement("statistics-model")

        val costs = io.github.jmltoolkit.stat.ExpressionCosts()
        for (key in config.jmlKeys) {
            val statVisitor = io.github.jmltoolkit.stat.StatVisitor(xmlDocument, key, costs)
            val e = xmlDocument.createElement("settings")
            e.setAttribute("keys", "" + key)
            xmlRoot.appendChild(e)
            for (node in nodes) {
                node.accept(statVisitor, e)
            }
        }

        val transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        val result = javax.xml.transform.stream.StreamResult(java.io.StringWriter())
        val source = javax.xml.transform.dom.DOMSource(xmlRoot)
        transformer.transform(source, result)
        println(result.writer.toString())
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.trace

import jjbmc.Assignment
import jjbmc.ErrorLogger.debug
import jjbmc.ErrorLogger.info
import jjbmc.JBMCOutput
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object TraceParser {
    private val jbmcBanner = """

            * *             JBMC 5.22.0 (cbmc-5.22.0) 64-bit            * *
            * *                 Copyright (C) 2001-2018                 * *
            * *              Daniel Kroening, Edmund Clarke             * *
            * * Carnegie Mellon University, Computer Science Department * *
            * *                  kroening@kroening.com                  * *
            """.trimIndent()

    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun parse(xmlFile: File?, printTrace: Boolean): JBMCOutput {
        val builder: DocumentBuilder
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(xmlFile)
        return parse(doc, printTrace)
    }

    @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
    fun parse(xmlContent: String, printTrace: Boolean): JBMCOutput {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        try {
            val `is` = InputSource(StringReader(xmlContent))
            val doc = builder.parse(`is`)
            doc.documentElement.normalize()
            return parse(doc, printTrace)
        } catch (e: SAXException) {
            if (xmlContent.startsWith(jbmcBanner)) {
                error("Error calling jbmc. Possibly provided faulty jbmc-arguments?")
            }
            throw e
        } catch (e: IOException) {
            if (xmlContent.startsWith(jbmcBanner)) {
                error("Error calling jbmc. Possibly provided faulty jbmc-arguments?")
            }
            throw e
        }
    }

    fun parse(xmlDoc: Document, printTrace: Boolean): JBMCOutput {
        val res: JBMCOutput = JBMCOutput()
        try {
            var trace: Trace? = null
            xmlDoc.documentElement.normalize()
            val messageList = xmlDoc.getElementsByTagName("message")
            for (i in 0..<messageList.length) {
                if (((messageList.item(i)) as Element).getAttribute("type") == "ERROR") {
                    res.errors.add(messageList.item(i).textContent)
                } else {
                    res.messages.add(messageList.item(i).textContent)
                }
            }
            if (res.errors.isNotEmpty()) {
                res.proverStatus = "ERROR"
                return res
            }
            val statusList = xmlDoc.getElementsByTagName("cprover-status")
            assert(statusList.length == 1)
            res.proverStatus = statusList.item(0).textContent
            if (!printTrace) {
                return res
            }
            val propertyNodeList = xmlDoc.getElementsByTagName("result")
            var reason: String?
            for (i in 0..<propertyNodeList.length) {
                reason = null
                val propertyNode = propertyNodeList.item(i)
                if (propertyNode.nodeType == Node.ELEMENT_NODE) {
                    val propertyElemnt = propertyNode as Element
                    var lineNumber = -1
                    if (propertyElemnt.getAttribute("status") == "FAILURE") {
                        val failure = propertyElemnt.getElementsByTagName("failure").item(0) as Element
                        reason = failure.getAttribute("reason")
                        val location = failure.getElementsByTagName("location").item(0) as Element?
                        if (location == null) {
                            if (propertyElemnt.getAttribute("property").contains("unwind")) {
                                res.addProperty(
                                    "Unwinding assertion",
                                    Trace(ArrayList()),
                                    -1,
                                    "Try to increase the unwinding parameter.",
                                    null
                                )
                                return res
                            } else {
                                throw Exception("location was null.")
                            }
                        } else {
                            lineNumber = location.getAttribute("line").toInt()
                        }
                        val relevantRange = TraceInformation.getRelevantRange(lineNumber)
                        val assignmentList = propertyNode.getElementsByTagName("assignment")
                        val assignments = arrayListOf<Assignment>()
                        var lineAssignments = arrayListOf<Assignment>()
                        var lastLine = -1
                        for (j in 0..<assignmentList.length) {
                            val assignment = assignmentList.item(j) as Element
                            if (assignment.getElementsByTagName("location").length > 0) {
                                val location1 = assignment
                                    .getElementsByTagName("location")
                                    .item(0) as Element
                                val lhs = assignment
                                    .getElementsByTagName("full_lhs")
                                    .item(0) as Element
                                val value = assignment
                                    .getElementsByTagName("full_lhs_value")
                                    .item(0) as Element
                                val line = location1.getAttribute("line").toInt()
                                // int origLine = TraceInformation.getOriginalLine(line);
                                val (a, b) = relevantRange!!
                                if (line > lastLine && line < b && line >= a) {
                                    // trace.provideGuesses(lineAssignments);
                                    lineAssignments = ArrayList()
                                    lastLine = line
                                }
                                var parameterName: String? = null
                                if (assignment.getAttribute("assignment_type") == "actual_parameter") {
                                    parameterName = assignment.getAttribute("display_name")
                                }
                                val assignment1 = Assignment(
                                    lineNumber = line,
                                    parameterName = parameterName!!,
                                    value = lhs.textContent,
                                    jbmcVarname = value.textContent,
                                )
                                lineAssignments.add(assignment1)
                                assignments.add(assignment1)
                            }
                        }
                        trace = extractTrace(assignments)
                        if (reason.contains("assertion")) {
                            trace.relevantVars = TraceInformation.getAssertVarsForLine(lineNumber)
                        }
                    }
                    if (lineNumber < 0) {
                        res.addProperty(propertyElemnt.getAttribute("property"), null, lineNumber, null, null)
                    } else {
                        if (reason!!.contains("assertion")) {
                            res.addProperty(
                                propertyElemnt.getAttribute("property"),
                                trace,
                                TraceInformation.getOriginalLine(lineNumber),
                                reason,
                                TraceInformation.getAssertForLine(lineNumber)
                            )
                        } else {
                            res.addProperty(
                                propertyElemnt.getAttribute("property"),
                                trace,
                                TraceInformation.getOriginalLine(lineNumber),
                                reason,
                                null
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            info("Error parsing xml file.")
            val tf = TransformerFactory.newInstance()
            val transformer: Transformer
            try {
                transformer = tf.newTransformer()
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
                transformer.setOutputProperty(OutputKeys.INDENT, "no")
                val writer = StringWriter()
                transformer.transform(DOMSource(xmlDoc), StreamResult(writer))
                val output: String = writer.toString()
                debug(output)
            } catch (ex: TransformerException) {
                ex.printStackTrace()
            }
            e.printStackTrace()
        }

        return res
    }

    fun extractTrace(assignments: MutableList<Assignment>): Trace = Trace(assignments)

    private fun getOriginalName(exprs: Array<String>, exprMap: MutableMap<String?, String?>): String {
        val res = StringBuilder()
        for (s in exprs) {
            res.append(getOriginalName(s, exprMap))
        }
        return res.toString()
    }

    private fun getOriginalName(expr: String, exprMap: MutableMap<String?, String?>): String {
        var expr = expr
        while (exprMap.containsKey(expr)) {
            expr = exprMap.get(expr)!!
            val exprs =
                expr.split("((?<=([.\\[\\]])|(?=([.\\[\\]]))))".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (exprs.size > 1) {
                expr = getOriginalName(exprs, exprMap)
            }
        }
        return expr
    }
}

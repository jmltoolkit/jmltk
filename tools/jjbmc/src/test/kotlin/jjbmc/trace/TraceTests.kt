/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.trace

import jjbmc.ErrorLogger.setDebugOn
import jjbmc.JJBMCOptions
import jjbmc.Operations
import jjbmc.trace.TraceParser.parse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Execution(ExecutionMode.SAME_THREAD)
class TraceTests {

    @TestFactory
    fun factory(): Sequence<DynamicTest> {
        val tests = listOf(
            TraceTest(
                "src/test/resources/traceTest/TraceTestCases.java", "TmpTestOut.txt",
                listOf("k", "tt", "table"), "test"
            ),
            TraceTest(
                "src/test/resources/traceTest/TraceTestCases.java",
                "TmpTestOut2.txt", listOf(), "test2"
            ),
            TraceTest(
                "src/test/resources/traceTest/TraceTestCases.java",
                "TmpTestOut3.txt", listOf("iotable"), "test3"
            ),
            TraceTest(
                "src/test/resources/traceTest/TraceTestCases.java",
                "TmpTestOut4.txt", listOf(), "test4"
            ),
            TraceTest(
                "src/test/resources/traceTest/TraceTestCases.java",
                "TmpTestOut5.txt", listOf(), "test5"
            ),
            TraceTest(
                "src/test/resources/traceTest/TraceTestCases.java",
                "TmpTestOut6.txt", listOf(), "test6"
            )
        )

        return tests.asSequence().map { DynamicTest.dynamicTest(it.inputFile.toString()) { it.run() } }
    }

    data class TraceTest(
        val inputFile: Path,
        val outFile: String,
        val relevantVars: List<String>,
        val functionName: String
    ) {
        constructor(inputFile: String, outFile: String, relevantVars: List<String>, functionName: String) : this(
            Paths.get(inputFile), outFile, relevantVars, functionName
        )

        fun run() {
            val options = JJBMCOptions()
            options.reset()
            options.runWithTrace = true
            options.keepTranslation = true
            options.functionName = functionName
            options.relevantVars.addAll(relevantVars)
            options.setTmpFolder(
                Paths.get("tmp").resolve("TraceTests").resolve(functionName).toAbsolutePath()
            )
            options.setFileName(inputFile)

            val operations = Operations(options)
            operations.translateAndRunJBMC()

            val f = options.getTmpFile().resolve("xmlout.xml").toFile()
            Assertions.assertTrue(f.exists(), "No JBMC output file was written.")
            val output = parse(f, true)
            val traces = output.printAllTraces()
            val traceSplits = traces.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val assignments = ArrayList<String>()
            for (s in traceSplits) {
                if (s.startsWith("in line")) {
                    assignments.add(s)
                }
            }
            val reference = File(outFile)
            Assertions.assertTrue(reference.exists())
            try {
                val lines = Files.readAllLines(reference.toPath())
                Assertions.assertEquals(lines.size, assignments.size)
                for (i in lines.indices) {
                    Assertions.assertEquals(lines[i].trim { it <= ' ' }, assignments[i]!!.trim { it <= ' ' })
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun run() {
            setDebugOn()
        }
    }
}

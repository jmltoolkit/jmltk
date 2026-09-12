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
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

@Execution(ExecutionMode.SAME_THREAD)
class TraceTests {
    @ParameterizedTest
    @MethodSource("getParameters")
    @Throws(Exception::class)
    fun traceTest(inputFile: Path?, outFile: String, relevantVars: MutableList<String?>, functionName: String) {
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
        Assertions.assertTrue(f.exists())
        val output = parse(f, true)
        val traces = output.printAllTraces()
        val traceSplits = traces.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val assignments: MutableList<String?> = ArrayList<String?>()
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
                Assertions.assertEquals(lines.get(i).trim { it <= ' ' }, assignments.get(i)!!.trim { it <= ' ' })
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        @BeforeAll
        fun run() {
            setDebugOn()
        }

        val parameters: Stream<Arguments?>
            get() = Stream.of<Arguments?>(
                Arguments.of(
                    "src/test/resources/traceTest/TraceTestCases.java",
                    "TmpTestOut.txt",
                    mutableListOf<String?>("k", "tt", "table"),
                    "test"
                ),
                Arguments.of(
                    "src/test/resources/traceTest/TraceTestCases.java",
                    "TmpTestOut2.txt",
                    mutableListOf<Any?>(),
                    "test2"
                ),
                Arguments.of(
                    "src/test/resources/traceTest/TraceTestCases.java",
                    "TmpTestOut3.txt",
                    mutableListOf<String?>("iotable"),
                    "test3"
                ),
                Arguments.of(
                    "src/test/resources/traceTest/TraceTestCases.java",
                    "TmpTestOut4.txt",
                    mutableListOf<Any?>(),
                    "test4"
                ),
                Arguments.of(
                    "src/test/resources/traceTest/TraceTestCases.java",
                    "TmpTestOut5.txt",
                    mutableListOf<Any?>(),
                    "test5"
                ),
                Arguments.of(
                    "src/test/resources/traceTest/TraceTestCases.java",
                    "TmpTestOut6.txt",
                    mutableListOf<Any?>(),
                    "test6"
                )
            )
    }
}

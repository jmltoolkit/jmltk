/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.stat

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Paths
import kotlin.io.path.readText

/**
 * Test cases for StatisticsTxtOutput.
 *
 * @author Alexander Weigl
 * @version 1 (8/12/26)
 */
internal class StatisticsTxtOutputTest {
    @Test
    fun testLengthyJmlExampleWithComprehensiveFeatures() {
        val code = Paths.get("src/test/resources/VerifiedBankAccount.java").readText()
        val output = parse(code)
        println(output)

        // Verify classes are detected
        assertThat(output).contains("VerifiedBankAccount")
        assertThat(output).contains("InsufficientFundsException")

        // Verify all methods are detected
        assertThat(output).contains("deposit")
        assertThat(output).contains("withdraw")
        assertThat(output).contains("closeAccount")
        assertThat(output).contains("getBalance")
        assertThat(output).contains("getOwner")
        assertThat(output).contains("getOverdraftLimit")
        assertThat(output).contains("getTransactionCount")
        assertThat(output).contains("compareTo")
        assertThat(output).contains("bulkDeposit")
        assertThat(output).contains("availableFunds")
        assertThat(output).contains("setOverdraftLimit")

        // Verify constructor is detected
        assertThat(output).contains("VerifiedBankAccount")

        // Verify table format
        assertThat(output).contains("|")
        assertThat(output).contains("TYPE")
        assertThat(output).contains("NAME")

        // Verify JML-specific statistics are present
        assertThat(output).containsMatch("(?i)(requires|ensures|invariant|pure|ghost|spec)")
    }

    // ==================== Helper Methods ====================
    private fun parse(code: String): String {
        val parse = javaParser.parse(code)
        require(parse.isSuccessful) {
            parse.problems.forEach(::println)
            "Parsing failed!"
        }

        val cu = parse.result.orElseThrow()

        val global = Statistics()
        cu.accept(JmlStatisticsReporter(), global)

        val stringWriter = StringWriter()
        val writer = PrintWriter(stringWriter)
        val output = StatisticsTxtOutput(writer, null)
        output.start()
        output.run(global, listOf(cu))
        output.end()
        writer.flush()
        return stringWriter.toString()
    }
}

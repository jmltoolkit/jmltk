/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.bench

import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BenchTest {
    @Test
    fun `parses corpus and mode`() {
        val c = parseBenchConfig(arrayOf("--corpus", "jml", "--mode", "grammar", "--rounds", "3", "--warmup", "1"))
        assertEquals("jml", c.corpus)
        assertEquals(BenchMode.GRAMMAR, c.mode)
        assertEquals(3, c.rounds)
        assertEquals(1, c.warmup)
    }

    @Test
    fun `rejects unknown mode`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseBenchConfig(arrayOf("--corpus", "plain", "--mode", "fast"))
        }
    }

    @Test
    fun `median handles odd and even sizes`() {
        assertEquals(2.0, median(listOf(1.0, 3.0, 2.0)))
        assertEquals(2.5, median(listOf(1.0, 4.0, 2.0, 3.0)))
    }

    @Test
    fun `percent delta is signed relative change`() {
        assertEquals(-20.0, percentDelta(100.0, 80.0), 1e-9)
        assertEquals(5.0, percentDelta(100.0, 105.0), 1e-9)
    }

    @Test
    fun `json round trip`() {
        val result =
            BenchResult(
                "plain",
                BenchMode.FULL,
                2,
                10,
                1,
                1,
                listOf("a/B.java"),
                listOf(RoundSample(1.0, 2.0)),
                1.0,
                1.0,
            )
        val file = Files.createTempFile("bench", ".json")
        writeResult(result, file)
        assertEquals(result, readResult(file))
    }

    @Test
    fun `verify reports a changed ok set`() {
        val baseline =
            BenchResult(
                "plain",
                BenchMode.FULL,
                2,
                10,
                1,
                1,
                listOf("a/B.java"),
                listOf(RoundSample(1.0, 2.0)),
                1.0,
                1.0,
            )
        val current = baseline.copy(fail = 0, ok = 2, failures = emptyList())
        assertFalse(verifyParity(baseline, current).isEmpty())
        assertTrue(verifyParity(baseline, baseline).isEmpty())
    }
}

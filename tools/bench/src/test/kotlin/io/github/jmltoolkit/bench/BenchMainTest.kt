/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.bench

import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BenchMainTest {
    @Test
    fun `missing baseline exits 2`() {
        assertEquals(2, runMain(arrayOf("--corpus", "plain", "--verify")))
    }

    @Test
    fun `unknown corpus exits 2`() {
        assertEquals(2, runMain(arrayOf("--corpus", "nope", "--rounds", "1", "--warmup", "0")))
    }

    @Test
    fun `parity mismatch exits 1`() {
        val baseline = emptyPathBaseline("x")
        val file = Files.createTempFile("baseline", ".json")
        writeResult(baseline, file)
        assertEquals(
            1,
            runMain(
                arrayOf(
                    "--corpus",
                    "path:src/test",
                    "--mode",
                    "grammar",
                    "--warmup",
                    "0",
                    "--rounds",
                    "1",
                    "--baseline",
                    file.toString(),
                    "--verify",
                ),
            ),
        )
    }

    @Test
    fun `refuses to verify against the json it writes`() {
        val file = Files.createTempFile("baseline", ".json")
        writeResult(emptyPathBaseline(null), file)
        assertEquals(
            2,
            runMain(
                arrayOf(
                    "--corpus",
                    "path:src/test",
                    "--mode",
                    "grammar",
                    "--warmup",
                    "0",
                    "--rounds",
                    "1",
                    "--baseline",
                    file.toString(),
                    "--json",
                    file.toString(),
                    "--verify",
                ),
            ),
        )
    }

    private fun emptyPathBaseline(failure: String?): BenchResult =
        BenchResult(
            "path:src/test",
            BenchMode.GRAMMAR,
            0,
            0,
            0,
            if (failure == null) 0 else 1,
            listOfNotNull(failure),
            emptyList(),
            0.0,
            0.0,
        )

    @Test
    fun `ast change exits 1`() {
        val corpus = "path:src/test/resources/corpus"
        val file = Files.createTempFile("baseline", ".json")
        assertEquals(
            0,
            runMain(arrayOf("--corpus", corpus, "--mode", "grammar", "--warmup", "0", "--rounds", "1", "--json", file.toString())),
        )
        val baseline = readResult(file)
        assertEquals(setOf("src/test/resources/corpus/One.java"), baseline.astHashes.orEmpty().keys)
        writeResult(baseline.copy(astHashes = baseline.astHashes.orEmpty().mapValues { "deadbeef" }), file)
        assertEquals(
            1,
            runMain(
                arrayOf(
                    "--corpus",
                    corpus,
                    "--mode",
                    "grammar",
                    "--warmup",
                    "0",
                    "--rounds",
                    "1",
                    "--baseline",
                    file.toString(),
                    "--verify",
                ),
            ),
        )
    }

    @Test
    fun `matching ast exits 0`() {
        val corpus = "path:src/test/resources/corpus"
        val file = Files.createTempFile("baseline", ".json")
        assertEquals(
            0,
            runMain(arrayOf("--corpus", corpus, "--mode", "grammar", "--warmup", "0", "--rounds", "1", "--json", file.toString())),
        )
        assertEquals(
            0,
            runMain(
                arrayOf(
                    "--corpus",
                    corpus,
                    "--mode",
                    "grammar",
                    "--warmup",
                    "0",
                    "--rounds",
                    "1",
                    "--baseline",
                    file.toString(),
                    "--verify",
                ),
            ),
        )
    }
}

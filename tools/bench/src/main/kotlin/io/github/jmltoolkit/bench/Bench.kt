/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.bench

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.google.gson.GsonBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.streams.asSequence

enum class BenchMode {
    GRAMMAR,
    FULL,
}

data class BenchConfig(
    val corpus: String,
    val mode: BenchMode,
    val warmup: Int,
    val rounds: Int,
    val json: Path?,
    val baseline: Path?,
    val verify: Boolean,
)

data class RoundSample(
    val millis: Double,
    val megabytesPerSecond: Double,
)

data class BenchResult(
    val corpus: String,
    val mode: BenchMode,
    val files: Int,
    val bytes: Long,
    val ok: Int,
    val fail: Int,
    val failures: List<String>,
    val rounds: List<RoundSample>,
    val medianMillis: Double,
    val bestMillis: Double,
    val astHashes: Map<String, String>? = null,
)

private data class SourceFile(
    val displayName: String,
    val text: String,
)

private val gson = GsonBuilder().setPrettyPrinting().create()

fun parseBenchConfig(args: Array<String>): BenchConfig {
    var corpus = "plain"
    var mode = BenchMode.FULL
    var warmup = 3
    var rounds = 5
    var json: Path? = null
    var baseline: Path? = null
    var verify = false

    var i = 0
    fun value(option: String): String {
        if (i + 1 >= args.size) throw IllegalArgumentException("Missing value for $option")
        return args[++i]
    }

    while (i < args.size) {
        when (args[i]) {
            "--corpus" -> corpus = value("--corpus")

            "--mode" ->
                mode =
                    when (val raw = value("--mode")) {
                        "grammar" -> BenchMode.GRAMMAR
                        "full" -> BenchMode.FULL
                        else -> throw IllegalArgumentException("Unknown mode: $raw")
                    }

            "--warmup" -> warmup = intValue("--warmup", value("--warmup"))

            "--rounds" -> rounds = intValue("--rounds", value("--rounds"))

            "--json" -> json = Paths.get(value("--json"))

            "--baseline" -> baseline = Paths.get(value("--baseline"))

            "--verify" -> verify = true

            else -> throw IllegalArgumentException("Unknown option: ${args[i]}")
        }
        i++
    }

    require(warmup >= 0) { "--warmup must be >= 0" }
    require(rounds >= 1) { "--rounds must be >= 1" }
    return BenchConfig(corpus, mode, warmup, rounds, json, baseline, verify)
}

private fun intValue(
    option: String,
    raw: String,
): Int = raw.toIntOrNull() ?: throw IllegalArgumentException("Invalid integer for $option: $raw")

fun median(values: List<Double>): Double {
    require(values.isNotEmpty()) { "median of an empty list" }
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 0) {
        (sorted[middle - 1] + sorted[middle]) / 2.0
    } else {
        sorted[middle]
    }
}

fun percentDelta(
    baseline: Double,
    current: Double,
): Double = (current - baseline) / baseline * 100.0

fun verifyParity(
    baseline: BenchResult,
    current: BenchResult,
): List<String> {
    val differences = mutableListOf<String>()
    if (baseline.corpus != current.corpus) {
        differences += "corpus: ${baseline.corpus} -> ${current.corpus}"
    }
    if (baseline.mode != current.mode) {
        differences += "mode: ${baseline.mode} -> ${current.mode}"
    }
    if (baseline.files != current.files) {
        differences += "files: ${baseline.files} -> ${current.files}"
    }
    if (baseline.bytes != current.bytes) {
        differences += "bytes: ${baseline.bytes} -> ${current.bytes}"
    }
    if (baseline.ok != current.ok) {
        differences += "ok: ${baseline.ok} -> ${current.ok}"
    }
    if (baseline.fail != current.fail) {
        differences += "fail: ${baseline.fail} -> ${current.fail}"
    }
    val baselineHashes = baseline.astHashes.orEmpty()
    val currentHashes = current.astHashes.orEmpty()
    (baselineHashes.keys + currentHashes.keys).sorted().forEach { file ->
        val before = baselineHashes[file]
        val after = currentHashes[file]
        when {
            before == null -> differences += "AST added: $file"
            after == null -> differences += "AST missing: $file"
            before != after -> differences += "AST changed: $file"
        }
    }
    val missing = baseline.failures.toSet() - current.failures.toSet()
    val added = current.failures.toSet() - baseline.failures.toSet()
    missing.sorted().forEach { differences += "no longer fails: $it" }
    added.sorted().forEach { differences += "now fails: $it" }
    return differences
}

fun writeResult(
    result: BenchResult,
    path: Path,
) {
    path.parent?.let { Files.createDirectories(it) }
    Files.writeString(path, gson.toJson(result) + "\n")
}

fun readResult(path: Path): BenchResult = gson.fromJson(Files.readString(path), BenchResult::class.java)

fun runBenchmark(config: BenchConfig): BenchResult {
    val repoRoot = Paths.get("").toAbsolutePath().normalize()
    val sources = loadSources(config.corpus, repoRoot)
    val bytes = sources.sumOf { it.text.toByteArray(Charsets.UTF_8).size.toLong() }
    val parserConfig = parserConfiguration(config)

    repeat(config.warmup) { parseOnce(sources, parserConfig) }

    val rounds = mutableListOf<RoundSample>()
    var lastFailures = emptyList<String>()
    repeat(config.rounds) {
        System.gc()
        val start = System.nanoTime()
        val (failures, _) = parseOnce(sources, parserConfig)
        val millis = (System.nanoTime() - start) / 1_000_000.0
        lastFailures = failures
        rounds += RoundSample(millis, megabytesPerSecond(bytes, millis))
    }

    return BenchResult(
        corpus = config.corpus,
        mode = config.mode,
        files = sources.size,
        bytes = bytes,
        ok = sources.size - lastFailures.size,
        fail = lastFailures.size,
        failures = lastFailures,
        rounds = rounds,
        medianMillis = median(rounds.map { it.millis }),
        bestMillis = rounds.minOf { it.millis },
        astHashes = if (config.json != null || config.verify) parseHashes(sources, parserConfig) else null,
    )
}

private fun parseHashes(
    sources: List<SourceFile>,
    parserConfiguration: ParserConfiguration,
): Map<String, String> {
    val parser = JavaParser(parserConfiguration)
    val hashes = mutableMapOf<String, String>()
    for (source in sources) {
        val result = parser.parse(source.text)
        if (result.isSuccessful) {
            hashes[source.displayName] = fingerprint(result.getResult().orElseThrow().toString())
        }
    }
    return hashes
}

/** A short structural fingerprint: 64 bits of SHA-256 is ample for benchmark parity checks. */
private fun fingerprint(text: String): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(16)

private fun parserConfiguration(config: BenchConfig): ParserConfiguration {
    val isJml = config.corpus == "jml"
    val parserConfiguration =
        ParserConfiguration()
            .setProcessJml(isJml)
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
    if (config.mode == BenchMode.GRAMMAR) {
        parserConfiguration.processors.clear()
    }
    return parserConfiguration
}

private fun megabytesPerSecond(
    bytes: Long,
    millis: Double,
): Double = (bytes / 1_048_576.0) / (millis / 1000.0)

private fun parseOnce(
    sources: List<SourceFile>,
    parserConfiguration: ParserConfiguration,
): Pair<List<String>, Int> {
    val parser = JavaParser(parserConfiguration)
    val failures = mutableListOf<String>()
    var parsed = 0
    for (source in sources) {
        val result = parser.parse(source.text)
        if (result.isSuccessful) {
            parsed++
        } else {
            failures += source.displayName
        }
    }
    return failures.sorted() to parsed
}

private fun loadSources(
    corpus: String,
    repoRoot: Path,
): List<SourceFile> {
    val roots =
        when {
            corpus == "plain" ->
                listOf(
                    repoRoot.resolve("javaparser-core/src/main/java"),
                    repoRoot.resolve("javaparser-symbol-solver-core/src/main/java"),
                )

            corpus == "jml" -> listOf(repoRoot.resolve("jmlparser-jml-tests/src/test/resources/fullexamples"))

            corpus.startsWith("path:") -> listOf(Paths.get(corpus.removePrefix("path:")).toAbsolutePath().normalize())

            else -> throw IllegalArgumentException("Unknown corpus: $corpus (expected plain, jml, or path:<dir>)")
        }

    return roots
        .flatMap { root ->
            require(Files.isDirectory(root)) { "Corpus directory does not exist: $root" }
            Files.walk(root).use { stream ->
                stream
                    .asSequence()
                    .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".java") }
                    .map { path ->
                        val display = if (path.startsWith(repoRoot)) repoRoot.relativize(path).toString() else path.toString()
                        SourceFile(display, Files.readString(path))
                    }
                    .toList()
            }
        }
        .sortedBy { it.displayName }
}

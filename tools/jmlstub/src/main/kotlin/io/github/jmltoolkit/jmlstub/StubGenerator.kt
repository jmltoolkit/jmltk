/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.jmlstub

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.TypeDeclaration
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.io.path.Path

/**
 * Generator for creating Java stub files from existing Java source code.
 * The generator preserves JML specifications while replacing method bodies with empty implementations.
 *
 * @author Alexander Weigl
 * @version 1 (7/19/26)
 */
class StubGenerator(private val config: StubConfig) : Callable<Set<CompilationUnit>> {
    internal val parser: JavaParser by lazy {
        val parserConfig = ParserConfiguration()
        JavaParser(parserConfig)
    }

    override fun call(): Set<CompilationUnit> {
        val compilationUnits = generate(config.files)
        val types = run(compilationUnits)
        return types.map { it.findCompilationUnit().get() }.toSet()
    }

    fun run(compilationUnits: List<List<CompilationUnit>>): MutableCollection<TypeDeclaration<*>> {
        val types = compilationUnits.flatten().flatMap { it.types() }
        val fqdns = types
            .map { it.fullyQualifiedName.get() }
            .toSortedSet()
        val byFqdn = types.groupBy { it.fullyQualifiedName.get() }

        print("Found the following types: $fqdns")

        val survived = mutableMapOf<String, TypeDeclaration<*>>()

        for ((fqdn, seq) in byFqdn) {
            val excluded = typeIn(fqdn, config.excludeClassesRegex)
            if (excluded) continue

            val merge = typeIn(fqdn, config.mergeClassesRegex)

            if (!merge) {
                survived[fqdn] = seq.first()
            } else {
                survived[fqdn] = JmlStubCombiner(config).combine(seq).types.first()
            }
        }
        return survived.values
    }

    fun run(cu: CompilationUnit) = run(listOf(listOf(cu))).first()

    private fun generate(files: List<Path>): List<List<CompilationUnit>> =
        listOf(config.jreClasses.map { Class.forName(it) }.map { createStub(it) }) +
        files.map { rootPath ->
            rootPath.toFile().walkTopDown().mapNotNull { f ->
                when {
                    f.name.endsWith(".class") -> createStub(f.toPath())
                    f.name.endsWith(".java") -> readJavaFile(f.toPath())
                    else -> null
                }
            }.toList()
        }

    private fun createStub(it: Class<*>) = JREClassStubGenerator(it).generate()
    private fun readJavaFile(it: Path): CompilationUnit = parser.parse(it).result.get()
    private fun createStub(it: Path) = ClassStubGenerator(it).generate()

    /**
     * Combine multiple JML specifications into a single specification.
     */
    fun combineSpecifications(specs: List<String>): String = specs.filter { it.isNotBlank() }.joinToString("\n    ")
}

/**
 * Configuration options for JML stub generation.
 */
data class StubConfig(
    val jmlKeys: List<String> = emptyList(),
    val excludeClasses: List<String> = listOf(),
    val mergeClasses: List<String> = listOf(),
    val addGeneratedAnnotation: Boolean = true,
    val throwUnsupportedForStubs: Boolean = false,
    val preserveContracts: Boolean = false,
    val outputDir: Path = Path("out"),
    val files: List<Path> = listOf(),
    val jreClasses: List<String> = listOf(),

) {
    val excludeClassesRegex by lazy { excludeClasses.map { matchesPattern(it) } }
    val mergeClassesRegex by lazy { mergeClasses.map { matchesPattern(it) } }
}

/**
 * Matches a fully qualified name against a Gradle-style pattern.
 *
 * Examples:
 * - "com.example.MyTest" matches "*.MyTest"
 * - "com.example.MyTest" matches "com.example.*"
 * - "com.example.MyTest" matches "com.**.MyTest"
 * - "com.example.MyTest" matches "**.MyTest"
 * - "com.example.MyTest" matches "com.example.MyTest"
 */
internal fun matchesPattern(pattern: String): Regex {
    // Convert Gradle-style pattern to regex
    // Escape special regex characters first
    var regexPattern = pattern
        .replace(".", "\\.")  // Escape dots (package separators)
        .replace("?", "\\?")  // Escape question marks
        .replace("+", "\\+")  // Escape plus signs
        .replace("^", "\\^")  // Escape carets
        .replace("$", "\\$")  // Escape dollar signs
        .replace("[", "\\[")  // Escape opening brackets
        .replace("]", "\\]")  // Escape closing brackets
        .replace("(", "\\(")  // Escape opening parentheses
        .replace(")", "\\)")  // Escape closing parentheses
        .replace("{", "\\{")  // Escape opening braces
        .replace("}", "\\}")  // Escape closing braces
        .replace("|", "\\|")  // Escape pipes

    // Replace ** with a placeholder first (matches anything including dots)
    regexPattern = regexPattern.replace("**", "§DOUBLESTAR§")

    // Replace * with regex that matches anything except dots
    regexPattern = regexPattern.replace("*", "[^.]*")

    // Replace placeholder back with .* (matches anything including dots)
    regexPattern = regexPattern.replace("§DOUBLESTAR§", ".*")

    // Anchor the pattern to match the entire string
    val anchoredPattern = "^$regexPattern$"

    return anchoredPattern.toRegex()
}

/**
 * Checks if a fully qualified name matches any of the given patterns.
 * Patterns support Gradle-style wildcards:
 * - `*` matches any characters except the dot separator
 * - `**` matches any characters including the dot separator
 * - Exact match otherwise
 *
 * @param fqdn the fully qualified name to check (e.g., "com.example.MyClass")
 * @param patterns the list of patterns to match against
 * @return true if fqdn matches any pattern
 */
internal fun typeIn(fqdn: String, patterns: List<Regex>) = patterns.any { it.matches(fqdn) }

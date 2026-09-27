/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver

/**
 * Helpers to parse Java/JML sources in the same way the language server parses
 * documents (JML processing enabled, optional symbol solver attached).
 */
object JmlTestSupport {
    fun javaParser(withSymbolSolver: Boolean = true): JavaParser {
        val config = ParserConfiguration()
        config.isProcessJml = true
        config.languageLevel = ParserConfiguration.LanguageLevel.JAVA_25
        if (withSymbolSolver) {
            config.setSymbolResolver(
                JavaSymbolSolver(
                    CombinedTypeSolver(
                        ClassLoaderTypeSolver(ClassLoader.getSystemClassLoader())
                    )
                )
            )
        }
        return JavaParser(config)
    }

    fun parse(code: String, withSymbolSolver: Boolean = true): CompilationUnit {
        val result = javaParser(withSymbolSolver).parse(code)
        check(result.isSuccessful) {
            "Parsing failed:\n" + result.problems.joinToString("\n") { it.verboseMessage }
        }
        return result.result.get()
    }
}

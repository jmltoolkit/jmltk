/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import io.github.jmltoolkit.jmlstub.JmlStubCombiner
import io.github.jmltoolkit.jmlstub.JmlStubConfig
import io.github.jmltoolkit.jmlstub.JmlStubGenerator
import java.io.File

/**
 * Command for generating and combining JML stub files.
 * 
 * Generation: Reads Java class files and generates Java stubs using JavaParser AST.
 * Combination: Merges multiple JML specifications into unified specifications.
 *
 * @author Alexander Weigl
 * @version 1 (7/19/26)
 */
class JmlStubCommand : CliktCommand(name = "jmlstub") {
    
    override fun help(context: Context): String = 
        "Generate Java stubs from source files and combine JML specifications"

    private val outputDir by option("-o", "--output", help = "Output directory for generated stubs")
        .file()
        .default(File("stubs"))

    private val combine by option("--combine", help = "Combine multiple files into single stub")
        .flag()

    private val noJml by option("--no-jml", help = "Disable JML processing")
        .flag()

    private val throwUnsupported by option("--throw-unsupported", help = "Throw UnsupportedOperationException in stub methods")
        .flag()

    private val preserveContracts by option("--preserve-contracts", help = "Preserve JML contracts in stubs")
        .flag(default = true)

    private val files by argument("FILES").file().multiple()

    override fun run() {
        if (files.isEmpty()) {
            echo("No input files specified")
            return
        }

        val config = JmlStubConfig(
            processJml = !noJml,
            throwUnsupportedForStubs = throwUnsupported,
            preserveContracts = preserveContracts
        )

        when {
            combine -> combineFiles(config)
            else -> generateStubs(config)
        }
    }

    private fun generateStubs(config: JmlStubConfig) {
        val generator = JmlStubGenerator(config)
        val inputFiles = files.toList()

        echo("Generating stubs for ${inputFiles.size} files...")
        
        val stubs = generator.generate(inputFiles)
        
        outputDir.mkdirs()
        
        stubs.forEach { cu ->
            val fileName = cu.storage.map { it.fileName }.orElse("stub.java")
            val outputFile = File(outputDir, fileName)
            outputFile.writeText(cu.toString())
            echo("Generated: ${outputFile.absolutePath}")
        }

        echo("Successfully generated ${stubs.size} stub files")
    }

    private fun combineFiles(config: JmlStubConfig) {
        val combiner = JmlStubCombiner()
        val inputFiles = files.toList()

        echo("Combining ${inputFiles.size} files...")
        
        val combined = combiner.combineFromFiles(inputFiles)
        
        outputDir.mkdirs()
        val packageName = combined.packageDeclaration?.nameAsString?.replace('.', '/') ?: ""
        val outputSubdir = File(outputDir, packageName).also { it.mkdirs() }
        val outputFile = File(outputSubdir, "CombinedStub.java")
        
        outputFile.writeText(combined.toString())
        echo("Combined stub written to: ${outputFile.absolutePath}")
    }
}
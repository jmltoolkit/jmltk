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
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.github.jmltoolkit.jmlstub.StubConfig
import io.github.jmltoolkit.jmlstub.StubGenerator
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * Command for generating and combining JML stub files.
 *
 * Generation: Reads Java class files and generates Java stubs using JavaParser AST.
 * Combination: Merges multiple JML specifications into unified specifications.
 *
 * @author Alexander Weigl
 * @version 1 (7/19/26)
 */
class JmlStubCommand : CliktCommand(name = "stubby") {

    override fun help(context: Context): String =
        "Generate Java stubs from class files, JRE classes. Is able to create an effective JML specifications folder"

    private val outputDir by option("-o", "--output", help = "Output directory for generated stubs")
        .path()
        .default(Path("stubs"))

    private val merge by option(
        "--merge",
        help = "Methods/classes given in this set are merged. Normally, the first one wins."
    ).multiple()

    private val classes by option("--class").multiple().help("specifies a list of classes using fqn")

    private val files by argument("FILES").path(mustExist = true, mustBeReadable = true).multiple()

    override fun run() {
        if (files.isEmpty()) {
            echo("No input files specified")
            return
        }
        val config = StubConfig(outputDir = outputDir, files = files, jreClasses = classes)
        val generator = StubGenerator(config)

        echo("Generating stubs for ${files.size} files...")

        val stubs = generator.call()

        outputDir.createDirectories()
        stubs.forEach { cu ->
            val filename = (cu.primaryType.orElse(null) ?: cu.types().first()).nameAsString
            val packageName = cu.packageDeclaration.orElse(null)?.nameAsString
            val outputFile = (if (packageName != null) outputDir.resolve(packageName) else outputDir)
                .resolve("$filename.java")
            outputFile.writeText(cu.toString())
            echo("Generated: ${outputFile.toAbsolutePath()}")
        }

        echo("Successfully generated ${stubs.size} stub files")
    }
}

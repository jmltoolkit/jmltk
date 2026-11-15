/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.javaparser.JmlTkBuild
import java.io.File
import java.io.IOException

open class PpCommand(name: String = "pp") : CliktCommand(name = name) {
    init {
        versionOption(JmlTkBuild.PROJECT_VERSION)
    }

    private val prettyPrint by option("-p", "--pretty-print", help = "Format the input before rendering").flag()
    private val format by option("-f", "--format", help = "Output format")
        .enum<OutputFormat>()
        .default(OutputFormat.TEXT)
    private val language by option("--language", help = "Input language")
        .enum<Language>()
        .default(Language.JAVA)
    private val snippet by option("--snippet", help = "How to interpret Java input")
        .enum<SnippetKind>()
        .default(SnippetKind.AUTO)
    private val width by option("-w", "--width", help = "Pretty-print line width").int().default(100)
    private val style by option("--style", help = "Pygments/minted style name for LaTeX")
    private val minted by option("--minted", help = "LaTeX: emit \\PYG body content (default)").flag()
    private val latexRaw by option("--latex-raw", help = "LaTeX: emit \\jml* macros").flag()
    private val latexPreamble by option("--latex-preamble", help = "LaTeX raw: print macro/color definitions").flag()
    private val htmlFull by option("--html-full", help = "HTML: emit a full document").flag()
    private val css by option("--css", help = "HTML: inline this CSS file").file(mustExist = true, canBeDir = false)
    private val noColor by option("--no-color", help = "Force plain text").flag()
    private val files by argument("FILES").file().multiple()

    override fun help(context: Context): String = "Pretty-print and syntax-highlight Java/JML source."

    override fun run() {
        if (minted && latexRaw) {
            throw UsageError("--minted and --latex-raw are mutually exclusive")
        }
        if (latexPreamble && (format != OutputFormat.LATEX || !latexRaw)) {
            throw UsageError("--latex-preamble requires --format latex and --latex-raw")
        }

        if (latexPreamble) {
            print(LatexRenderer(raw = true).preamble())
            return
        }

        val inputs =
            try {
                InputReader().read(files)
            } catch (e: InputException) {
                throw CliktError(e.message ?: "Cannot read input", statusCode = 1)
            }
        val cssText =
            try {
                css?.readText()
            } catch (e: IOException) {
                throw CliktError("Cannot read ${css?.path}: ${e.message}", statusCode = 1)
            }

        val options =
            RenderOptions(
                prettyPrint = prettyPrint,
                format = format,
                language = language,
                snippet = snippet,
                width = width,
                style = style,
                latexRaw = latexRaw,
                htmlFull = htmlFull,
                css = cssText,
                color = ColorSupport.enabled(noColor),
            )
        val facade = PrettyPrintingFacade(warning = { System.err.println(it) })
        val rendered =
            try {
                facade.renderAll(inputs, options)
            } catch (e: PrettyPrintingException) {
                throw CliktError(e.message ?: "Cannot render input", statusCode = 3)
            }
        print(rendered)
        if (!rendered.endsWith("\n")) println()
    }
}

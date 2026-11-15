/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

/** Runs the parse -> optional format -> highlight -> render pipeline. */
class PrettyPrintingFacade(
    private val parser: SnippetParser = SnippetParser(),
    private val highlighter: Highlighter = Highlighter(),
    private val formatter: JmlFmt = JmlFmt(),
    private val warning: (String) -> Unit = {},
) {
    fun render(
        input: String,
        options: RenderOptions = RenderOptions(),
    ): String {
        val parsed = parser.parse(input, options.language, options.snippet)
        if (parsed.kind == ParsedKind.PLAIN && !options.prettyPrint) {
            warning("Warning: input could not be parsed; output is not highlighted")
        }
        val source = if (options.prettyPrint) format(parsed, input, options) else input
        if (options.format == OutputFormat.TEXT) return TextRenderer().render(source)

        val highlighted =
            if (options.prettyPrint) {
                parser.parse(source, options.language, SnippetKind.AUTO)
            } else {
                parsed
            }
        val spans = highlighter.highlight(highlighted)

        return when (options.format) {
            OutputFormat.TEXT -> TextRenderer().render(source)

            OutputFormat.ANSI -> AnsiRenderer(options.color).render(spans)

            OutputFormat.HTML -> {
                val fragment = HtmlRenderer().renderFragment(spans)
                if (options.htmlFull) HtmlRenderer().renderDocument(fragment, options.css) else fragment
            }

            OutputFormat.LATEX -> {
                val body = LatexRenderer(raw = options.latexRaw).render(spans)
                if (!options.latexRaw && options.style != null) {
                    "\\usemintedstyle{${options.style}}\n$body"
                } else {
                    body
                }
            }
        }
    }

    /**
     * Renders every input and joins the results with a blank line. For a full HTML document the
     * fragments are wrapped once, so several inputs produce one valid document.
     */
    fun renderAll(
        inputs: List<String>,
        options: RenderOptions = RenderOptions(),
    ): String {
        if (options.format == OutputFormat.HTML && options.htmlFull) {
            val fragments = inputs.map { render(it, options.copy(htmlFull = false)).trimEnd('\n') }
            return HtmlRenderer().renderDocument(fragments.joinToString("\n\n"), options.css)
        }
        return inputs.joinToString("\n\n") { render(it, options).trimEnd('\n') } + "\n"
    }

    private fun format(
        parsed: ParsedSource,
        input: String,
        options: RenderOptions,
    ): String {
        if (parsed.kind == ParsedKind.PLAIN) {
            throw PrettyPrintingException("Cannot pretty-print: the input does not parse")
        }
        return try {
            when {
                parsed.kind == ParsedKind.COMPILATION_UNIT -> formatter.pp(input, options.width)
                else -> parsed.nodes.joinToString("\n") { it.toString().trimEnd() }
            }
        } catch (e: Exception) {
            throw PrettyPrintingException("Cannot pretty-print: ${e.message}")
        }
    }
}

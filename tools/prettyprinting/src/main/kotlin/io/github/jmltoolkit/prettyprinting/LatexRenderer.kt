/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

/**
 * LaTeX renderer. In minted mode it emits Pygments-compatible `\PYG{class}{...}` body content; in
 * raw mode it emits semantic `\jml<class>{...}` macros whose definitions come from [preamble].
 */
class LatexRenderer(private val raw: Boolean = false) {
    fun render(spans: List<Span>): String =
        buildString {
            for ((style, text) in groups(spans)) {
                if (style == StyleClass.TEXT) {
                    append(if (text.isBlank()) text else escapeLatex(text))
                } else {
                    appendStyled(this, style, text)
                }
            }
        }

    private fun appendStyled(
        result: StringBuilder,
        style: StyleClass,
        text: String,
    ) {
        text.split("\n").forEachIndexed { index, line ->
            if (index > 0) result.append('\n')
            if (line.isEmpty()) return@forEachIndexed
            if (raw) {
                result.append("\\jml").append(style.latexMacro).append("{")
                result.append(escapeLatex(line)).append("}")
            } else {
                result.append("\\PYG{").append(style.cssClass).append("}{")
                result.append(escapePygments(line)).append("}")
            }
        }
    }

    fun preamble(): String =
        buildString {
            append("\\usepackage[T1]{fontenc}\n")
            append("\\usepackage{textcomp}\n")
            append("\\usepackage{xcolor}\n")
            for (style in StyleClass.values()) {
                if (style == StyleClass.TEXT) continue
                val attributes = pygmentsDefault[style]
                val colorName = "jmltk" + style.latexMacro.replaceFirstChar { it.uppercase() }
                if (attributes?.rgb != null) {
                    append("\\definecolor{")
                    append(colorName)
                    append("}{HTML}{")
                    append(String.format("%06X", attributes.rgb))
                    append("}\n")
                }
                append("\\newcommand{\\jml")
                append(style.latexMacro)
                append("}[1]{")
                if (attributes?.rgb != null) append("\\textcolor{").append(colorName).append("}{")
                if (attributes?.bold == true) append("\\textbf{")
                if (attributes?.italic == true) append("\\textit{")
                append("#1")
                if (attributes?.italic == true) append("}")
                if (attributes?.bold == true) append("}")
                if (attributes?.rgb != null) append("}")
                append("}\n")
            }
        }

    private fun groups(spans: List<Span>): List<Pair<StyleClass, String>> {
        val groups = mutableListOf<Pair<StyleClass, StringBuilder>>()
        for (span in spans) {
            if (groups.isNotEmpty() && groups.last().first == span.style) {
                groups.last().second.append(span.text)
            } else {
                groups.add(span.style to StringBuilder(span.text))
            }
        }
        return groups.map { it.first to it.second.toString() }
    }

    private fun escapePygments(text: String): String =
        buildString {
            for (c in text) {
                when (c) {
                    '\\' -> append("\\PYGZbs{}")
                    '{' -> append("\\PYGZob{}")
                    '}' -> append("\\PYGZcb{}")
                    '^' -> append("\\PYGZca{}")
                    '_' -> append("\\PYGZus{}")
                    '&' -> append("\\PYGZam{}")
                    '<' -> append("\\PYGZlt{}")
                    '>' -> append("\\PYGZgt{}")
                    '#' -> append("\\PYGZsh{}")
                    '%' -> append("\\PYGZpc{}")
                    '$' -> append("\\PYGZdl{}")
                    '-' -> append("\\PYGZhy{}")
                    '\'' -> append("\\PYGZsq{}")
                    '"' -> append("\\PYGZdq{}")
                    '~' -> append("\\PYGZti{}")
                    else -> append(c)
                }
            }
        }

    private fun escapeLatex(text: String): String =
        buildString {
            for (c in text) {
                when (c) {
                    '\\' -> append("\\textbackslash{}")
                    '{' -> append("\\{")
                    '}' -> append("\\}")
                    '#' -> append("\\#")
                    '%' -> append("\\%")
                    '&' -> append("\\&")
                    '_' -> append("\\_")
                    '$' -> append("\\$")
                    '^' -> append("\\textasciicircum{}")
                    '~' -> append("\\textasciitilde{}")
                    '<' -> append("\\textless{}")
                    '>' -> append("\\textgreater{}")
                    '-' -> append("{-}")
                    '\'' -> append("\\textquotesingle{}")
                    '"' -> append("\\textquotedbl{}")
                    else -> append(c)
                }
            }
        }
}

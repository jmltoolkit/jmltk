/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

enum class OutputFormat { TEXT, ANSI, HTML, LATEX }

data class RenderOptions(
    val prettyPrint: Boolean = false,
    val format: OutputFormat = OutputFormat.TEXT,
    val language: Language = Language.JAVA,
    val snippet: SnippetKind = SnippetKind.AUTO,
    val width: Int = 100,
    val style: String? = null,
    val latexRaw: Boolean = false,
    val htmlFull: Boolean = false,
    val css: String? = null,
    val color: Boolean = true,
)

class PrettyPrintingException(message: String) : RuntimeException(message)

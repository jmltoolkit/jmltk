/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

/** ANSI renderer using the Pygments `default` palette quantized to xterm-256 colors. */
class AnsiRenderer(private val color: Boolean = true) {
    fun render(spans: List<Span>): String =
        buildString {
            for (span in spans) {
                if (!color || span.style == StyleClass.TEXT || span.text.isBlank()) {
                    append(span.text)
                    continue
                }
                val attributes = pygmentsDefault[span.style]
                val codes = mutableListOf<String>()
                if (attributes?.bold == true) codes.add("1")
                if (attributes?.italic == true) codes.add("3")
                attributes?.rgb?.let {
                    codes.add("38")
                    codes.add("5")
                    codes.add(xterm256(it).toString())
                }
                if (codes.isEmpty()) {
                    append(span.text)
                } else {
                    append("\u001B[")
                    append(codes.joinToString(";"))
                    append("m")
                    append(span.text)
                    append("\u001B[0m")
                }
            }
        }
}

object ColorSupport {
    fun enabled(
        noColorFlag: Boolean,
        env: (String) -> String? = { System.getenv(it) },
    ): Boolean = !noColorFlag && env("NO_COLOR").isNullOrEmpty()
}

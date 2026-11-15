/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

/** HTML renderer using Pygments class names. No CSS is bundled. */
class HtmlRenderer {
    fun renderFragment(spans: List<Span>): String {
        val body =
            buildString {
                for (span in spans) {
                    val escaped = escape(span.text)
                    if (span.style == StyleClass.TEXT) {
                        append(escaped)
                    } else {
                        append("<span class=\"")
                        append(span.style.cssClass)
                        append("\">")
                        append(escaped)
                        append("</span>")
                    }
                }
            }
        return "<pre class=\"highlight\"><code>$body</code></pre>\n"
    }

    fun renderDocument(
        fragment: String,
        css: String?,
    ): String =
        buildString {
            append("<!DOCTYPE html>\n")
            append("<html>\n")
            append("<head>\n")
            append("<meta charset=\"utf-8\">\n")
            if (css != null) {
                append("<style>\n")
                append(css)
                append("\n</style>\n")
            }
            append("</head>\n")
            append("<body>\n")
            append(fragment)
            append("</body>\n")
            append("</html>\n")
        }

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

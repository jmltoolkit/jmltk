/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.hover

class JmlDocumentationIndex {
    private val index by lazy {
        val map = mutableMapOf<String, String>()
        val lines = javaClass.getResourceAsStream("/doc.md")
            ?.bufferedReader()?.readLines()
            ?: error("Could not load /doc/index.properties")

        var keys = setOf<String>()
        val builder = StringBuilder()
        for (line in lines) {
            if (line.startsWith("%keywords")) {
                keys = line.substringAfter(' ').splitToSequence(' ').toSet()
            } else {
                if (line.startsWith("-----")) {
                    val s = builder.toString().intern()
                    for (key in keys) {
                        map[key] = s
                    }
                    builder.clear()
                } else {
                    builder.appendLine(line)
                }
            }
        }
        map
    }

    fun get(entry: String) = index[entry]
}

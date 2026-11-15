/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import java.io.File
import java.io.IOException

class InputException(message: String) : RuntimeException(message)

/** Reads code from files and stdin (`-`), rejecting an interactive call with no input. */
class InputReader(
    private val stdin: () -> String = { System.`in`.reader(Charsets.UTF_8).readText() },
    private val terminal: () -> Boolean = { System.console() != null },
) {
    fun read(files: List<File>): List<String> {
        if (files.isEmpty()) {
            if (terminal()) throw InputException("No input files and stdin is a terminal")
            return listOf(readStdin())
        }

        var stdinRead = false
        return files.map { file ->
            if (file.path == "-") {
                if (stdinRead) throw InputException("stdin given more than once")
                stdinRead = true
                readStdin()
            } else {
                try {
                    file.readText(Charsets.UTF_8)
                } catch (e: IOException) {
                    throw InputException("Cannot read ${file.path}: ${e.message}")
                }
            }
        }
    }

    private fun readStdin(): String =
        try {
            stdin()
        } catch (e: IOException) {
            throw InputException("Cannot read stdin: ${e.message}")
        }
}

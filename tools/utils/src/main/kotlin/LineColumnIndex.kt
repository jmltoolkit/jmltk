/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.utils

import com.github.javaparser.Position
import com.github.javaparser.Range

/**
 * The index is 1-based. The first character also begins in line 1 and column 1.
 *
 * @author Alexander Weigl
 * @version 1 (18.10.22)
 */
class LineColumnIndex(private val content: String) {
    var lineOffsets: IntArray =
        IntArray(1 + content.chars().filter { it: Int -> it == '\n'.code }.count().toInt())

    init {
        var cur = 1
        val chars = content.toCharArray()
        for (i in chars.indices) {
            if (chars[i] == '\n') lineOffsets[cur++] = i + 1
        }
    }

    fun substring(range: Range): String = substring(range.begin, range.end)

    private fun substring(begin: Position, end: Position): String = substring(begin.line, begin.column, end.line, end.column)

    fun substring(beginLine: Int, beginColumn: Int, endLine: Int, endColumn: Int): String {
        val a = positionToOffset(beginLine, beginColumn)
        val b = positionToOffset(endLine, endColumn)
        return content.substring(a, b + 1)
    }

    fun positionToOffset(p: Position): Int = positionToOffset(p.line, p.column)

    fun positionToOffset(line: Int, column: Int): Int = lineOffsets[line - 1] + column - 1
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.smt.model

import com.github.javaparser.resolution.types.ResolvedType
import java.io.PrintWriter

/**
 * @author Alexander Weigl
 * @version 1 (07.08.22)
 */
class SList(stype: SmtType?, javaType: ResolvedType?, private val value: List<SExpr>) : SExpr(javaType, stype) {
    override fun appendTo(writer: PrintWriter) {
        writer.write('('.code)
        for (i in value.indices) {
            value[i].appendTo(writer)
            if (i < value.size - 1) writer.write(' '.code)
        }
        writer.write(')'.code)
    }

    fun get(i: Int): SExpr = value[i]
}

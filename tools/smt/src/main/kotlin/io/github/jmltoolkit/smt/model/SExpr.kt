/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.smt.model

import com.github.javaparser.resolution.types.ResolvedType
import io.github.jmltoolkit.smt.solver.AppendableTo
import java.io.PrintWriter
import java.io.StringWriter

/**
 * @author Alexander Weigl
 * @version 1 (07.08.22)
 */
abstract class SExpr(
    var javaType: ResolvedType? = null,
    var smtType: SmtType? = null
) : AppendableTo {

    fun asList(): SList = this as SList

    fun asSymbolValue(): String = (this as SAtom).value

    override fun toString(): String {
        val sw = StringWriter()
        appendTo(PrintWriter(sw))
        return sw.toString()
    }
}

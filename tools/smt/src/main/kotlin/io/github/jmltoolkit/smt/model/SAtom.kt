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
class SAtom(stype: SmtType?, javaType: ResolvedType?, val value: String) : SExpr(javaType, stype) {
    override fun appendTo(writer: PrintWriter) {
        writer.write(value)
    }
}

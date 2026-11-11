/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc

import jjbmc.trace.TraceInformation

data class Assignment(
    val parameterName: String,
    var value: String,
    var jbmcVarname: String = "",
    var lineNumber: Int = 0,
    var guessedValue: Any? = null,
    var guess: String? = null,
) {
    override fun toString(): String {
        val v = guessedValue?.toString() ?: value
        val lhs = TraceInformation.applyExpressionMap(this.guess)
        return "in line $lineNumber: $lhs ($jbmcVarname) = $v"
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.jml2java

import com.github.javaparser.ast.Node

object VerifyState {
    fun verify(n: Node) {
        for (c in n.childNodes) {
            verify(c)
            check(
                !(c.parentNode == null || c.parentNode.get() !== n)
            ) { "Broken parent relation for node: " + c }
        }
    }
}

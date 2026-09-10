/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.jml2java

import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.visitor.ModifierVisitor
import com.github.javaparser.ast.visitor.Visitable

class ReplaceVariable(val orig: Parameter, val replacement: String) : ModifierVisitor<Void>() {
    override fun visit(n: NameExpr, arg: Void): Visitable {
        if (n.nameAsString == orig.nameAsString) {
            n.setName(replacement)
        }
        return super.visit(n, arg)
    }
}

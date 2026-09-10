/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.utils

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import jjbmc.FunctionNameVisitor.TestBehaviour

class TestOptionsListener : VoidVisitorAdapter<MutableList<TestOptions>>() {
    override fun visit(n: MethodDeclaration, testOptions: MutableList<TestOptions>) {
        val to = getQualifiedNameTestOptions(TestBehaviour.Verifyable, 5, n.resolve().getQualifiedName())
        testOptions.add(to)
    }
}

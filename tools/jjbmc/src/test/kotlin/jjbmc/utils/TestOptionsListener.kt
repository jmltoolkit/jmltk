/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.utils;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import jjbmc.FunctionNameVisitor.TestBehaviour;

import java.util.List;

public class TestOptionsListener extends VoidVisitorAdapter<List<TestOptions>> {
    @Override
    public void visit(MethodDeclaration n, List<TestOptions> testOptions) {
        TestOptions to =
                new TestOptions(TestBehaviour.Verifyable, 5, n.resolve().getQualifiedName());
        testOptions.add(to);
    }
}

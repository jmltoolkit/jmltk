/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

/**
 * Test for issue 1482: https://github.com/javaparser/javaparser/issues/1482
 * When trying to resolve a MethodCallExpr within a multi catch, an UnsupportedOperationException is thrown.
 */
class MultiCatchMethodCallExprTest extends AbstractResolutionTest {

    @Test
    void issue1482() {
        CompilationUnit cu = parseSample("Issue1482");
        cu.accept(new Visitor(), null);
    }

    private static class Visitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodCallExpr n, Void arg) {
            if (n.getArguments().size() != 0) {
                JavaParserFacade.get(new ReflectionTypeSolver(true)).getType(n.getArgument(0));
            }
        }
    }
}

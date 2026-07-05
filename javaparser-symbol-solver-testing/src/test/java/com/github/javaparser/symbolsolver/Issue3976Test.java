/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue3976Test extends AbstractResolutionTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_9)
    void test() {
        String testCase = "import java.util.Map;\n"
                + "public class Foo {\n"
                + "		public Object m() {\n"
                + "			return Map.of(\"k0\", 0, \"k1\", 1D);\n"
                + "		}\n"
                + "}";

        CompilationUnit cu = JavaParserAdapter.of(createParserWithResolver(defaultTypeSolver()))
                .parse(testCase);

        MethodCallExpr methodCallExpr = cu.findFirst(MethodCallExpr.class).get();

        ResolvedType rt = methodCallExpr.calculateResolvedType();
        assertEquals("java.util.Map<java.lang.String, java.lang.Number>", rt.describe());
    }
}

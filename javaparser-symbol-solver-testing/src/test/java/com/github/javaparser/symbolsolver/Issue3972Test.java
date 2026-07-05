/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import org.junit.jupiter.api.Test;

public class Issue3972Test extends AbstractResolutionTest {

    @Test
    void test() {
        JavaParserAdapter parser = JavaParserAdapter.of(createParserWithResolver(defaultTypeSolver()));

        CompilationUnit cu = parser.parse("class C {\n" + "    void f() throws NoSuchMethodException {\n"
                + "        Class cls = getClass();\n"
                + "        cls.getSuperclass().getSuperclass().toString();\n"
                + "    }\n"
                + "}");

        for (MethodCallExpr methodCallExpr : cu.findAll(MethodCallExpr.class)) {
            methodCallExpr.getScope().ifPresent(s -> {
                String type = s.calculateResolvedType().describe();
                assertNotNull(type);
            });
        }
    }
}

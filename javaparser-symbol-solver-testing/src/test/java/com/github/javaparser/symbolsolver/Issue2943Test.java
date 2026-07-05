/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class Issue2943Test {
    @Test
    public void testPeek() {
        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver(false)));
        StaticJavaParser.setConfiguration(config);

        CompilationUnit cu = StaticJavaParser.parse("package foo;\n"
                + "import java.util.stream.Collectors;\n"
                + "import java.util.stream.IntStream;\n"
                + "import java.util.stream.Stream;\n"
                + "public class TestPeek {\n"
                + "    public void foo() {\n"
                + "        Stream.of(1,2,3).peek(info -> { System.out.println(info); }).collect(Collectors.toList());\n"
                + "    }\n"
                + "}");

        for (MethodCallExpr methodCallExpr : cu.findAll(MethodCallExpr.class)) {
            assertDoesNotThrow(methodCallExpr::resolve);
        }
    }
}

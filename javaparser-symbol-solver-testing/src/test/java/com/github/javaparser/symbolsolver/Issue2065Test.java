/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue2065Test extends AbstractResolutionTest {

    @Test
    void test() {
        String code = "import java.util.stream.Stream;\n" + "\n"
                + "public class A {\n"
                + "    public void test(){\n"
                + "        Stream.of(1,2).reduce((a, b) -> Math.max(a, b));\n"
                + "    }\n"
                + "}";

        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver(false)));
        StaticJavaParser.setConfiguration(config);

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<MethodCallExpr> exprs = cu.findAll(MethodCallExpr.class);
        for (MethodCallExpr expr : exprs) {
            if (expr.getNameAsString().contentEquals("max")) {
                assertEquals("java.lang.Math.max(int, int)", expr.resolve().getQualifiedSignature());
            }
        }
    }
}

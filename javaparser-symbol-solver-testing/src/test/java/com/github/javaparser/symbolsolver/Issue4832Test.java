/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

public class Issue4832Test extends AbstractResolutionTest {

    @Test()
    void test() {
        String code = "public class Stream_NoImports {\n"
                + "    public java.util.stream.Stream<java.lang.String> simpleToList() {\n"
                + "       return java.util.stream.Stream.of(\"a\", \"b\", \"c\");\n"
                + "    }\n"
                + "}";

        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
        StaticJavaParser.setConfiguration(config);
        CompilationUnit cu = StaticJavaParser.parse(code);

        // Find all the calculations with two sides:
        cu.findAll(MethodCallExpr.class).forEach(mc -> {
            // Find out what type it has:
            ResolvedType resolvedType = mc.calculateResolvedType();
            assertEquals("java.util.stream.Stream<java.lang.String>", resolvedType.describe());
        });
    }
}

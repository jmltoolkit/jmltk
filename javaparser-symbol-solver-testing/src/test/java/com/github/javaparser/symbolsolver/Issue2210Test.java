/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue2210Test extends AbstractResolutionTest {

    @BeforeEach
    void setup() {}

    @Test
    void test2210Issue() {
        // Source code
        String sourceCode = "class A {" + " public void m() {\n"
                + "   java.util.Arrays.asList(1, 2, 3).forEach(System.out::println);"
                + " }\n"
                + "}";
        // Setup symbol solver
        ParserConfiguration configuration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver())));
        // Setup parser
        JavaParser parser = new JavaParser(configuration);
        CompilationUnit cu = parser.parse(sourceCode).getResult().get();
        // Test
        MethodReferenceExpr expr = Navigator.demandNodeOfGivenClass(cu, MethodReferenceExpr.class);
        ResolvedType type = expr.calculateResolvedType();
        assertEquals("java.util.function.Consumer<? super T>", type.describe());
    }
}

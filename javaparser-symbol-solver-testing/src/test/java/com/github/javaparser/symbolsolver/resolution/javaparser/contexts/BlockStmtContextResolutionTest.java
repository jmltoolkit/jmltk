/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution.javaparser.contexts;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
class BlockStmtContextResolutionTest extends AbstractResolutionTest {

    @BeforeEach
    void setup() {}

    // issue #3526
    @Test
    void must_be_resolved_from_previous_declaration() {
        String src = "public class Example {\n"
                + "    int a = 3;\n"
                + "    public void bla() {\n"
                + "        a = 7; // 'a' must be resolved as int not String\n"
                + "        String a = \"\";\n"
                + "        a = \"test\";\n"
                + "    }\n"
                + "}";
        ParserConfiguration configuration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver())));
        StaticJavaParser.setConfiguration(configuration);
        CompilationUnit cu = StaticJavaParser.parse(src);
        AssignExpr expr = cu.findFirst(AssignExpr.class).get();
        ResolvedType rt = expr.calculateResolvedType();
        assertEquals("int", rt.describe());
    }

    @Test
    void must_be_resolved_from_previous_declaration_second_declaration_of_the_same_field_name() {
        String src = "public class Example {\n"
                + "    int a = 3;\n"
                + "    public void bla() {\n"
                + "        a = 7; // 'a' must be resolved as int not String\n"
                + "        String a = \"\";\n"
                + "        a = \"test\";\n"
                + "    }\n"
                + "}";
        ParserConfiguration configuration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver())));
        StaticJavaParser.setConfiguration(configuration);
        CompilationUnit cu = StaticJavaParser.parse(src);
        AssignExpr expr = cu.findAll(AssignExpr.class).get(1);
        ResolvedType rt2 = expr.calculateResolvedType();
        assertEquals("java.lang.String", rt2.describe());
    }
}

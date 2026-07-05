/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.FileNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class Issue366Test extends AbstractResolutionTest {

    @Test()
    void test() throws FileNotFoundException {

        String code = "class A { int j = ~1;}";

        StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<UnaryExpr> exprs = cu.findAll(UnaryExpr.class);
        exprs.forEach(expr -> {
            ResolvedType rt = expr.calculateResolvedType();
            assertEquals("int", rt.describe());
        });
    }
}

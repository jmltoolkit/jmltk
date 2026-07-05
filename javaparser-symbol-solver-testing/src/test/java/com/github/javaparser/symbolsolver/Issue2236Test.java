/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

public class Issue2236Test extends AbstractSymbolResolutionTest {

    @Test
    public void test() {
        final Path testRoot = adaptPath("src/test/resources/issue2236");
        TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(testRoot);
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver(reflectionTypeSolver, javaParserTypeSolver);
        ParserConfiguration configuration =
                new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
        StaticJavaParser.setConfiguration(configuration);

        String src = "class X {public void f(){ " + "new A<Object>(new Boolean(true)); }}";

        CompilationUnit cu = StaticJavaParser.parse(src);

        List<ObjectCreationExpr> oces = cu.findAll(ObjectCreationExpr.class);
        assertEquals("A.A(java.lang.Boolean)", oces.get(0).resolve().getQualifiedSignature());
    }
}

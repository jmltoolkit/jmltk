/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue3277Test extends AbstractResolutionTest {

    @Test
    void test() throws IOException {
        String code = "public class StackOverflowTestCase {\n"
                + "	private C c = new C();\n"
                + "\n"
                + "	public void method1() {\n"
                + "		String localVariable = ConstantA.b.new innerClassInB(c.d.str1, c.d.str2).toString();\n"
                + "	}\n"
                + "}";
        Path pathToSourceFile = adaptPath("src/test/resources/issue3277");
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(
                new CombinedTypeSolver(new ReflectionTypeSolver(), new JavaParserTypeSolver(pathToSourceFile)));
        parserConfiguration.setSymbolResolver(symbolSolver);
        JavaParser javaParser = new JavaParser(parserConfiguration);
        CompilationUnit cu = javaParser.parse(code).getResult().get();
        MethodCallExpr methodCallExpr = cu.findFirst(MethodCallExpr.class).orElse(null);
        assertEquals("java.lang.Object.toString()", methodCallExpr.resolve().getQualifiedSignature());
    }
}

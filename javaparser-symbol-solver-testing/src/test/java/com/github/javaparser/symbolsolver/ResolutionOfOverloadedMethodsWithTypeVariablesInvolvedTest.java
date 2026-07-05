/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResolutionOfOverloadedMethodsWithTypeVariablesInvolvedTest {

    @Test
    void test() {
        String code = "public class Box<E> {\n"
                + "    private E element;\n"
                + "\n"
                + "    public Box(E element) {\n"
                + "        this.element = element;\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public String toString() {\n"
                + "        StringBuilder builder = new StringBuilder();\n"
                + "        builder.append(element == this ? \"(this box)\" : element);\n"
                + "        return builder.toString();\n"
                + "    }\n"
                + "}";
        StaticJavaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
        CompilationUnit cu = StaticJavaParser.parse(code);

        final List<MethodCallExpr> methodCallExprs = cu.findAll(MethodCallExpr.class);
        MethodCallExpr methodCallExpr = methodCallExprs.get(0);
        assertEquals("builder.append(element == this ? \"(this box)\" : element)", methodCallExpr.toString());
        assertEquals("append", methodCallExpr.resolve().getName());
        assertEquals(
                "java.lang.Object",
                methodCallExpr.resolve().getParam(0).getType().describe());
    }
}

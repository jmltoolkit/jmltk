/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue4668Test extends AbstractResolutionTest {

    @Test
    void test() {
        String code = "package com.example.test;\n" + "public class ThisExprTester {\n"
                + "    Test test;\n"
                + "    void getClasses() {\n"
                + "        class innerClass {\n"
                + "            public void test() {\n"
                + "                ThisExprTester.this.test.test();\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}\n"
                + "class Test { void test() {} }";
        StaticJavaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
        CompilationUnit cu = StaticJavaParser.parse(code);

        FieldAccessExpr fae = cu.findFirst(FieldAccessExpr.class).orElseThrow(null);
        assertEquals("com.example.test.Test", fae.calculateResolvedType().describe());
        assertEquals(
                "com.example.test.ThisExprTester",
                fae.getScope().calculateResolvedType().describe());
    }
}

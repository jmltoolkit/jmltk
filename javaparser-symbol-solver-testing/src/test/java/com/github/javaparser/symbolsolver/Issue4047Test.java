/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import java.util.List;
import org.junit.jupiter.api.Test;

public class Issue4047Test extends AbstractResolutionTest {

    @Test
    void test() {

        String code = "import static java.lang.String.valueOf;\n"
                + "public class MyClass { \n"
                + "  void f() { \n"
                + "    Long Integer = null; \n"
                + "    Integer.intValue(); \n"
                + "    valueOf(Integer); \n"
                + "  } \n"
                + "} \n";

        JavaParserAdapter parser = JavaParserAdapter.of(createParserWithResolver(defaultTypeSolver()));
        CompilationUnit cu = parser.parse(code);

        List<MethodCallExpr> exprs = cu.findAll(MethodCallExpr.class);

        assertEquals("java.lang.Long.intValue()", exprs.get(0).resolve().getQualifiedSignature());
        assertEquals(
                "java.lang.String.valueOf(java.lang.Object)",
                exprs.get(1).resolve().getQualifiedSignature());
    }
}

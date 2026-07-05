/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class Issue4037Test extends AbstractResolutionTest {

    @Test
    void test() {

        String code = "public class Test1 {\n"
                + "	    public static Integer getD2() { return null; }\n"
                + "	    public static Integer getD1() { return null; }\n"
                + "	    public void m(java.util.function.Supplier... rs) { }\n"
                + "\n"
                + "	    public void test() {\n"
                + "	        new Test1().m(Test1::getD1, Test1::getD2);    // exception throws\n"
                + "	    }\n"
                + "	}";

        JavaParserAdapter parser = JavaParserAdapter.of(createParserWithResolver(defaultTypeSolver()));
        CompilationUnit cu = parser.parse(code);

        List<MethodReferenceExpr> exprs = cu.findAll(MethodReferenceExpr.class);

        exprs.forEach(expr -> {
            assertDoesNotThrow(() -> expr.resolve());
        });
    }
}

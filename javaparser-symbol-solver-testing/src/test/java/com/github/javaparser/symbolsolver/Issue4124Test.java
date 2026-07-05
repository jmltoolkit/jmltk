/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue4124Test extends AbstractResolutionTest {

    @Test
    void issue4124_withDifferentTypeParameterName() {

        String code = "import java.util.Collections;\n"
                + "import java.util.List;\n"
                + "public class Foo<E> {\n"
                + "    public void test(List<E> ls){\n"
                + "        Collections.synchronizedList(ls);\n"
                + "    }\n"
                + "}\n";

        CompilationUnit cu = JavaParserAdapter.of(createParserWithResolver(defaultTypeSolver()))
                .parse(code);

        MethodCallExpr m = cu.findFirst(MethodCallExpr.class).get();
        ResolvedMethodDeclaration rmd = m.resolve();
        assertEquals("java.util.Collections.synchronizedList(java.util.List<T>)", rmd.getQualifiedSignature());
    }

    @Test
    void issue4124_withSameTypeParameterName() {
        String code = "import java.util.Collections;\n"
                + "import java.util.List;\n"
                + "public class Foo<T> {\n"
                + "    public void test(List<T> ls){\n"
                + "        Collections.synchronizedList(ls);\n"
                + "    }\n"
                + "}\n";

        CompilationUnit cu = JavaParserAdapter.of(createParserWithResolver(defaultTypeSolver()))
                .parse(code);

        MethodCallExpr m = cu.findFirst(MethodCallExpr.class).get();
        ResolvedMethodDeclaration rmd = m.resolve();
        assertEquals("java.util.Collections.synchronizedList(java.util.List<T>)", rmd.getQualifiedSignature());
    }
}

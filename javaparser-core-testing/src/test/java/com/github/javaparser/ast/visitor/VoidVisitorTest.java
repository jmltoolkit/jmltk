/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.visitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VoidVisitorTest {
    @Test()
    void compareFindAllSizeWithVoidVisitorAdapterSize() throws IOException {
        CompilationUnit unit = createUnit();

        List<ObjectCreationExpr> oce = unit.findAll(ObjectCreationExpr.class);

        AtomicInteger foundObjs = new AtomicInteger(0);
        unit.accept(
                new VoidVisitorAdapter<Object>() {
                    @Override
                    public void visit(ObjectCreationExpr exp, Object arg) {
                        super.visit(exp, arg);
                        ((AtomicInteger) arg).incrementAndGet();
                    }
                },
                foundObjs);

        Assertions.assertEquals(oce.size(), foundObjs.get());
    }

    private CompilationUnit createUnit() {
        JavaParser javaParser = new JavaParser();

        CompilationUnit unit = javaParser
                .parse("public class Test\n" + "{\n"
                        + "   public class InnerTest\n"
                        + "   {\n"
                        + "       public InnerTest() {}\n"
                        + "   }\n"
                        + "    \n"
                        + "   public Test() {\n"
                        + "   }\n"
                        + "\n"
                        + "   public static void main( String[] args ) { \n"
                        + "       new Test().new InnerTest();\n"
                        + "   }\n"
                        + "}")
                .getResult()
                .get();
        return unit;
    }

    @Test()
    void testFindAllSize() throws IOException {
        CompilationUnit unit = createUnit();

        List<ObjectCreationExpr> oce = unit.findAll(ObjectCreationExpr.class);

        Assertions.assertEquals(2, oce.size());
    }
}

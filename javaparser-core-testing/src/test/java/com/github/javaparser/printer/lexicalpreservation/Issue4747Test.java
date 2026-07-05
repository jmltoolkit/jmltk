/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

public class Issue4747Test extends AbstractLexicalPreservingTest {

    @Test
    void test() {
        final String code = "public class TestClass {\n"
                + "    @com.abc.def.TestMarkerAnnotation\n"
                + "    public void method1() {}\n"
                + "}";
        considerCode(code);
        cu.accept(
                new ModifierVisitor<Void>() {
                    public Visitable visit(final MarkerAnnotationExpr expr, final Void arg) {
                        if (expr.getNameAsString().equals("com.abc.def.TestMarkerAnnotation")) {
                            expr.setName("TestMarkerAnnotation");
                        }

                        return super.visit(expr, arg);
                    }
                },
                null);

        String actual = LexicalPreservingPrinter.print(cu);
        String expected =
                "public class TestClass {\n" + "    @TestMarkerAnnotation\n" + "    public void method1() {}\n" + "}";

        assertEqualsStringIgnoringEol(expected, actual);
    }
}

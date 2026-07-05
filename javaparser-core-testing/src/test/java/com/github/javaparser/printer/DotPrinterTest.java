/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer;

import static com.github.javaparser.StaticJavaParser.parse;
import static com.github.javaparser.StaticJavaParser.parseExpression;
import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import org.junit.jupiter.api.Test;

class DotPrinterTest {
    @Test
    void testWithType() {
        String expectedOutput = "digraph {" + System.lineSeparator();
        expectedOutput += "n0 [label=\"root (MethodCallExpr)\"];" + System.lineSeparator();
        expectedOutput += "n1 [label=\"name (SimpleName)\"];" + System.lineSeparator();
        expectedOutput += "n0 -> n1;" + System.lineSeparator();
        expectedOutput += "n2 [label=\"identifier='x'\"];" + System.lineSeparator();
        expectedOutput += "n1 -> n2;" + System.lineSeparator();
        expectedOutput += "n3 [label=\"arguments\"];" + System.lineSeparator();
        expectedOutput += "n0 -> n3;" + System.lineSeparator();
        expectedOutput += "n4 [label=\"argument (IntegerLiteralExpr)\"];" + System.lineSeparator();
        expectedOutput += "n3 -> n4;" + System.lineSeparator();
        expectedOutput += "n5 [label=\"value='1'\"];" + System.lineSeparator();
        expectedOutput += "n4 -> n5;" + System.lineSeparator();
        expectedOutput += "n6 [label=\"argument (IntegerLiteralExpr)\"];" + System.lineSeparator();
        expectedOutput += "n3 -> n6;" + System.lineSeparator();
        expectedOutput += "n7 [label=\"value='1'\"];" + System.lineSeparator();
        expectedOutput += "n6 -> n7;" + System.lineSeparator();
        expectedOutput += "}";

        DotPrinter dotPrinter = new DotPrinter(true);
        Expression expression = parseExpression("x(1,1)");
        String output = dotPrinter.output(expression);
        assertEquals(expectedOutput, output);
    }

    @Test
    void testWithoutType() {
        String expectedOutput = "digraph {" + System.lineSeparator();
        expectedOutput += "n0 [label=\"root\"];" + System.lineSeparator();
        expectedOutput += "n1 [label=\"operator='PLUS'\"];" + System.lineSeparator();
        expectedOutput += "n0 -> n1;" + System.lineSeparator();
        expectedOutput += "n2 [label=\"left\"];" + System.lineSeparator();
        expectedOutput += "n0 -> n2;" + System.lineSeparator();
        expectedOutput += "n3 [label=\"value='1'\"];" + System.lineSeparator();
        expectedOutput += "n2 -> n3;" + System.lineSeparator();
        expectedOutput += "n4 [label=\"right\"];" + System.lineSeparator();
        expectedOutput += "n0 -> n4;" + System.lineSeparator();
        expectedOutput += "n5 [label=\"value='1'\"];" + System.lineSeparator();
        expectedOutput += "n4 -> n5;" + System.lineSeparator();
        expectedOutput += "}";

        DotPrinter dotPrinter = new DotPrinter(false);
        Expression expression = parseExpression("1+1");
        String output = dotPrinter.output(expression);
        assertEquals(expectedOutput, output);
    }

    @Test
    void testIssue1871() {
        DotPrinter printer = new DotPrinter(false);
        CompilationUnit cu = parse("//q\"q\nclass X{}");
        String output = printer.output(cu);
        assertEqualsStringIgnoringEol(
                "digraph {\n" + "n0 [label=\"root\"];\n"
                        + "n1 [label=\"types\"];\n"
                        + "n0 -> n1;\n"
                        + "n2 [label=\"type\"];\n"
                        + "n1 -> n2;\n"
                        + "n3 [label=\"isCompact='false'\"];\n"
                        + "n2 -> n3;\n"
                        + "n4 [label=\"isInterface='false'\"];\n"
                        + "n2 -> n4;\n"
                        + "n5 [label=\"name\"];\n"
                        + "n2 -> n5;\n"
                        + "n6 [label=\"identifier='X'\"];\n"
                        + "n5 -> n6;\n"
                        + "n7 [label=\"comment\"];\n"
                        + "n2 -> n7;\n"
                        + "n8 [label=\"content='q\\\"q'\"];\n"
                        + "n7 -> n8;\n"
                        + "}",
                output);
    }
}

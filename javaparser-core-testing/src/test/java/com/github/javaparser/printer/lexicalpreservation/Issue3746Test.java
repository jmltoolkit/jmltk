/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.junit.jupiter.api.Test;

public class Issue3746Test extends AbstractLexicalPreservingTest {

    @Test
    void test() {
        considerCode("public class MyClass {\n" + " String s0;\n" + " // Comment\n" + " String s1;\n" + "}");

        considerCode(
                "class A {\n" + "  void foo() {\n" + "    int first = 1;\n" + "    int second = 2;\n" + "  }\n" + "}");

        String expected = "class A {\n" + "  void foo() {\n" + "    foo();\n" + "    int second = 2;\n" + "  }\n" + "}";
        BlockStmt block = cu.findAll(BlockStmt.class).get(0);
        ExpressionStmt newStmt = new ExpressionStmt(new MethodCallExpr("foo"));
        block.addStatement(1, newStmt);
        block.getStatement(0).remove();
        assertEquals(expected, LexicalPreservingPrinter.print(cu));
    }
}

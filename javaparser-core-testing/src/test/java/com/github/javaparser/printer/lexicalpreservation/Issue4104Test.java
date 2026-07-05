/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import org.junit.jupiter.api.Test;

public class Issue4104Test extends AbstractLexicalPreservingTest {

    @Test
    void test() {
        considerCode("class Foo {\n"
                + "  void foo() {\n"
                + "    switch(bar) {\n"
                + "      default:\n"
                + "        break;\n"
                + "    }\n"
                + "  }\n"
                + "}");
        // should be this
        //		String expected =
        //				"class Foo {\n"
        //				+ "  void foo() {\n"
        //				+ "    switch(bar) {\n"
        //				+ "      default:\n"
        //				+ "        break;\n"
        //				+ "      case 0:\n"
        //				+ "          break;\n"
        //				+ "    }\n"
        //				+ "  }\n"
        //				+ "}";

        String expected = "class Foo {\n"
                + "  void foo() {\n"
                + "    switch(bar) {\n"
                + "      default:\n"
                + "        break;\n"
                + "      case 0:\n"
                + "          break;\n"
                + "      }\n"
                + "  }\n"
                + "}";

        SwitchStmt switchStmt =
                cu.findAll(SwitchStmt.class).stream().findFirst().get();

        SwitchEntry newEntry = new SwitchEntry();
        newEntry.setLabels(NodeList.nodeList(new IntegerLiteralExpr(0)));
        newEntry.setStatements(NodeList.nodeList(new BreakStmt()));
        switchStmt.getEntries().addNLast(newEntry);

        assertEqualsStringIgnoringEol(expected, LexicalPreservingPrinter.print(cu));
    }
}

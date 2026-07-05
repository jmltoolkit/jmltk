/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Test;

public class Issue3441Test extends AbstractLexicalPreservingTest {

    @Test
    void test() {
        considerCode("public class Foo {\n" + "    void bar() {\n"
                + "        stmt1(); // comment 1\n"
                + "        stmt2(); // comment 2\n"
                + "    }\n"
                + "}");
        String expected =
                "public class Foo {\n" + "    void bar() {\n" + "        stmt2(); // comment 2\n" + "    }\n" + "}";

        BlockStmt block = cu.findFirst(BlockStmt.class).get();
        Statement stmt = block.getStatements().get(0);

        block.remove(stmt);

        assertEqualsStringIgnoringEol(expected, LexicalPreservingPrinter.print(cu));
    }
}

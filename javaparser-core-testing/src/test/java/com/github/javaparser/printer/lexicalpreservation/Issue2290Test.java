/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.ast.stmt.ExpressionStmt;
import java.util.List;
import org.junit.jupiter.api.Test;

public class Issue2290Test extends AbstractLexicalPreservingTest {

    @Test
    public void test() {

        considerCode("public class Clone1 {\n" + "  public static void main(String[] args) {\n"
                + "    System.out.println(\"I'm a clone10\");\n"
                + "    System.out.println(\"I'm not a clone!\");\n"
                + "    System.out.println(\"I'm a clone10\");\n"
                + "  }\n"
                + "}");
        List<ExpressionStmt> exprs = cu.findAll(ExpressionStmt.class);
        ExpressionStmt es = exprs.get(exprs.size() - 1);
        es.getParentNode().get().remove(es);
        exprs = cu.findAll(ExpressionStmt.class);
        // verify that one statement is removed
        assertTrue(exprs.size() == 2);
        // verify that the first statement is not removed
        assertEquals("System.out.println(\"I'm a clone10\");", exprs.get(0).toString());
    }
}

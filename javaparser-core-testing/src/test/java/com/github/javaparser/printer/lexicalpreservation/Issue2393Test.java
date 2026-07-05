/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.stmt.IfStmt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue2393Test extends AbstractLexicalPreservingTest {

    @Test
    public void test() {
        considerCode("public class Test { public void foo() { int i = 0;\nif(i == 5) { System.out.println(i); } } }");
        IfStmt ifStmt = cu.findFirst(IfStmt.class).orElseThrow(() -> new IllegalStateException("Expected if"));
        ifStmt.setCondition(StaticJavaParser.parseExpression("i > 0"));
        assertEquals("i > 0", ifStmt.getCondition().toString());
    }
}

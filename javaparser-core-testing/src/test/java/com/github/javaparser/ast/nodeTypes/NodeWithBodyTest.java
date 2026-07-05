/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.utils.TestParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeWithBodyTest {
    @Test
    void emptyStatementIsEmpty() {
        ForStmt forStmt = TestParser.parseStatement("for(;;);").asForStmt();

        assertTrue(forStmt.hasEmptyBody());
    }

    @Test
    void emptyBlockIsEmpty() {
        ForStmt forStmt = TestParser.parseStatement("for(;;){}").asForStmt();

        assertTrue(forStmt.hasEmptyBody());
    }

    @Test
    void simpleStatementIsNotEmpty() {
        ForStmt forStmt = TestParser.parseStatement("for(;;)a=b;").asForStmt();

        assertFalse(forStmt.hasEmptyBody());
    }

    @Test
    void nonEmptyBlockIsNotEmpty() {
        ForStmt forStmt = TestParser.parseStatement("for(;;){a=b;}").asForStmt();

        assertFalse(forStmt.hasEmptyBody());
    }
}

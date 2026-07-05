/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.stmt;

import static com.github.javaparser.StaticJavaParser.parseStatement;
import static org.junit.jupiter.api.Assertions.*;

import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import org.junit.jupiter.api.Test;

class ForEachStmtTest {
    @Test
    void nonFinalPrimitive() {
        ForEachStmt statement = parseStatement("for (int i : ints) {}").asForEachStmt();
        assertFalse(statement.hasFinalVariable());
        assertEquals(PrimitiveType.intType(), statement.getVariableDeclarator().getType());
        assertEquals("i", statement.getVariableDeclarator().getName().getIdentifier());
    }

    @Test
    void finalNonPrimitive() {
        ForEachStmt statement = parseStatement("for (final Object o : objs) {}").asForEachStmt();
        assertTrue(statement.hasFinalVariable());
        assertEquals(
                new ClassOrInterfaceType(null, "Object"),
                statement.getVariableDeclarator().getType());
        assertEquals("o", statement.getVariableDeclarator().getName().getIdentifier());
    }
}

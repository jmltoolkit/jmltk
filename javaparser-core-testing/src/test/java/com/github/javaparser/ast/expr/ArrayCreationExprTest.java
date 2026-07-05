/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.PrimitiveType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArrayCreationExprTest {

    @Test
    void correctlyCreatesExpressionWithDefaultConstructor() {
        ArrayCreationExpr expr = new ArrayCreationExpr();

        assertEquals("new empty[] {}", expr.toString());
    }

    @Test
    void correctlyCreatesExpressionWithSimpleConstructor() {
        ArrayCreationExpr expr = new ArrayCreationExpr(PrimitiveType.byteType());

        assertEquals("new byte[] {}", expr.toString());
    }

    @Test
    void correctlyCreatesExpressionWithFullConstructor() {
        ArrayInitializerExpr initializer = new ArrayInitializerExpr(
                new NodeList<>(new IntegerLiteralExpr("1"), new IntegerLiteralExpr("2"), new IntegerLiteralExpr("3")));
        ArrayCreationExpr expr =
                new ArrayCreationExpr(PrimitiveType.intType(), new NodeList<>(new ArrayCreationLevel()), initializer);

        assertEquals("new int[] { 1, 2, 3 }", expr.toString());
    }
}

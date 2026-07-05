/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.transformations.ast.body;

import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.printer.lexicalpreservation.AbstractLexicalPreservingTest;
import org.junit.jupiter.api.Test;

/**
 * Transforming BinaryExpr and verifying the LexicalPreservation works as expected.
 */
class OperatorTransformationsTest extends AbstractLexicalPreservingTest {

    @Test
    void binaryExpressionOperator() {
        considerExpression("a && b");
        expression.asBinaryExpr().setRight(new NameExpr("c"));
        assertTransformedToString("a && c", expression);
    }

    @Test
    void unaryExpressionOperator() {
        considerExpression("!a");
        expression.asUnaryExpr().setExpression(new NameExpr("b"));
        assertTransformedToString("!b", expression);
    }

    @Test
    void assignExpressionOperator() {
        considerExpression("a <<= 1");
        expression.asAssignExpr().setValue(new IntegerLiteralExpr(2));
        assertTransformedToString("a <<= 2", expression);
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import com.github.javaparser.ParseProblemException;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SuperExprTest {
    @Test
    void justSuper() {
        assertThrows(ParseProblemException.class, () -> parseExpression("super"));
    }

    @Test
    void singleScopeSuper() {
        Expression expr = parseExpression("A.super");

        Name className = expr.asSuperExpr().getTypeName().get();

        assertEquals("A", className.asString());
    }

    @Test
    void multiScopeSuper() {
        Expression expr = parseExpression("a.B.super");

        Name className = expr.asSuperExpr().getTypeName().get();

        assertEquals("a.B", className.asString());
    }
}

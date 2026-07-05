/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static com.github.javaparser.utils.TestUtils.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.junit.jupiter.api.Test;

class NodeWithTraversableScopeTest {
    @Test
    void traverse1() {
        NodeWithTraversableScope expression = parseExpression("getAddress().name.startsWith(\"abc\")");

        assertInstanceOf(MethodCallExpr.class, expression);
        expression = (NodeWithTraversableScope) expression.traverseScope().get();
        assertInstanceOf(FieldAccessExpr.class, expression);
        expression = (NodeWithTraversableScope) expression.traverseScope().get();
        assertInstanceOf(MethodCallExpr.class, expression);
        assertFalse(expression.traverseScope().isPresent());
    }
}

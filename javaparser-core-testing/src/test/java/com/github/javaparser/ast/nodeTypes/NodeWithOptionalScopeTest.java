/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeWithOptionalScopeTest {

    @Test
    void commonExpressionWhichHaveInterfaceNodeWithOptionalScope() {
        MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr("A"), "call");
        ObjectCreationExpr objectCreationExpr = new ObjectCreationExpr();

        assertTrue(methodCallExpr.hasScope());
        assertFalse(objectCreationExpr.hasScope());
    }

    @Test
    void removeScope() {
        MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr("A"), "method");

        methodCallExpr.removeScope();

        assertFalse(methodCallExpr.hasScope());
    }
}

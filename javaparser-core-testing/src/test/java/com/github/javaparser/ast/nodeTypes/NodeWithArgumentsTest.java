/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import static com.github.javaparser.ast.expr.Expression.EXCLUDE_ENCLOSED_EXPR;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.printer.lexicalpreservation.AbstractLexicalPreservingTest;
import org.junit.jupiter.api.Test;

class NodeWithArgumentsTest extends AbstractLexicalPreservingTest {

    @Test
    void testGetArgumentPosition() {
        considerCode("" + "class Foo {\n"
                + "    Map<Integer,String> map = new HashMap<>();\n"
                + "    public String bar(int i) {\n"
                + "        return map.put(((i)),((\"baz\")));\n"
                + "    } \n"
                + "}");
        MethodCallExpr mce = cu.findFirst(MethodCallExpr.class).get();
        Expression arg0 = mce.getArgument(0);
        Expression arg1 = mce.getArgument(1);
        Expression innerExpr0 =
                arg0.asEnclosedExpr().getInner().asEnclosedExpr().getInner();
        Expression innerExpr1 =
                arg1.asEnclosedExpr().getInner().asEnclosedExpr().getInner();

        assertEquals(0, mce.getArgumentPosition(arg0)); // with no conversion
        assertEquals(
                0,
                mce.getArgumentPosition(innerExpr0, EXCLUDE_ENCLOSED_EXPR)); // with a conversion skipping EnclosedExprs
        assertEquals(1, mce.getArgumentPosition(arg1)); // with no conversion
        assertEquals(
                1,
                mce.getArgumentPosition(innerExpr1, EXCLUDE_ENCLOSED_EXPR)); // with a conversion skipping EnclosedExprs
    }
}

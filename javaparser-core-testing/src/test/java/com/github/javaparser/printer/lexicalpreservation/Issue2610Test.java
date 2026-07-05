/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class Issue2610Test extends AbstractLexicalPreservingTest {

    /*
     * This test case must prevent an UnsupportedOperation Removed throwed by LexicalPreservation when we try to replace an expression
     */
    @Test
    public void test() {

        considerCode("public class Bar {\n" + "    public void foo() {\n"
                + "          // comment\n"
                + "          System.out.print(\"error\");\n"
                + "    }\n"
                + "}");
        // contruct a statement with a comment
        Expression expr = StaticJavaParser.parseExpression("System.out.println(\"warning\")");
        // Replace the method expression
        Optional<MethodCallExpr> mce = cu.findFirst(MethodCallExpr.class);
        mce.get().getParentNode().get().replace(mce.get(), expr);
        // TODO assert something
    }
}

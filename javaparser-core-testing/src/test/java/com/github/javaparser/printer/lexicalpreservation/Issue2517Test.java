/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import org.junit.jupiter.api.Test;

public class Issue2517Test extends AbstractLexicalPreservingTest {

    @Test
    public void test() {

        considerCode("public class A {\n" + "    public A(String a , String b) {\n"
                + "    }\n"
                + "    public static A m() {\n"
                + "      return new A(\"a\",\"b\");\n"
                + "    }\n"
                + "}");

        String expected = "public class A {\n" + "    public A(String a , String b) {\n"
                + "    }\n"
                + "    public static A m() {\n"
                + "      return new A(\"b\", \"a\");\n"
                + "    }\n"
                + "}";

        ObjectCreationExpr cd = cu.findFirst(ObjectCreationExpr.class).get();
        NodeList<Expression> args = cd.getArguments();
        Expression a1 = args.get(0);
        Expression a2 = args.get(1);
        NodeList<Expression> newArgs = new NodeList<>(a2, a1);
        cd.setArguments(newArgs);

        assertEqualsStringIgnoringEol(expected, LexicalPreservingPrinter.print(cu));
    }
}

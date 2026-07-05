/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

public class Issue3936Test extends AbstractLexicalPreservingTest {
    static final String given = "package some.project;\n"
            + "\n"
            + "import java.util.Optional;\n"
            + "\n"
            + "public class SomeClass {\n"
            + "\n"
            + "	String html = \"\" + \"<html>\\n\"\n"
            + "			+ \"\\t<head>\\n\"\n"
            + "			+ \"\\t\\t<meta charset=\\\"utf-8\\\">\\n\"\n"
            + "			+ \"\\t</head>\\n\"\n"
            + "			+ \"\\t<body class=\\\"default-view\\\" style=\\\"word-wrap: break-word;\\\">\\n\"\n"
            + "			+ \"\\t\\t<p>Hello, world</p>\\n\"\n"
            + "			+ \"\\t</body>\\n\"\n"
            + "			+ \"</html>\\n\";\n"
            + "}";

    @Test
    void test() {
        considerCode(given);

        String newText = "\tfirstRow\n\tsecondRow\n\tthirdRow";

        LexicalPreservingPrinter.setup(cu);

        VariableDeclarator expr = cu.findFirst(VariableDeclarator.class).get();
        expr.setInitializer(new TextBlockLiteralExpr(newText));

        String actual = LexicalPreservingPrinter.print(cu);
        String expected = "package some.project;\n"
                + "\n"
                + "import java.util.Optional;\n"
                + "\n"
                + "public class SomeClass {\n"
                + "\n"
                + "	String html = \"\"\"\n"
                + "\tfirstRow\n"
                + "\tsecondRow\n"
                + "\tthirdRow\"\"\";\n"
                + "}";
        assertEqualsStringIgnoringEol(expected, actual);
    }
}

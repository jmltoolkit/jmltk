/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

public class Issue2374Test extends AbstractLexicalPreservingTest {

    @Test
    @Disabled(
            "Test disabled because this test fails on Ubuntu systems regardless of the Java version, but works on Windows and macOS systems")
    public void test() {
        String lineComment = "Example comment";
        considerCode("public class Bar {\n" + "    public void foo() {\n"
                + "        System.out.print(\"Hello\");\n"
                + "    }\n"
                + "}");
        String expected = "public class Bar {\n"
                + "    public void foo() {\n"
                + "        System.out.print(\"Hello\");\n"
                + "        //Example comment\n"
                + "        System.out.println(\"World!\");\n"
                + "    }\n"
                + "}";
        // contruct a statement with a comment
        Statement stmt = StaticJavaParser.parseStatement("System.out.println(\"World!\");");
        stmt.setLineComment(lineComment);
        // add the statement to the ast
        Optional<MethodDeclaration> md = cu.findFirst(MethodDeclaration.class);
        md.get().getBody().get().addStatement(stmt);
        // print the result from LexicalPreservingPrinter
        String result = LexicalPreservingPrinter.print(cu);
        // verify that the LexicalPreservingPrinter don't forget the comment
        assertEqualsStringIgnoringEol(expected, result);
    }
}

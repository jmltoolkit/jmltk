/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.junit.jupiter.api.Test;

public class Issue3937Test extends AbstractLexicalPreservingTest {
    static final String given = "package custom.project;\n" + "\n"
            + "import java.util.stream.Stream;\n"
            + "\n"
            + "class TestFileSystemCodeProvider {\n"
            + "	void testInMemoryFileSystem() {\n"
            + "\n"
            + "		Stream.of(\"\").listFilesForContent(file -> {\n"
            + "			System.out.println(s);\n"
            + "		});\n"
            + "	}\n"
            + "}\n"
            + "";

    @Test
    void test() {
        considerCode(given);

        LexicalPreservingPrinter.setup(cu);

        LambdaExpr lambdaExpr = cu.findFirst(LambdaExpr.class).get();
        lambdaExpr.setBody(new ExpressionStmt(new MethodCallExpr(new NameExpr("SomeClass"), "someMethod")));

        String actual = LexicalPreservingPrinter.print(cu);
        String expected = "package custom.project;\n"
                + "\n"
                + "import java.util.stream.Stream;\n"
                + "\n"
                + "class TestFileSystemCodeProvider {\n"
                + "	void testInMemoryFileSystem() {\n"
                + "\n"
                + "		Stream.of(\"\").listFilesForContent(file -> SomeClass.someMethod());\n"
                + "	}\n"
                + "}\n"
                + "";
        assertEqualsStringIgnoringEol(expected, actual);
    }
}

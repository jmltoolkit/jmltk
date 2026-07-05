/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;

import static com.github.javaparser.utils.TestUtils.assertEqualsString;
import static com.github.javaparser.utils.TestUtils.readResource;

public abstract class AbstractLexicalPreservingTest {

    protected CompilationUnit cu;
    protected Expression expression;
    protected Statement statement;

    @AfterAll
    public static void tearDown() {}

    @AfterEach
    public void reset() {
        StaticJavaParser.setConfiguration(new ParserConfiguration());
    }

    protected void considerCode(String code) {
        cu = LexicalPreservingPrinter.setup(StaticJavaParser.parse(code));
    }

    protected void considerExpression(String code) {
        expression = LexicalPreservingPrinter.setup(StaticJavaParser.parseExpression(code));
    }

    protected void considerStatement(String code) {
        statement = LexicalPreservingPrinter.setup(StaticJavaParser.parseStatement(code));
    }

    protected void considerVariableDeclaration(String code) {
        expression = LexicalPreservingPrinter.setup(StaticJavaParser.parseVariableDeclarationExpr(code));
    }

    protected String considerExample(String resourceName) throws IOException {
        String code = readExample(resourceName);
        considerCode(code);
        return code;
    }

    protected String readExample(String resourceName) throws IOException {
        return readResource("com/github/javaparser/lexical_preservation_samples/" + resourceName + ".java.txt");
    }

    protected void assertTransformed(String exampleName, Node node) throws IOException {
        String expectedCode = readExample(exampleName + "_expected");
        String actualCode = LexicalPreservingPrinter.print(node);

        // Note that we explicitly care about line endings when handling lexical preservation.
        assertEqualsString(expectedCode, actualCode);
    }

    protected void assertUnchanged(String exampleName) throws IOException {
        String expectedCode = considerExample(exampleName + "_original");
        String actualCode = LexicalPreservingPrinter.print(cu != null ? cu : expression);

        // Note that we explicitly care about line endings when handling lexical preservation.
        assertEqualsString(expectedCode, actualCode);
    }

    protected void assertTransformedToString(String expectedPartialCode, Node node) {
        String actualCode = LexicalPreservingPrinter.print(node);

        // Note that we explicitly care about line endings when handling lexical preservation.
        assertEqualsString(expectedPartialCode, actualCode);
    }
}

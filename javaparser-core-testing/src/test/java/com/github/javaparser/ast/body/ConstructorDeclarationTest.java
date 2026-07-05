/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.body;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.ParseStart.COMPILATION_UNIT;
import static com.github.javaparser.Providers.provider;
import static com.github.javaparser.utils.TestUtils.assertNoProblems;
import static org.junit.jupiter.api.Assertions.*;

class ConstructorDeclarationTest {
    @Test
    void acceptsSuper() {
        ConstructorDeclaration cons = new ConstructorDeclaration("Cons");
        cons.createBody().addStatement("super();");

        assertEquals(
                String.format("public Cons() {%1$s" + "    super();%1$s" + "}", LineSeparator.SYSTEM), cons.toString());
    }

    @Test
    void explicitConstructorInvocationAfterFirstStatement() {
        String code = "class Foo {\n" + "    public Foo() {\n"
                + "        int x = 2;\n"
                + "        super();\n"
                + "        x = 3;\n"
                + "    }\n"
                + "}";

        ParserConfiguration configuration =
                new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25);
        JavaParser parser = new JavaParser(configuration);
        ParseResult<CompilationUnit> result = parser.parse(COMPILATION_UNIT, provider(code));
        assertNoProblems(result);

        CompilationUnit cu = result.getResult().get();

        ConstructorDeclaration constructorDeclaration =
                Navigator.demandNodeOfGivenClass(cu, ConstructorDeclaration.class);
        NodeList<Statement> statements = constructorDeclaration.getBody().get().getStatements();

        assertTrue(statements.get(0).isExpressionStmt());
        assertTrue(statements.get(0).asExpressionStmt().getExpression().isVariableDeclarationExpr());
        assertTrue(statements.get(1).isExplicitConstructorInvocationStmt());
        assertFalse(statements.get(1).asExplicitConstructorInvocationStmt().isThis());
        assertTrue(statements.get(2).isExpressionStmt());
        assertTrue(statements.get(2).asExpressionStmt().getExpression().isAssignExpr());
    }
}

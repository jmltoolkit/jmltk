/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import static com.github.javaparser.ParseStart.EXPRESSION;
import static com.github.javaparser.ParseStart.STATEMENT;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_22;
import static com.github.javaparser.Providers.provider;
import static com.github.javaparser.utils.TestUtils.assertNoProblems;
import static com.github.javaparser.utils.TestUtils.assertProblems;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Test;

/**
 * See <a href="https://openjdk.org/jeps/456">JEP456</a> for descriptions of the cases tested here.
 */
class Java22ValidatorTest {

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(JAVA_22));

    @Test
    void matchAllAllowedInRecordList() {
        ParseResult<Expression> result =
                javaParser.parse(EXPRESSION, provider("switch(x){case Box(_) -> System.out.println(0);}"));
        assertNoProblems(result);
    }

    @Test
    void matchAllNotAllowedAsTopLevelPattern() {
        ParseResult<Expression> result =
                javaParser.parse(EXPRESSION, provider("switch(x){case _ -> System.out.println(0);}"));
        assertProblems(result, "(line 1,col 16) Unnamed variables only supported in cases described by JEP456");
    }

    @Test
    void unnamedTypePatternAllowed() {
        ParseResult<Expression> result =
                javaParser.parse(EXPRESSION, provider("switch(x){case Foo _ -> System.out.println(0);}"));
        assertNoProblems(result);
    }

    @Test
    void unnamedVariableDeclaratorAllowed() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("int _ = 42;"));
        assertNoProblems(result);
    }

    @Test
    void unnamedVariableAllowedInForEach() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("for (Foo _ : items) {}"));
        assertNoProblems(result);
    }

    @Test
    void unnamedVariableAllowedInForUpdate() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("for (int i = 0; _ = foo(); i++) {}"));
        assertNoProblems(result);
    }

    @Test
    void unnamedVariableAllowedInCatchBlock() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("try {} catch (Exception _) {}"));
        assertNoProblems(result);
    }

    @Test
    void unnamedVariableAllowedInTryWithResources() {
        ParseResult<Statement> result =
                javaParser.parse(STATEMENT, provider("try(var _ = foo()) {} catch (Exception e) {}"));
        assertNoProblems(result);
    }

    @Test
    void unnamedVariableAllowedAsLambdaParameter() {
        ParseResult<Expression> result = javaParser.parse(EXPRESSION, provider("foo(_ -> System.out.println(0))"));
        assertNoProblems(result);
    }

    @Test
    void unnamedVariableNotAllowedAsArgument() {
        ParseResult<Expression> result = javaParser.parse(EXPRESSION, provider("foo(_)"));
        assertProblems(result, "(line 1,col 5) Unnamed variables only supported in cases described by JEP456");
    }

    @Test
    void unnamedVariableNotAllowedInNonDeclAssignment() {
        ParseResult<Expression> result = javaParser.parse(EXPRESSION, provider("_ = 12"));
        assertProblems(result, "(line 1,col 1) Unnamed variables only supported in cases described by JEP456");
    }

    @Test
    void unnamedVariableNotAllowedInForUpdate() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("for (;; _++) {}"));
        assertProblems(result, "(line 1,col 9) Unnamed variables only supported in cases described by JEP456");
    }

    @Test
    void unnamedVariableNotAllowedOnRhsInForCondition() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("for (; x = _; ) {}"));
        assertProblems(result, "(line 1,col 12) Unnamed variables only supported in cases described by JEP456");
    }
}

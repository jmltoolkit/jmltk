/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import static com.github.javaparser.ParseStart.EXPRESSION;
import static com.github.javaparser.ParseStart.STATEMENT;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_21;
import static com.github.javaparser.Providers.provider;
import static com.github.javaparser.utils.TestUtils.assertNoProblems;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.utils.TestUtils;
import org.junit.jupiter.api.Test;

class Java21ValidatorTest {

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(JAVA_21));

    @Test
    void switchDefaultCaseAllowed() {
        ParseResult<Expression> result =
                javaParser.parse(EXPRESSION, provider("switch(x){case null, default -> System.out.println(0);}"));
        assertNoProblems(result);
    }

    @Test
    void switchPatternWithGuardAllowed() {
        ParseResult<Expression> result = javaParser.parse(
                EXPRESSION, provider("switch(x){case String s when s.length() > 5 -> System.out.println(0);}"));
        assertNoProblems(result);
    }

    @Test
    void recordPatternsAllowed() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("if (a instanceof Box(String s)) {}"));
        TestUtils.assertNoProblems(result);
    }
}

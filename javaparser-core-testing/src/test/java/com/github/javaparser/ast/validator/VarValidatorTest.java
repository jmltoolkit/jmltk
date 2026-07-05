/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import static com.github.javaparser.ParseStart.STATEMENT;
import static com.github.javaparser.Providers.provider;
import static com.github.javaparser.utils.TestUtils.assertNoProblems;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Test;

class VarValidatorTest {
    public static final JavaParser javaParser =
            new JavaParser(new ParserConfiguration().setLanguageLevel(LanguageLevel.BLEEDING_EDGE));

    @Test
    void typePatternExpr() {
        ParseResult<Statement> result = javaParser.parse(
                STATEMENT, provider("switch (shape) {\n" + "  case Circle(var radius) -> { } \n" + "}"));

        assertNoProblems(result);
    }
}

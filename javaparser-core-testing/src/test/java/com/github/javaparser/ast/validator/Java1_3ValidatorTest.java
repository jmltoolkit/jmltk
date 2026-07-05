/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.ParseStart.STATEMENT;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_1_3;
import static com.github.javaparser.Providers.provider;
import static com.github.javaparser.utils.TestUtils.assertNoProblems;
import static com.github.javaparser.utils.TestUtils.assertProblems;

class Java1_3ValidatorTest {
    public static final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(JAVA_1_3));

    @Test
    void noAssert() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("assert a;"));
        assertProblems(
                result,
                "(line 1,col 1) 'assert' keyword is not supported. Pay attention that this feature is supported starting from 'JAVA_1_4' language level. If you need that feature the language level must be configured in the configuration before parsing the source files.");
    }

    @Test
    void assertIsValideIdentifierBeforeJAVA_1_4() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("String assert;"));
        assertNoProblems(result);
    }
}

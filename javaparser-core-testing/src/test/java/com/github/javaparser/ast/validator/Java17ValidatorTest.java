/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.TestUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.ParseStart.COMPILATION_UNIT;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_17;
import static com.github.javaparser.Providers.provider;

class Java17ValidatorTest {

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(JAVA_17));

    @Nested
    class Sealed {

        @Test
        void sealedAllowed() {
            ParseResult<CompilationUnit> result =
                    javaParser.parse(COMPILATION_UNIT, provider("sealed class X permits Y, Z {}"));
            TestUtils.assertNoProblems(result);
        }

        @Test
        void nonSealedAllowed() {
            ParseResult<CompilationUnit> result = javaParser.parse(COMPILATION_UNIT, provider("non-sealed class X {}"));
            TestUtils.assertNoProblems(result);
        }
    }
}

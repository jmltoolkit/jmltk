/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import static com.github.javaparser.ParseStart.*;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_25;
import static com.github.javaparser.Providers.provider;
import static com.github.javaparser.utils.TestUtils.assertNoProblems;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import org.junit.jupiter.api.Test;

/**
 * Test for Java 25 language level support.
 *
 * @see <a href="https://openjdk.org/projects/jdk/25/">https://openjdk.org/projects/jdk/25/</a>
 */
class Java25ValidatorTest {

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(JAVA_25));

    @Test
    void moduleImportAllowed() {
        ParseResult<ImportDeclaration> result =
                javaParser.parse(IMPORT_DECLARATION, provider("import module java.base;"));
        assertNoProblems(result);
    }

    @Test
    void explicitConstructorInvocationAfterFirstStatementAllowed() {
        String code = "class Foo {\n" + "    public Foo() {\n"
                + "        int x = 2;\n"
                + "        super();\n"
                + "    }\n"
                + "}";

        ParseResult<CompilationUnit> result = javaParser.parse(COMPILATION_UNIT, provider(code));
        assertNoProblems(result);
    }
}

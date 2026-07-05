/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import static com.github.javaparser.ParseStart.COMPILATION_UNIT;
import static com.github.javaparser.ParseStart.STATEMENT;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_23;
import static com.github.javaparser.Providers.provider;
import static com.github.javaparser.utils.TestUtils.assertNoProblems;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Test;

/**
 * Test for Java 23 language level support.
 * Tests basic functionality inherited from Java 22.
 *
 * @see <a href="https://openjdk.org/projects/jdk/23/">https://openjdk.org/projects/jdk/23/</a>
 */
class Java23ValidatorTest {

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(JAVA_23));

    @Test
    void basicParsing() {
        ParseResult<CompilationUnit> result =
                javaParser.parse(COMPILATION_UNIT, provider("class X { void m() { System.out.println(\"Hello\"); } }"));
        assertNoProblems(result);
    }

    @Test
    void yieldStatementSupported() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("yield 42;"));
        assertNoProblems(result);
    }

    @Test
    void switchExpressionSupported() {
        ParseResult<Statement> result = javaParser.parse(
                STATEMENT, provider("int result = switch(x) { case 1 -> 10; case 2 -> 20; default -> 0; };"));
        assertNoProblems(result);
    }

    @Test
    void recordsSupported() {
        ParseResult<CompilationUnit> result =
                javaParser.parse(COMPILATION_UNIT, provider("record Point(int x, int y) {}"));
        assertNoProblems(result);
    }

    @Test
    void textBlocksSupported() {
        ParseResult<CompilationUnit> result = javaParser.parse(
                COMPILATION_UNIT, provider("class X { String s = \"\"\"\n    Hello\n    World\n    \"\"\"; }"));
        assertNoProblems(result);
    }

    @Test
    void unnamedVariablesFromJava22Supported() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("int _ = 42;"));
        assertNoProblems(result);
    }
}

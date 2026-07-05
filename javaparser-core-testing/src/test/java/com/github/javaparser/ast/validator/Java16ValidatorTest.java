/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import static com.github.javaparser.ParseStart.COMPILATION_UNIT;
import static com.github.javaparser.ParseStart.STATEMENT;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_16;
import static com.github.javaparser.Providers.provider;
import static com.github.javaparser.utils.TestUtils.assertNoProblems;
import static com.github.javaparser.utils.TestUtils.assertProblems;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class Java16ValidatorTest {

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(JAVA_16));

    @Test
    void localInterface() {
        ParseResult<CompilationUnit> result =
                javaParser.parse(COMPILATION_UNIT, provider("class X{ void x() {" + "interface I{}}}"));
        assertNoProblems(result);
    }

    @Nested
    class Yield {
        @Test
        void yieldAllowed() {
            ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("switch(x){case 3: yield 6;}"));
            assertNoProblems(result);
        }
    }

    @Nested
    class PatternMatching {
        @Test
        void patternMatchingAllowed() {
            ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("if (a instanceof String s) {}"));
            assertNoProblems(result);
        }

        @Test
        void recordPatternsForbidden() {
            ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("if (a instanceof Box(String s)) {}"));
            assertProblems(
                    result,
                    "(line 1,col 18) Record patterns are not supported. Pay attention that this feature is supported starting from 'JAVA_21' language level. If you need that feature the language level must be configured in the configuration before parsing the source files.");
        }
    }

    /**
     * Records are available within Java 14 (preview), Java 15 (2nd preview), and Java 16 (release).
     * The introduction of records means that they are no longer able to be used as identifiers.
     */
    @Nested
    class Record {

        @Nested
        class RecordAsTypeIdentifierForbidden {
            @Test
            void recordUsedAsClassIdentifier() {
                String s = "public class record {}";
                ParseResult<CompilationUnit> result = javaParser.parse(COMPILATION_UNIT, provider(s));
                assertProblems(
                        result,
                        "(line 1,col 14) 'record' is a restricted identifier and cannot be used for type declarations");
            }

            @Test
            void recordUsedAsEnumIdentifier() {
                String s = "public enum record {}";
                ParseResult<CompilationUnit> result = javaParser.parse(COMPILATION_UNIT, provider(s));
                assertProblems(
                        result,
                        "(line 1,col 13) 'record' is a restricted identifier and cannot be used for type declarations");
            }

            @Test
            void recordUsedAsRecordIdentifier() {
                String s = "public record record() {}";
                ParseResult<CompilationUnit> result = javaParser.parse(COMPILATION_UNIT, provider(s));
                assertProblems(
                        result,
                        "(line 1,col 15) 'record' is a restricted identifier and cannot be used for type declarations");
            }
        }

        @Nested
        class RecordUsedAsIdentifierAllowedAsFieldDeclarations {
            @Test
            void recordUsedAsFieldIdentifierInClass() {
                String s = "class X { int record; }";
                ParseResult<CompilationUnit> result = javaParser.parse(COMPILATION_UNIT, provider(s));
                assertNoProblems(result);
            }

            @Test
            void recordUsedAsFieldIdentifierInInterface() {
                String s = "interface X { int record; }";
                ParseResult<CompilationUnit> result = javaParser.parse(COMPILATION_UNIT, provider(s));
                assertNoProblems(result);
            }
        }

        @Nested
        class RecordDeclarationPermitted {
            @Test
            void recordDeclaration() {
                String s = "record X() { }";
                ParseResult<CompilationUnit> result = javaParser.parse(COMPILATION_UNIT, provider(s));
                assertNoProblems(result);
            }
        }
    }
}

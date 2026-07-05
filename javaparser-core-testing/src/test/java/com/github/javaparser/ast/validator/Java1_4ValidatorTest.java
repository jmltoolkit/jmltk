/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import static com.github.javaparser.ParseStart.*;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_1_4;
import static com.github.javaparser.Providers.provider;
import static com.github.javaparser.utils.TestUtils.assertNoProblems;
import static com.github.javaparser.utils.TestUtils.assertProblems;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Test;

class Java1_4ValidatorTest {
    public static final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(JAVA_1_4));

    @Test
    void yesAssert() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("assert a;"));
        assertNoProblems(result);
    }

    @Test
    void assertIsNotValideIdentifierSinceJAVA_1_4() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("String assert;"));
        assertProblems(
                result,
                "(line 1,col 8) 'assert' identifier is not supported. Pay attention that this feature is no longer supported since 'JAVA_1_4' language level. If you need that feature the language level must be configured in the configuration before parsing the source files.");
    }

    @Test
    void noGenerics() {
        ParseResult<CompilationUnit> result =
                javaParser.parse(COMPILATION_UNIT, provider("class X<A>{List<String> b;}"));
        assertProblems(
                result,
                "(line 1,col 1) Generics are not supported. Pay attention that this feature is supported starting from 'JAVA_5' language level. If you need that feature the language level must be configured in the configuration before parsing the source files.",
                "(line 1,col 12) Generics are not supported. Pay attention that this feature is supported starting from 'JAVA_5' language level. If you need that feature the language level must be configured in the configuration before parsing the source files.");
    }

    @Test
    void noAnnotations() {
        ParseResult<CompilationUnit> result =
                javaParser.parse(COMPILATION_UNIT, provider("@Abc @Def() @Ghi(a=3) @interface X{}"));
        assertProblems(
                result,
                "(line 1,col 13) Annotations are not supported. Pay attention that this feature is supported starting from 'JAVA_5' language level. If you need that feature the language level must be configured in the configuration before parsing the source files.",
                "(line 1,col 1) Annotations are not supported. Pay attention that this feature is supported starting from 'JAVA_5' language level. If you need that feature the language level must be configured in the configuration before parsing the source files.",
                "(line 1,col 6) Annotations are not supported. Pay attention that this feature is supported starting from 'JAVA_5' language level. If you need that feature the language level must be configured in the configuration before parsing the source files.");
    }

    @Test
    void novarargs() {
        ParseResult<Parameter> result = javaParser.parse(PARAMETER, provider("String... x"));
        assertProblems(
                result,
                "(line 1,col 1) Varargs are not supported. Pay attention that this feature is supported starting from 'JAVA_5' language level. If you need that feature the language level must be configured in the configuration before parsing the source files.");
    }

    @Test
    void noforeach() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("for(X x: xs){}"));
        assertProblems(
                result,
                "(line 1,col 1) For-each loops are not supported. Pay attention that this feature is supported starting from 'JAVA_5' language level. If you need that feature the language level must be configured in the configuration before parsing the source files.");
    }

    @Test
    void staticImport() {
        ParseResult<CompilationUnit> result = javaParser.parse(
                COMPILATION_UNIT, provider("import static x;import static x.*;import x.X;import x.*;"));
        assertProblems(
                result,
                "(line 1,col 1) Static imports are not supported. Pay attention that this feature is supported starting from 'JAVA_5' language level. If you need that feature the language level must be configured in the configuration before parsing the source files.",
                "(line 1,col 17) Static imports are not supported. Pay attention that this feature is supported starting from 'JAVA_5' language level. If you need that feature the language level must be configured in the configuration before parsing the source files.");
    }

    @Test
    void enumAllowedAsIdentifier() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("int enum;"));
        assertNoProblems(result);
    }
}

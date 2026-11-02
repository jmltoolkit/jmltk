/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JML declarations that are legal only at class level (model methods, model/ghost
 * fields, represents clauses, invariants, lemmas, ...) must be rejected when they
 * appear inside a method body, with a targeted diagnostic:
 *
 * <pre>{@code
 * class Foo {
 *     int m() {
 *         /*@ model int modelMethod; *&#47;   // not allowed — class-level declaration
 *         return 0;
 *     }
 * }
 * }</pre>
 */
class ModelDeclarationInMethodTest {

    private final ParserConfiguration config = new ParserConfiguration().setProcessJml(true);
    private final JavaParser javaParser = new JavaParser(config);

    private ParseResult<CompilationUnit> parse(String jmlInsideMethod) {
        return javaParser.parse(
                "class Foo {\n    int m() {\n        " + jmlInsideMethod + "\n        return 0;\n    }\n}\n");
    }

    private static List<String> messages(ParseResult<CompilationUnit> r) {
        return r.getProblems().stream().map(Problem::getMessage).collect(Collectors.toList());
    }

    @Test
    void modelMethodInMethodIsRejected() {
        ParseResult<CompilationUnit> r = parse("/*@ model int modelMethod() { return 0; } */");
        assertThat(r.isSuccessful()).isFalse();
        assertThat(messages(r))
                .anyMatch(
                        msg -> msg.contains("Model method modelMethod") && msg.contains("not allowed within methods"));
        assertThat(messages(r)).noneMatch(msg -> msg.contains("Could not handle the JML comment"));
    }

    @Test
    void modelFieldInMethodIsRejected() {
        ParseResult<CompilationUnit> r = parse("/*@ model int modelField; */");
        assertThat(r.isSuccessful()).isFalse();
        assertThat(messages(r))
                .anyMatch(msg -> msg.contains("Model field modelField") && msg.contains("not allowed within methods"));
        assertThat(messages(r)).noneMatch(msg -> msg.contains("Could not handle the JML comment"));
    }

    @Test
    void sameModelMethodAtClassLevelIsAccepted() {
        ParseResult<CompilationUnit> r = javaParser.parse(
                "class Foo {\n    /*@ model int modelMethod() { return 0; } */\n    int m() { return 0; }\n}\n");
        assertThat(r.isSuccessful())
                .as("class-level model method must parse:\n" + messages(r))
                .isTrue();
    }

    @Test
    void ghostLocalVariableInMethodIsStillAccepted() {
        ParseResult<CompilationUnit> r = parse("/*@ ghost int local; */");
        assertThat(r.isSuccessful())
                .as("local ghost variable statement must parse:\n" + messages(r))
                .isTrue();
    }

    @Test
    void unrelatedMalformedJmlKeepsGenericMessage() {
        ParseResult<CompilationUnit> r = parse("/*@ nonsense nonsense nonsense; */");
        assertThat(r.isSuccessful()).isFalse();
        assertThat(messages(r)).anyMatch(msg -> msg.contains("Could not handle the JML comment"));
    }
}

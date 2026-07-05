/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.ParseStart.COMPILATION_UNIT;
import static com.github.javaparser.Providers.provider;
import static com.github.javaparser.ast.Node.Parsedness.PARSED;
import static com.github.javaparser.ast.Node.Parsedness.UNPARSABLE;
import static org.assertj.core.api.Assertions.assertThat;

class ParseResultTest {
    private final JavaParser javaParser = new JavaParser(new ParserConfiguration());

    @Test
    void whenParsingSucceedsThenWeGetResultsAndNoProblems() {
        ParseResult<CompilationUnit> result = javaParser.parse(COMPILATION_UNIT, provider("class X{}"));

        assertThat(result.getResult().isPresent()).isTrue();
        assertThat(result.getResult().get().getParsed()).isEqualTo(PARSED);
        assertThat(result.getProblems()).isEmpty();

        assertThat(result.toString()).isEqualTo("Parsing successful");
    }

    @Test
    void whenParsingFailsThenWeGetProblemsAndABadResult() {
        ParseResult<CompilationUnit> result = javaParser.parse(COMPILATION_UNIT, provider("class {"));

        assertThat(result.getResult().isPresent()).isTrue();
        assertThat(result.getResult().get().getParsed()).isEqualTo(UNPARSABLE);
        assertThat(result.getProblems().size()).isEqualTo(1);

        Problem problem = result.getProblem(0);
        assertThat(problem.getMessage()).startsWith("Parse error. Found \"{\", ");

        assertThat(result.toString())
                .startsWith("Parsing failed:" + LineSeparator.SYSTEM + "(line 1,col 1) Parse error.");
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.ParseStart.COMPILATION_UNIT;
import static com.github.javaparser.Providers.provider;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for <a href="https://github.com/jmltoolkit/jmltk/issues/71">jmltk#71</a>.
 *
 * <p>The lookahead used to disambiguate a local variable declaration from other
 * statements must not swallow the parse error: a malformed declaration such as
 * {@code int i = ;} has to be reported at the offending token (the {@code =}),
 * not at the start of the statement.
 */
class VariableDeclarationErrorPositionTest {
    private final JavaParser javaParser = new JavaParser(new ParserConfiguration());

    @Test
    void malformedLocalVariableDeclarationReportsTheOffendingToken() {
        ParseResult<CompilationUnit> result = javaParser.parse(COMPILATION_UNIT, provider("""
                class A {
                    void m() {
                        int i = ;
                    }
                }
                """));

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getProblems()).hasSize(1);

        Problem problem = result.getProblem(0);
        TokenRange location = problem.getLocation().orElseThrow(AssertionError::new);
        Range range = location.getBegin().getRange().orElseThrow(AssertionError::new);

        // the offending "=" on line 3, column 15, not the statement start on line 3, column 9
        assertThat(range.begin.line).isEqualTo(3);
        assertThat(range.begin.column).isEqualTo(15);
    }
}

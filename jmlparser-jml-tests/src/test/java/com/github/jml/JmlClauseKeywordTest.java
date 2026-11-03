/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml;

import com.github.javaparser.GeneratedJavaParserConstants;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.jml.clauses.JmlClause;
import com.github.javaparser.ast.jml.clauses.JmlClauseKeyword;
import com.github.javaparser.ast.jml.clauses.JmlSimpleExprClause;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for the {@link JmlClauseKeyword} token constants and clause-kind resolution.
 */
class JmlClauseKeywordTest {
    private JavaParser javaParser = createJavaParser();

    private static JavaParser createJavaParser() {
        var config = new ParserConfiguration();
        config.setProcessJml(true);
        return new JavaParser(config);
    }

    @Test
    void decreaseKindsReferenceTheirOwnTokenConstants() {
        assertThat(JmlClauseKeyword.DECREASING.getTokenType()).isEqualTo(GeneratedJavaParserConstants.DECREASING);
        assertThat(JmlClauseKeyword.DECREASING_REDUNDANTLY.getTokenType())
                .isEqualTo(GeneratedJavaParserConstants.DECREASING_REDUNDANTLY);
    }

    @Test
    void informationFlowKindsReferenceTheirOwnTokenConstants() {
        assertThat(JmlClauseKeyword.DETERMINES.getTokenType()).isEqualTo(GeneratedJavaParserConstants.DETERMINES);
        assertThat(JmlClauseKeyword.LOOP_DETERMINES.getTokenType())
                .isEqualTo(GeneratedJavaParserConstants.LOOP_DETERMINES);
        assertThat(JmlClauseKeyword.SEPARATES.getTokenType()).isEqualTo(GeneratedJavaParserConstants.SEPARATES);
        assertThat(JmlClauseKeyword.LOOP_SEPARATES.getTokenType())
                .isEqualTo(GeneratedJavaParserConstants.LOOP_SEPARATES);
    }

    @Test
    void parsesDecreasingRedundantlyLoopContract() {
        var cu = javaParser.parse("""
                class A {
                    void m(int n) {
                        /*@ loop_invariant n >= 0;
                          @ decreasing_redundantly n;
                          @*/
                        while (n > 0) n--;
                    }
                }
                """);
        assertThat(cu.getProblems()).isEmpty();
        var clauses = cu.getResult().get().findAll(JmlSimpleExprClause.class);
        assertThat(clauses).anyMatch(c -> c.getKind().getValue() == JmlClauseKeyword.DECREASING_REDUNDANTLY);
    }

    @Test
    void parsesDecreasingAndDeterminesClauses() {
        var cu = javaParser.parse("""
                class A {
                    void m(int n) {
                        /*@ loop_invariant n >= 0;
                          @ decreasing n;
                          @*/
                        while (n > 0) n--;
                    }
                    /*@ normal_behavior
                      @   requires n > 0;
                      @   ensures \\result >= 0;
                      @   determines n;
                      @*/
                    int f(int n) { return n; }
                }
                """);
        assertThat(cu.getProblems()).isEmpty();
        var kinds = cu.getResult().get().findAll(JmlClause.class).stream()
                .map(c -> c.getKind().getValue())
                .toList();
        assertThat(kinds).contains(JmlClauseKeyword.DECREASING, JmlClauseKeyword.DETERMINES);
    }
}

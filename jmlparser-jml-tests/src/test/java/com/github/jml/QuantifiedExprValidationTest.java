/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.expr.Expression;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Syntactic validation of iterable-domain quantifiers:
 * {@code (\forall int e : a; ...)} and friends. Multi-declarator + iterable is
 * rejected by the grammar at parse time; the binding-shape checks (no
 * initializer, the domain must not reference the bound variable) live in
 * {@link com.github.javaparser.jml.JmlProcessor} and fire when the parser is
 * configured with {@code processJml=true}.
 *
 * @author Alexander Weigl
 */
class QuantifiedExprValidationTest {
    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setProcessJml(true));

    private void assertProblem(String input, String messageFragment) {
        ParseResult<Expression> r = javaParser.parseJmlExpression(input);
        assertThat(r.isSuccessful()).as(input).isFalse();
        assertThat(r.getProblems())
                .as(input)
                .anySatisfy(problem -> assertThat(problem.getMessage()).contains(messageFragment));
    }

    @Test
    void validIterableQuantifiersParseCleanly() {
        for (String input : new String[] {
            "(\\forall int e : a; e > 0)",
            "(\\forall var e : a; e != null)",
            "(\\forall int i : 0..n; a[i])",
            "(\\forall int i; 0 <= i < 5; a[i])",
            "(\\forall int e : list.e; e > 0)"
        }) {
            ParseResult<Expression> r = javaParser.parseJmlExpression(input);
            assertThat(r.isSuccessful()).as(input).isTrue();
            assertThat(r.getProblems()).as(input).isEmpty();
        }
    }

    @Test
    void iterableBindsExactlyOneVariable() {
        assertProblem("(\\forall int i, j : 0..n; i < j)", "binds exactly one variable");
    }

    @Test
    void iterableMustNotReferenceBoundVariable() {
        assertProblem("(\\forall int e : a[e]; e > 0)", "must not reference the bound variable");
    }

    @Test
    void boundBindingMustNotHaveInitializer() {
        assertProblem("(\\forall int e = 3 : a; e > 0)", "must not have an initializer");
    }
}

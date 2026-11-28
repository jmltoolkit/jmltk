/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins parser behaviors that the rejected grammar experiments (P1/P3) could have changed. These are
 * deliberate regression tests, not coverage for new functionality.
 */
class ParserInvariantsTest {

    private static boolean parsesAsIdentifier(String text) {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.getProcessors().clear();
        return new JavaParser(configuration)
                .parse("class X { Object " + text + "; }")
                .isSuccessful();
    }

    @Test
    void truncatedInputReturnsAResultInsteadOfThrowing() {
        assertDoesNotThrow(() -> new JavaParser().parse("class X { Object "));
        assertDoesNotThrow(
                () -> new JavaParser(new ParserConfiguration().setProcessJml(true)).parse("class X { void m() { a."));
    }

    @Test
    void keywordLikeTokensStillParseAsIdentifiers() {
        String[] keywords = {
            "invariant",
            "requires",
            "ensures",
            "record",
            "yield",
            "module",
            "open",
            "when",
            "sealed",
            "source",
            "exec",
            "ccatch",
            "ccat",
            "_"
        };
        for (String keyword : keywords) {
            assertTrue(parsesAsIdentifier(keyword), "keyword is no longer accepted as an identifier: " + keyword);
        }
    }

    @Test
    void jmlMarkersParseWithProcessingDisabled() {
        String source = "class X { //@ invariant true;\n void m() {} }";
        var result = new JavaParser(new ParserConfiguration().setProcessJml(false)).parse(source);
        assertTrue(result.isSuccessful(), result.getProblems().toString());
    }

    @Test
    void scopedGenericTypesParse() {
        var result = new JavaParser().parse("class X { a.b.C<D>.E<F> f; Outer<T>.Inner<U> g; }");
        assertTrue(result.isSuccessful(), result.getProblems().toString());
        var types = result.getResult().orElseThrow().findAll(ClassOrInterfaceType.class);
        assertTrue(types.stream()
                .anyMatch(t -> t.getNameAsString().equals("E")
                        && t.getTypeArguments().orElseThrow().size() == 1
                        && t.getScope().orElseThrow().getNameAsString().equals("C")));
        assertTrue(types.stream()
                .anyMatch(t -> t.getNameAsString().equals("Inner")
                        && t.getScope().orElseThrow().getNameAsString().equals("Outer")));
    }
}

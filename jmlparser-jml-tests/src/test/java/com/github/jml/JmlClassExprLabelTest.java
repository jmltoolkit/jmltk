/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.jml.body.JmlClassExprDeclaration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JmlClassExprLabelTest {

    @Test
    void keepsLabelOfClassLevelExpression() {
        var result = new JavaParser(new ParserConfiguration().setProcessJml(true))
                .parseJmlClassLevel("invariant positive: true;");
        assertTrue(result.isSuccessful(), result.getProblems().toString());

        JmlClassExprDeclaration declaration = result.getResult().orElseThrow().getChildren().stream()
                .filter(JmlClassExprDeclaration.class::isInstance)
                .map(JmlClassExprDeclaration.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no class expression parsed: "
                        + result.getResult().orElseThrow().getChildNodes()));
        assertEquals("positive", declaration.getName().orElseThrow().asString());
        assertTrue(declaration.toString().contains("positive:"), declaration.toString());
    }

    @Test
    void printsTheActualClauseKind() {
        var result = new JavaParser(new ParserConfiguration().setProcessJml(true))
                .parseJmlClassLevel("axiom labelled: true;");
        assertTrue(result.isSuccessful(), result.getProblems().toString());
        JmlClassExprDeclaration declaration = result.getResult().orElseThrow().getChildren().stream()
                .filter(JmlClassExprDeclaration.class::isInstance)
                .map(JmlClassExprDeclaration.class::cast)
                .findFirst()
                .orElseThrow();
        assertTrue(declaration.toString().contains("axiom"), declaration.toString());
        assertTrue(declaration.toString().contains("labelled:"), declaration.toString());
    }

    @Test
    void stillParsesUnlabeledClassLevelExpression() {
        var result =
                new JavaParser(new ParserConfiguration().setProcessJml(true)).parseJmlClassLevel("invariant true;");
        assertTrue(result.isSuccessful(), result.getProblems().toString());

        JmlClassExprDeclaration declaration = result.getResult().orElseThrow().getChildren().stream()
                .filter(JmlClassExprDeclaration.class::isInstance)
                .map(JmlClassExprDeclaration.class::cast)
                .findFirst()
                .orElseThrow();
        assertTrue(declaration.getName().isEmpty());
    }
}

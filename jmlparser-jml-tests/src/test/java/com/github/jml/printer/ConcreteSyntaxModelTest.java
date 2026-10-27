/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.printer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.jml.ArbitraryNodeContainer;
import com.github.javaparser.ast.jml.body.JmlLemmaDeclaration;
import com.github.javaparser.printer.ConcreteSyntaxModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static com.github.javaparser.StaticJavaParser.parseJmlExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcreteSyntaxModelTest {

    private String print(Node node) {
        return ConcreteSyntaxModel.genericPrettyPrint(node);
    }

    @ParameterizedTest()
    @CsvFileSource(resources = "/csm_jml_expression.txt", delimiterString = "FOOBARFOOBAZ")
    void printQuantifiedExpr(String line) {
        Expression expr = parseJmlExpression(line);
        System.out.println(expr);
        assertEquals(line, print(expr));
    }

    @ParameterizedTest()
    @CsvFileSource(resources = "/csm_jml_classlevel.txt", delimiterString = "FOOBARFOOBAZ")
    void printJmlClassLevel(String line) {
        ParseResult<ArbitraryNodeContainer> result = new JavaParser().parseJmlClassLevel(line);
        Assertions.assertTrue(result.isSuccessful(), () -> result.getProblems().toString());
        JmlLemmaDeclaration lemma = result.getResult().get().getChildren().stream()
                .filter(n -> n instanceof JmlLemmaDeclaration)
                .map(n -> (JmlLemmaDeclaration) n)
                .findFirst()
                .orElseThrow();
        assertEquals(line, print(lemma));
    }
}

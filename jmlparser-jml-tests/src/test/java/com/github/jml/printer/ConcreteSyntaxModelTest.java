/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.printer;

import static com.github.javaparser.StaticJavaParser.parseJmlExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.printer.ConcreteSyntaxModel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

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
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.expr.Expression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/**
 * @author Alexander Weigl
 * @version 1 (11.07.22)
 */
class JmlExpressionTest {
    private JavaParser javaParser = new JavaParser();

    @ParameterizedTest
    @CsvFileSource(resources = "/jml-exprs.txt", delimiterString = "FOOBARBAZ")
    void parse(String input) {
        ParseResult<Expression> r = javaParser.parseJmlExpression(input);
        if (!r.isSuccessful()) {
            r.getProblems().forEach(System.out::println);
        }
        Assertions.assertTrue(r.isSuccessful());
    }
}

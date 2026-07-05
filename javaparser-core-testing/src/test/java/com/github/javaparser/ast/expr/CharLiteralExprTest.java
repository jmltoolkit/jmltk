/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParserConfiguration;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.Providers.provider;
import static com.github.javaparser.StaticJavaParser.parseExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CharLiteralExprTest {
    @Test
    void parseSimpleChar() {
        CharLiteralExpr c = parseExpression("'a'");
        assertEquals("a", c.getValue());
    }

    @Test
    void parseSimpleEscape() {
        CharLiteralExpr c = parseExpression("'\\t'");
        assertEquals("\\t", c.getValue());
    }

    @Test
    void parseUnicode() {
        CharLiteralExpr c = parseExpression("'Ω'");
        assertEquals("Ω", c.getValue());
    }

    @Test
    void parseNumericEscape() {
        CharLiteralExpr c = parseExpression("'\\177'");
        assertEquals("\\177", c.getValue());
    }

    @Test
    void parseUnicodeEscape() {
        CharLiteralExpr c = parseExpression("'\\u03a9'");
        assertEquals("\\u03a9", c.getValue());
    }

    @Test
    void parseUnicodeEscapedEscape() {
        JavaParser javaParser = new JavaParser(new ParserConfiguration().setPreprocessUnicodeEscapes(true));

        CharLiteralExpr c = javaParser
                .parse(ParseStart.EXPRESSION, provider("'\\u005c''"))
                .getResult()
                .get()
                .asCharLiteralExpr();
        assertEquals("\\'", c.getValue());
    }
}

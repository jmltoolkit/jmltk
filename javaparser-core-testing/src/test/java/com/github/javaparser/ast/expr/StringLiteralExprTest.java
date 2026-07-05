/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringLiteralExprTest {
    @Test
    void unicodeEscapesArePreservedInStrings() {
        StringLiteralExpr omega = parseExpression("\"xxx\\u03a9xxx\"");
        assertEquals("xxx\\u03a9xxx", omega.getValue());
    }
}

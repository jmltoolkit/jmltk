/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.utils.TestParser;
import org.junit.jupiter.api.Test;

class ObjectCreationExprTest {
    @Test
    void aaa() {
        Expression e = TestParser.parseExpression("new @Test N()");
        assertEquals("new @Test N()", e.toString());
    }
}

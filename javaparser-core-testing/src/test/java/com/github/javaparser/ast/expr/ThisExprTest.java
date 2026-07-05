/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThisExprTest {
    @Test
    void justThis() {
        Expression expr = parseExpression("this");

        assertTrue(expr.isThisExpr());
    }

    @Test
    void justThisName() {
        JavaParser javaParser = new JavaParser(new ParserConfiguration().setStoreTokens(false));
        ParseResult<Expression> parseResult = javaParser.parseExpression("this.c");
        FieldAccessExpr fieldAccess = parseResult.getResult().get().asFieldAccessExpr();
        assertEquals("c", fieldAccess.getName().asString());
    }

    @Test
    void singleScopeThis() {
        Expression expr = parseExpression("A.this");

        Name className = expr.asThisExpr().getTypeName().get();

        assertEquals("A", className.asString());
    }

    @Test
    void singleScopeThisName() {
        JavaParser javaParser = new JavaParser(new ParserConfiguration().setStoreTokens(false));
        ParseResult<Expression> parseResult = javaParser.parseExpression("A.this.c");
        FieldAccessExpr fieldAccess = parseResult.getResult().get().asFieldAccessExpr();
        assertEquals("c", fieldAccess.getName().asString());
    }

    @Test
    void multiScopeThis() {
        Expression expr = parseExpression("a.B.this");

        Name className = expr.asThisExpr().getTypeName().get();

        assertEquals("a.B", className.asString());
    }

    @Test
    void multiScopeThisName() {
        JavaParser javaParser = new JavaParser(new ParserConfiguration().setStoreTokens(false));
        ParseResult<Expression> parseResult = javaParser.parseExpression("a.B.this.c");
        FieldAccessExpr fieldAccess = parseResult.getResult().get().asFieldAccessExpr();
        assertEquals("c", fieldAccess.getName().asString());
    }
}

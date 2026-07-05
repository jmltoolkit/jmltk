/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.stmt;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_12;
import static com.github.javaparser.utils.TestParser.parseCompilationUnit;
import static com.github.javaparser.utils.TestParser.parseStatement;
import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import org.junit.jupiter.api.Test;

class YieldStmtTest {
    @Test
    void yield() {
        YieldStmt statement = parseStatement("yield 12*12;").asYieldStmt();
        assertEquals(BinaryExpr.class, statement.getExpression().getClass());
    }

    @Test
    void yield2() {
        YieldStmt statement;
        statement = parseStatement("yield (2 + 2);").asYieldStmt();
        assertEquals(EnclosedExpr.class, statement.getExpression().getClass());
        statement = parseStatement("yield ((2 + 2) * 3);").asYieldStmt();
        assertEquals(EnclosedExpr.class, statement.getExpression().getClass());
    }

    @Test
    void yieldMethodCall() {
        YieldStmt statement;
        statement = parseStatement("yield a();").asYieldStmt();
        assertEquals(MethodCallExpr.class, statement.getExpression().getClass());
        statement = parseStatement("yield a(5, arg);").asYieldStmt();
        assertEquals(MethodCallExpr.class, statement.getExpression().getClass());
        statement = parseStatement("yield a.b();").asYieldStmt();
        assertEquals(MethodCallExpr.class, statement.getExpression().getClass());
        statement = parseStatement("yield a.b(5, arg);").asYieldStmt();
        assertEquals(MethodCallExpr.class, statement.getExpression().getClass());
        statement = parseStatement("yield this.b();").asYieldStmt();
        assertEquals(MethodCallExpr.class, statement.getExpression().getClass());
        statement = parseStatement("yield this.b(5, arg);").asYieldStmt();
        assertEquals(MethodCallExpr.class, statement.getExpression().getClass());
        statement = parseStatement("yield Clazz.this.b();").asYieldStmt();
        assertEquals(MethodCallExpr.class, statement.getExpression().getClass());
        statement = parseStatement("yield Clazz.this.b(5, arg);").asYieldStmt();
        assertEquals(MethodCallExpr.class, statement.getExpression().getClass());
    }

    @Test
    void yieldAssignment() {
        YieldStmt statement = parseStatement("yield (x = 5);").asYieldStmt();
        assertEquals(EnclosedExpr.class, statement.getExpression().getClass());
    }

    @Test
    void yieldConditional() {
        YieldStmt statement = parseStatement("yield x ? 5 : 6;").asYieldStmt();
        assertEquals(ConditionalExpr.class, statement.getExpression().getClass());
    }

    @Test
    void threadYieldShouldNotBreak() {
        parseStatement("Thread.yield();").asExpressionStmt().getExpression().asMethodCallExpr();
    }

    @Test
    void keywordShouldNotInterfereWithIdentifiers() {
        CompilationUnit compilationUnit =
                parseCompilationUnit(JAVA_12, "class yield { yield yield(yield yield){yield();} }");
        assertEqualsStringIgnoringEol(
                "class yield {\n" + "\n"
                        + "    yield yield(yield yield) {\n"
                        + "        yield();\n"
                        + "    }\n"
                        + "}\n",
                compilationUnit.toString());
    }

    @Test
    void keywordShouldNotInterfereWithIdentifiers2() {
        CompilationUnit compilationUnit = parseCompilationUnit("enum X { yield, }");
        assertEqualsStringIgnoringEol("enum X {\n" + "\n" + "    yield\n" + "}\n", compilationUnit.toString());
    }

    @Test
    void keywordShouldNotInterfereWithIdentifiers3() {
        YieldStmt statement;
        statement = parseStatement("yield yield;").asYieldStmt();
        assertEquals(NameExpr.class, statement.getExpression().getClass());
        statement = parseStatement("yield Clazz.yield();").asYieldStmt();
        assertEquals(MethodCallExpr.class, statement.getExpression().getClass());
        statement = parseStatement("yield yield.yield();").asYieldStmt();
        assertEquals(MethodCallExpr.class, statement.getExpression().getClass());
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BinaryExprTest {

    @BeforeEach
    void initParser() {
        StaticJavaParser.setConfiguration(new ParserConfiguration());
    }

    @Test
    void convertOperator() {
        assertEquals(
                AssignExpr.Operator.PLUS,
                BinaryExpr.Operator.PLUS.toAssignOperator().get());
    }

    /**
     * Evaluation takes place left to right, with && taking precedence over ||
     * <p>
     * true || false && false || false
     * true ||      (1)       || false
     * (        2           ) || false
     * (             3               )
     * <p>
     * true || false && false || false
     * true ||    (false)     || false
     * (     true           ) || false
     * (           true              )
     */
    @Nested
    class LogicalOperatorPrecedence {

        @Test
        public void logicalAndOr() {
            Expression expression = StaticJavaParser.parseExpression("true || false && false || false");
            Expression bracketedExpression = applyBrackets(expression);

            String expected = "(true || (false && false)) || false";
            String actual = bracketedExpression.toString();

            assertEquals(expected, actual);
        }

        @Test
        public void logicalOrEvaluationLeftToRight() {
            Expression expression = StaticJavaParser.parseExpression("false || true || false || true || false || true");
            Expression bracketedExpression = applyBrackets(expression);

            String expected = "((((false || true) || false) || true) || false) || true";
            String actual = bracketedExpression.toString();

            assertEquals(expected, actual);
        }

        @Test
        public void logicalAndEvaluationLeftToRight() {
            Expression expression = StaticJavaParser.parseExpression("false && true && false && true && false && true");
            Expression bracketedExpression = applyBrackets(expression);

            String expected = "((((false && true) && false) && true) && false) && true";
            String actual = bracketedExpression.toString();

            assertEquals(expected, actual);
        }

        @Test
        public void andTakesPrecedenceOverOr() {
            Expression expression = StaticJavaParser.parseExpression("true || false && false");
            Expression bracketedExpression = applyBrackets(expression);

            String expected = "true || (false && false)";
            String actual = bracketedExpression.toString();

            assertEquals(expected, actual);
        }

        @Test
        public void andTakesPrecedenceOverOrThenLeftToRight() {
            Expression expression = StaticJavaParser.parseExpression("true || false && false || true");
            Expression bracketedExpression = applyBrackets(expression);

            String expected = "(true || (false && false)) || true";
            String actual = bracketedExpression.toString();

            assertEquals(expected, actual);
        }

        @Test
        public void example() {
            Expression expression =
                    StaticJavaParser.parseExpression("year % 4 == 0 && year % 100 != 0 || year % 400 == 0");
            Expression bracketedExpression = applyBrackets(expression);

            String expected = "((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0)";
            String actual = bracketedExpression.toString();

            assertEquals(expected, actual);
        }
    }

    private Expression applyBrackets(Expression expression) {
        expression.findAll(BinaryExpr.class).stream()
                .filter(binaryExpr -> binaryExpr.getOperator() == BinaryExpr.Operator.AND
                        || binaryExpr.getOperator() == BinaryExpr.Operator.OR)
                .forEach(binaryExpr -> {
                    if (!binaryExpr.getLeft().isBooleanLiteralExpr()) {
                        binaryExpr.setLeft(new EnclosedExpr(binaryExpr.getLeft()));
                    }
                    if (!binaryExpr.getRight().isBooleanLiteralExpr()) {
                        binaryExpr.setRight(new EnclosedExpr(binaryExpr.getRight()));
                    }
                });

        return expression;
    }

    @Test
    void binaryExprWithAssertAsLeftOperandTest() {
        // Note: "assert" as an identifier is only valid in Java < 1.4
        // In modern Java, "assert" is a keyword. However, "assert" as an identifier is still supported
        // for backward compatibility.
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_1_0);

        Expression e = parseExpression("assert + 42");
        assertInstanceOf(BinaryExpr.class, e);
        BinaryExpr binary = e.asBinaryExpr();
        assertEquals(BinaryExpr.Operator.PLUS, binary.getOperator());

        // Check left operand is "assert" (as identifier)
        assertInstanceOf(NameExpr.class, binary.getLeft());
        assertEquals("assert", binary.getLeft().asNameExpr().getNameAsString());

        assertInstanceOf(IntegerLiteralExpr.class, binary.getRight());
        assertEquals("42", binary.getRight().asIntegerLiteralExpr().getValue());
    }

    @Test
    void binaryExprWithAssertAsRightOperandTest() {
        // Note: "assert" as an identifier is only valid in Java < 1.4
        // In modern Java, "assert" is a keyword. However, "assert" as an identifier is still supported
        // for backward compatibility.
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_1_0);

        Expression e = parseExpression("x + assert");
        assertInstanceOf(BinaryExpr.class, e);
        BinaryExpr binary = e.asBinaryExpr();
        assertEquals(BinaryExpr.Operator.PLUS, binary.getOperator());

        assertInstanceOf(NameExpr.class, binary.getLeft());
        assertEquals("x", binary.getLeft().asNameExpr().getNameAsString());

        // Check right operand is "assert" (as identifier)
        assertInstanceOf(NameExpr.class, binary.getRight());
        assertEquals("assert", binary.getRight().asNameExpr().getNameAsString());
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.visitor;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.printer.lexicalpreservation.AbstractLexicalPreservingTest;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.StaticJavaParser.parseBodyDeclaration;
import static com.github.javaparser.StaticJavaParser.parseExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModifierVisitorTest extends AbstractLexicalPreservingTest {
    @Test
    void makeSureParentListsCanBeModified() {
        NodeList<StringLiteralExpr> list = new NodeList<>();
        list.add(new StringLiteralExpr("t"));
        list.add(new StringLiteralExpr("a"));
        list.add(new StringLiteralExpr("b"));
        list.add(new StringLiteralExpr("c"));

        list.accept(
                new ModifierVisitor<Void>() {
                    @Override
                    public Visitable visit(final StringLiteralExpr n, final Void arg) {
                        String v = n.getValue();

                        list.addNFirst(new StringLiteralExpr("extra " + v));
                        list.remove(new StringLiteralExpr("t"));

                        if (v.equals("a")) {
                            return new StringLiteralExpr("x");
                        }
                        if (v.equals("b")) {
                            return null;
                        }

                        return n;
                    }
                },
                null);

        assertEquals("extra c", list.get(0).getValue());
        assertEquals("extra b", list.get(1).getValue());
        assertEquals("extra a", list.get(2).getValue());
        assertEquals("extra t", list.get(3).getValue());
        assertEquals("x", list.get(4).getValue());
        assertEquals("c", list.get(5).getValue());
        assertEquals(6, list.size());
    }

    @Test
    void binaryExprReturnsLeftExpressionWhenRightSideIsRemoved() {
        Expression expression = parseExpression("1+2");
        Visitable result = expression.accept(
                new ModifierVisitor<Void>() {
                    public Visitable visit(IntegerLiteralExpr integerLiteralExpr, Void arg) {
                        if (integerLiteralExpr.getValue().equals("1")) {
                            return null;
                        }
                        return integerLiteralExpr;
                    }
                },
                null);
        assertEquals("2", result.toString());
    }

    @Test
    void binaryExprReturnsRightExpressionWhenLeftSideIsRemoved() {
        final Expression expression = parseExpression("1+2");
        final Visitable result = expression.accept(
                new ModifierVisitor<Void>() {
                    public Visitable visit(IntegerLiteralExpr integerLiteralExpr, Void arg) {
                        if (integerLiteralExpr.getValue().equals("2")) {
                            return null;
                        }
                        return integerLiteralExpr;
                    }
                },
                null);
        assertEquals("1", result.toString());
    }

    @Test
    void fieldDeclarationCantSurviveWithoutVariables() {
        final BodyDeclaration<?> bodyDeclaration = parseBodyDeclaration("int x=1;");

        final Visitable result = bodyDeclaration.accept(
                new ModifierVisitor<Void>() {
                    public Visitable visit(VariableDeclarator x, Void arg) {
                        return null;
                    }
                },
                null);

        assertNull(result);
    }

    @Test
    void variableDeclarationCantSurviveWithoutVariables() {
        final BodyDeclaration<?> bodyDeclaration = parseBodyDeclaration("void x() {int x=1;}");

        final Visitable result = bodyDeclaration.accept(
                new ModifierVisitor<Void>() {
                    public Visitable visit(VariableDeclarator x, Void arg) {
                        return null;
                    }
                },
                null);

        assertEquals("void x() {" + LineSeparator.SYSTEM + "}", result.toString());
    }

    @Test
    void issue2124() {
        ModifierVisitor<Void> modifier = new ModifierVisitor<>();
        considerCode("\n" + "public class ModifierVisitorTest {\n"
                + "    private void causesException() {\n"
                + "        String[] listWithExtraCommaAndEqualElements = {\"a\", \"a\",};\n"
                + "    }\n"
                + "}");
        cu.accept(modifier, null);
        // there should be no exception
        LexicalPreservingPrinter.print(cu);
    }
}

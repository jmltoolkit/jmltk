/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.Solver;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.SymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Note this issue number refers to the archived `javasymbolsolver` repository,
 * whose issues prior to it being integrated into JavaParser itself are numbered separately:
 *
 * https://github.com/javaparser/javasymbolsolver/issues/343
 */
class Issue343Test extends AbstractResolutionTest {

    private TypeSolver typeResolver;
    private Solver symbolSolver;

    private ResolvedType getExpressionType(TypeSolver typeSolver, Expression expression) {
        return JavaParserFacade.get(typeSolver).getType(expression);
    }

    @BeforeEach
    void setup() {
        typeResolver = new ReflectionTypeSolver();
        symbolSolver = new SymbolSolver(typeResolver);
    }

    @Test
    void resolveStringLiteralOutsideAST() {
        assertTrue(symbolSolver
                .classToResolvedType(String.class)
                .equals(getExpressionType(typeResolver, new StringLiteralExpr(""))));
    }

    @Test
    void resolveIntegerLiteralOutsideAST() {
        assertEquals(
                symbolSolver.classToResolvedType(int.class),
                getExpressionType(typeResolver, new IntegerLiteralExpr(2)));
    }

    @Test
    void toResolveDoubleWeNeedTheAST() {
        assertThrows(
                UnsolvedSymbolException.class,
                () -> getExpressionType(typeResolver, parseExpression("new Double[]{2.0d, 3.0d}[1]")));
    }

    @Test
    void toResolveFloatWeNeedTheAST() {
        assertThrows(
                UnsolvedSymbolException.class,
                () -> getExpressionType(typeResolver, parseExpression("new Float[]{2.0d, 3.0d}[1]")));
    }

    @Test
    void resolveMethodCallOnStringLiteralOutsideAST() {
        assertTrue(symbolSolver
                .classToResolvedType(int.class)
                .equals(getExpressionType(typeResolver, new MethodCallExpr(new StringLiteralExpr("hello"), "length"))));
    }

    @Test
    void resolveLocaleOutsideAST() {
        assertThrows(
                UnsolvedSymbolException.class,
                () -> getExpressionType(typeResolver, new FieldAccessExpr(new NameExpr("Locale"), "US")));
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.LeanParserConfiguration;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Issue144Test extends AbstractResolutionTest {

    private TypeSolver typeSolver;

    @BeforeEach
    void setup() {
        Path srcDir = adaptPath("src/test/resources/issue144");
        typeSolver = new JavaParserTypeSolver(srcDir, new LeanParserConfiguration());
    }

    @Test
    void issue144() {
        CompilationUnit cu = parseSampleWithStandardExtension("issue144/HelloWorld");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "HelloWorld");
        ExpressionStmt expressionStmt = (ExpressionStmt)
                clazz.getMethodsByName("main").get(0).getBody().get().getStatement(0);
        MethodCallExpr methodCallExpr = (MethodCallExpr) expressionStmt.getExpression();
        Expression firstParameter = methodCallExpr.getArgument(0);
        JavaParserFacade javaParserFacade = JavaParserFacade.get(typeSolver);

        assertEquals(true, javaParserFacade.solve(firstParameter).isSolved());
        assertEquals(
                true,
                javaParserFacade
                        .solve(firstParameter)
                        .getCorrespondingDeclaration()
                        .isField());
        assertEquals(
                "hw",
                javaParserFacade
                        .solve(firstParameter)
                        .getCorrespondingDeclaration()
                        .getName());
    }

    @Test
    void issue144WithReflectionTypeSolver() {
        CompilationUnit cu = parseSampleWithStandardExtension("issue144/HelloWorld");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "HelloWorld");
        ExpressionStmt expressionStmt = (ExpressionStmt)
                clazz.getMethodsByName("main").get(0).getBody().get().getStatement(0);
        MethodCallExpr methodCallExpr = (MethodCallExpr) expressionStmt.getExpression();
        Expression firstParameter = methodCallExpr.getArgument(0);
        JavaParserFacade javaParserFacade = JavaParserFacade.get(new ReflectionTypeSolver(true));

        assertEquals(true, javaParserFacade.solve(firstParameter).isSolved());
    }

    @Test
    void issue144WithCombinedTypeSolver() {
        CompilationUnit cu = parseSampleWithStandardExtension("issue144/HelloWorld");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "HelloWorld");
        ExpressionStmt expressionStmt = (ExpressionStmt)
                clazz.getMethodsByName("main").get(0).getBody().get().getStatement(0);
        MethodCallExpr methodCallExpr = (MethodCallExpr) expressionStmt.getExpression();
        Expression firstParameter = methodCallExpr.getArgument(0);
        JavaParserFacade javaParserFacade =
                JavaParserFacade.get(new CombinedTypeSolver(typeSolver, new ReflectionTypeSolver(true)));

        assertEquals(true, javaParserFacade.solve(firstParameter).isSolved());
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.List;
import org.junit.jupiter.api.Test;

class Issue186Test extends AbstractResolutionTest {

    @Test
    void lambdaFlatMapIssue() {
        CompilationUnit cu = parseSample("Issue186");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "JavaTest");
        MethodDeclaration methodDeclaration = Navigator.demandMethod(clazz, "foo");
        MethodCallExpr methodCallExpr =
                Navigator.findMethodCall(methodDeclaration, "flatMap").get();
        TypeSolver typeSolver = new ReflectionTypeSolver();
        JavaParserFacade javaParserFacade = JavaParserFacade.get(typeSolver);
        assertEquals(
                "java.util.stream.Stream<java.lang.String>",
                javaParserFacade.getType(methodCallExpr).describe());
    }

    @Test
    void lambdaPrimitivesIssue() {
        CompilationUnit cu = parseSample("Issue186");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "JavaTest");
        MethodDeclaration methodDeclaration = Navigator.demandMethod(clazz, "bar");
        List<LambdaExpr> lambdas = methodDeclaration.findAll(LambdaExpr.class);
        TypeSolver typeSolver = new ReflectionTypeSolver();
        JavaParserFacade javaParserFacade = JavaParserFacade.get(typeSolver);
        assertEquals(
                "java.util.function.Predicate<? super java.lang.String>",
                javaParserFacade.getType(lambdas.get(0)).describe());
        assertEquals(
                "java.util.function.Function<? super java.lang.String, ? extends java.lang.Integer>",
                javaParserFacade.getType(lambdas.get(1)).describe());
        assertEquals(
                "java.util.function.Predicate<? super java.lang.Integer>",
                javaParserFacade.getType(lambdas.get(2)).describe());
    }
}

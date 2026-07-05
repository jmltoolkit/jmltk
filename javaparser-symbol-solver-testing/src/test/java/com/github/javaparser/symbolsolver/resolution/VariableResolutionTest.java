/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class VariableResolutionTest extends AbstractResolutionTest {

    @Test
    void variableResolutionNoBlockStmt() {
        // Test without nested block statement

        CompilationUnit cu = parseSample("VariableResolutionInVariousScopes");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "VariableResolutionInVariousScopes");

        MethodDeclaration method = Navigator.demandMethod(clazz, "noBlock");
        MethodCallExpr callExpr = method.findFirst(MethodCallExpr.class).get();
        MethodUsage methodUsage =
                JavaParserFacade.get(new ReflectionTypeSolver()).solveMethodAsUsage(callExpr);

        assertTrue(methodUsage.declaringType().getQualifiedName().equals("java.lang.String"));
    }

    @Test
    void variableResolutionWithBlockStmt() {
        // Test without nested block statement

        CompilationUnit cu = parseSample("VariableResolutionInVariousScopes");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "VariableResolutionInVariousScopes");

        MethodDeclaration method = Navigator.demandMethod(clazz, "withBlock");
        MethodCallExpr callExpr = method.findFirst(MethodCallExpr.class).get();
        MethodUsage methodUsage =
                JavaParserFacade.get(new ReflectionTypeSolver()).solveMethodAsUsage(callExpr);

        assertTrue(methodUsage.declaringType().getQualifiedName().equals("java.lang.String"));
    }
}

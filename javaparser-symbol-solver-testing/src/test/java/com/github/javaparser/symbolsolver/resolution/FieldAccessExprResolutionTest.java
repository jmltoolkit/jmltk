/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests resolution of FieldAccessExpr with same names in scope and identifier
 *
 * @author Takeshi D. Itoh
 */
class FieldAccessExprResolutionTest extends AbstractResolutionTest {

    @BeforeEach
    void configureSymbolSolver() throws IOException {
        // configure symbol solver so as not to potentially disturb tests in other classes
        StaticJavaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
    }

    @Test
    void solveX() throws IOException {
        CompilationUnit cu = parseSample("FieldAccessExprResolution");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "Main");
        MethodDeclaration md = Navigator.demandMethod(clazz, "x");
        MethodCallExpr mce = Navigator.findMethodCall(md, "method").get();
        String actual = mce.resolve().getQualifiedName();
        assertEquals("X.method", actual);
    }

    @Test
    void solveXX() throws IOException {
        CompilationUnit cu = parseSample("FieldAccessExprResolution");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "Main");
        MethodDeclaration md = Navigator.demandMethod(clazz, "x_x");
        MethodCallExpr mce = Navigator.findMethodCall(md, "method").get();
        String actual = mce.resolve().getQualifiedName();
        assertEquals("X.X1.method", actual);
    }

    @Test
    void solveXYX() throws IOException {
        CompilationUnit cu = parseSample("FieldAccessExprResolution");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "Main");
        MethodDeclaration md = Navigator.demandMethod(clazz, "x_y_x");
        MethodCallExpr mce = Navigator.findMethodCall(md, "method").get();
        String actual = mce.resolve().getQualifiedName();
        assertEquals("X.Y1.X2.method", actual);
    }

    @Test
    void solveXYZX() throws IOException {
        CompilationUnit cu = parseSample("FieldAccessExprResolution");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "Main");
        MethodDeclaration md = Navigator.demandMethod(clazz, "x_z_y_x");
        MethodCallExpr mce = Navigator.findMethodCall(md, "method").get();
        String actual = mce.resolve().getQualifiedName();
        assertEquals("X.Z1.Y2.X3.method", actual);
    }
}

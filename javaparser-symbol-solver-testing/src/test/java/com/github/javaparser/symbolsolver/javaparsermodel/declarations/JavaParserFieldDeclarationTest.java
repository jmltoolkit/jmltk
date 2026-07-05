/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarations;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclarationTest;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JavaParserFieldDeclarationTest implements ResolvedFieldDeclarationTest {

    @Test
    void whenTypeSolverIsNullShouldThrowIllegalArgumentException() {
        CompilationUnit compilationUnit = StaticJavaParser.parse("class A {String s;}");
        VariableDeclarator variableDeclarator =
                compilationUnit.findFirst(FieldDeclaration.class).get().getVariable(0);
        assertThrows(IllegalArgumentException.class, () -> new JavaParserFieldDeclaration(variableDeclarator, null));
    }

    @Test
    void verifyIsVolatileVariableDeclarationFromJavaParser() {
        CompilationUnit compilationUnit = StaticJavaParser.parse("class A {volatile int counter = 0;}");
        FieldDeclaration fieldDeclaration =
                compilationUnit.findFirst(FieldDeclaration.class).get();
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        ResolvedFieldDeclaration rfd =
                new JavaParserFieldDeclaration(fieldDeclaration.getVariable(0), reflectionTypeSolver);
        assertTrue(rfd.isVolatile());
    }

    @Test
    void verifyIsNotVolatileVariableDeclarationFromJavaParser() {
        CompilationUnit compilationUnit = StaticJavaParser.parse("class A {int counter = 0;}");
        FieldDeclaration fieldDeclaration =
                compilationUnit.findFirst(FieldDeclaration.class).get();
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        ResolvedFieldDeclaration rfd =
                new JavaParserFieldDeclaration(fieldDeclaration.getVariable(0), reflectionTypeSolver);
        assertFalse(rfd.isVolatile());
    }

    //
    //  Initialize ResolvedFieldDeclarationTest
    //

    private static ResolvedFieldDeclaration createResolvedFieldDeclaration(boolean isStatic) {
        String code = isStatic ? "class A {static String s;}" : "class A {String s;}";
        FieldDeclaration fieldDeclaration =
                StaticJavaParser.parse(code).findFirst(FieldDeclaration.class).get();
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        return new JavaParserFieldDeclaration(fieldDeclaration.getVariable(0), reflectionTypeSolver);
    }

    @Override
    public ResolvedFieldDeclaration createValue() {
        return createResolvedFieldDeclaration(false);
    }

    @Override
    public ResolvedFieldDeclaration createStaticValue() {
        return createResolvedFieldDeclaration(true);
    }

    @Override
    public Optional<Node> getWrappedDeclaration(AssociableToAST associableToAST) {
        return Optional.of(
                safeCast(associableToAST, JavaParserFieldDeclaration.class).getWrappedNode());
    }

    @Override
    public String getCanonicalNameOfExpectedType(ResolvedValueDeclaration resolvedDeclaration) {
        return String.class.getCanonicalName();
    }
}

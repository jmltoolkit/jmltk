/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassOrInterfaceDeclarationContextTest {

    private final TypeSolver typeSolver = new ReflectionTypeSolver();
    private JavaParser javaParser;

    @BeforeEach
    void beforeEach() {
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setSymbolResolver(new JavaSymbolSolver(typeSolver));
        javaParser = new JavaParser();
    }

    @Test
    void testSolveWithoutTypeArguments() {
        CompilationUnit alphaCU = parse("class Alpha { class Foo {} }");
        ClassOrInterfaceDeclaration alpha = Navigator.demandClass(alphaCU, "Alpha");
        ClassOrInterfaceDeclarationContext alphaContext = new ClassOrInterfaceDeclarationContext(alpha, typeSolver);

        assertTrue(alphaContext.solveType("Foo").isSolved());
        assertTrue(alphaContext.solveType("Foo", Collections.emptyList()).isSolved());
        assertFalse(alphaContext
                .solveType("Foo", Collections.singletonList(ResolvedPrimitiveType.INT))
                .isSolved());
    }

    @Test
    void testSolveWithTypeArguments() {
        CompilationUnit betaCU = parse("class Beta { class Foo<T> {} }");
        ClassOrInterfaceDeclaration beta = Navigator.demandClassOrInterface(betaCU, "Beta");
        ClassOrInterfaceDeclarationContext betaContext = new ClassOrInterfaceDeclarationContext(beta, typeSolver);

        assertTrue(betaContext.solveType("Foo").isSolved());
        assertFalse(betaContext.solveType("Foo", Collections.emptyList()).isSolved());
        assertTrue(betaContext
                .solveType("Foo", Collections.singletonList(ResolvedPrimitiveType.INT))
                .isSolved());
    }

    private CompilationUnit parse(String sourceCode) {
        return javaParser.parse(sourceCode).getResult().orElseThrow(AssertionError::new);
    }
}

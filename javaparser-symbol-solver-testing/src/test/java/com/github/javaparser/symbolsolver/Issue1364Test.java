/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Dominik Hardtke
 * @since 02/02/2018
 */
class Issue1364Test extends AbstractResolutionTest {
    private JavaParser javaParser;

    @BeforeEach
    void setup() {
        ClassOrInterfaceDeclaration fakeObject = new ClassOrInterfaceDeclaration();
        fakeObject.setName(new SimpleName("java.lang.Object"));

        TypeSolver typeSolver = new TypeSolver() {
            @Override
            public TypeSolver getParent() {
                return null;
            }

            @Override
            public void setParent(TypeSolver parent) {}

            @Override
            public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
                if ("java.lang.Object".equals(name)) {
                    // custom handling
                    return SymbolReference.solved(new JavaParserClassDeclaration(fakeObject, this));
                }

                return SymbolReference.unsolved();
            }

            @Override
            public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveTypeInModule(
                    String qualifiedModuleName, String simpleTypeName) {
                throw new UnsupportedOperationException();
            }
        };

        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(new JavaSymbolSolver(typeSolver));
        javaParser = new JavaParser(config);
    }

    @Test
    void resolveSubClassOfObject() {
        assertTimeoutPreemptively(Duration.ofMillis(1000L), () -> {
            String code = String.join(
                    System.lineSeparator(),
                    "package graph;",
                    "public class Vertex {",
                    "    public static void main(String[] args) {",
                    "        System.out.println();",
                    "    }",
                    "}");
            ParseResult<CompilationUnit> parseResult =
                    javaParser.parse(ParseStart.COMPILATION_UNIT, Providers.provider(code));
            assertTrue(parseResult.isSuccessful());
            assertTrue(parseResult.getResult().isPresent());
            List<MethodCallExpr> methodCallExprs = parseResult.getResult().get().findAll(MethodCallExpr.class);
            assertEquals(1, methodCallExprs.size());
            try {
                methodCallExprs.get(0).calculateResolvedType();
                fail("An UnsolvedSymbolException should be thrown");
            } catch (UnsolvedSymbolException ignored) {
            }
        });
    }
}

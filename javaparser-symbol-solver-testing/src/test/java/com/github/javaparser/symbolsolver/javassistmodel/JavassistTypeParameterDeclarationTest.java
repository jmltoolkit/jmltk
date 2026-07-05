/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javassistmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JavassistTypeParameterDeclarationTest extends AbstractResolutionTest {

    private TypeSolver typeSolver;

    @BeforeEach
    void setup() throws IOException {
        Path pathToJar = adaptPath("src/test/resources/javaparser-core-3.0.0-alpha.2.jar");
        typeSolver = new CombinedTypeSolver(new JarTypeSolver(pathToJar), new ReflectionTypeSolver());
    }

    @Test
    void testGetBounds() {
        JavassistClassDeclaration compilationUnit =
                (JavassistClassDeclaration) typeSolver.solveType("com.github.javaparser.utils.PositionUtils");
        assertTrue(compilationUnit.isClass());

        for (ResolvedMethodDeclaration method : compilationUnit.getDeclaredMethods()) {
            switch (method.getName()) {
                case "sortByBeginPosition":
                    List<ResolvedTypeParameterDeclaration> typeParams = method.getTypeParameters();
                    assertEquals(1, typeParams.size());
                    assertEquals("T", typeParams.get(0).getName());

                    List<ResolvedTypeParameterDeclaration.Bound> bounds =
                            typeParams.get(0).getBounds();
                    assertEquals(1, bounds.size());
                    assertTrue(bounds.get(0).isExtends());
                    assertEquals(
                            "com.github.javaparser.ast.Node",
                            bounds.get(0).getType().describe());
            }
        }
    }

    @Test
    void testGetComplexBounds() throws IOException {
        Path pathToJar = adaptPath("src/test/resources/javassist_generics/generics.jar");
        typeSolver = new CombinedTypeSolver(new JarTypeSolver(pathToJar), new ReflectionTypeSolver());

        JavassistClassDeclaration compilationUnit =
                (JavassistClassDeclaration) typeSolver.solveType("javaparser.GenericClass");
        assertTrue(compilationUnit.isClass());

        for (ResolvedMethodDeclaration method : compilationUnit.getDeclaredMethods()) {
            if ("complexGenerics".equals(method.getName())) {
                List<ResolvedTypeParameterDeclaration> typeParams = method.getTypeParameters();

                assertEquals(1, typeParams.size());
                assertEquals("T", typeParams.get(0).getName());

                List<ResolvedTypeParameterDeclaration.Bound> bounds =
                        typeParams.get(0).getBounds();
                assertEquals(2, bounds.size());
                assertTrue(bounds.get(0).isExtends());
                assertEquals("java.lang.Object", bounds.get(0).getType().describe());
                assertTrue(bounds.get(1).isExtends());
                assertEquals(
                        "javaparser.GenericClass.Foo<? extends T>",
                        bounds.get(1).getType().describe());
            }
        }
    }
}

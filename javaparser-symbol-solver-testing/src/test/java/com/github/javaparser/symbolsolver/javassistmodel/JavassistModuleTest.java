/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javassistmodel;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.AbstractSymbolResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

class JavassistModuleTest extends AbstractSymbolResolutionTest {

    private TypeSolver typeSolver;

    @BeforeEach
    void setup() throws IOException {
        Path pathToJar = adaptPath("src/test/resources/modules/com-github-javaparser-testmodule.jar");
        typeSolver = new CombinedTypeSolver(new JarTypeSolver(pathToJar), new ReflectionTypeSolver());
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_9)
    void shouldResolveExportedTypeInModule() {
        SymbolReference<ResolvedReferenceTypeDeclaration> solved =
                typeSolver.tryToSolveTypeInModule("com.github.javaparser.testmodule", "TestClass");
        assertTrue(solved.isSolved());
        assertEquals(
                "com.github.javaparser.testpackage.TestClass",
                solved.getCorrespondingDeclaration().getQualifiedName());
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution.typesolvers;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class ClassLoaderTypeSolverTest<T extends ClassLoaderTypeSolver> extends AbstractTypeSolverTest<T> {

    public ClassLoaderTypeSolverTest(Supplier<T> solverSupplier) {
        super(solverSupplier);
    }

    /**
     * When solving a nested type the argument may be a nested class but not in a canonical format.
     * This test checks when name is supplied without the canonical name the solver still resolves.
     */
    @Test
    void solveNonCanonicalNameForNestedClass() {
        String expectedCanonicalName = Map.Entry.class.getCanonicalName();
        String suppliedName = "java.util.Map.Entry";

        T typeSolver = createTypeSolver();
        SymbolReference<ResolvedReferenceTypeDeclaration> solvedType = typeSolver.tryToSolveType(suppliedName);
        assertTrue(solvedType.isSolved());

        ResolvedReferenceTypeDeclaration resolvedDeclaration = solvedType.getCorrespondingDeclaration();
        assertEquals(expectedCanonicalName, resolvedDeclaration.getQualifiedName());
    }
}

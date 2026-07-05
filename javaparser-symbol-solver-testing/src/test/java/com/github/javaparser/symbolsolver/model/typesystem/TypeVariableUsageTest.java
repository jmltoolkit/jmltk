/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.model.typesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TypeVariableUsageTest {

    private ResolvedTypeVariable tpA;
    private ReferenceTypeImpl tpString;
    private TypeSolver typeSolver;

    @BeforeEach
    void setup() {
        tpA = new ResolvedTypeVariable(
                ResolvedTypeParameterDeclaration.onType("A", "foo.Bar", Collections.emptyList()));

        typeSolver = new ReflectionTypeSolver();
        tpString = new ReferenceTypeImpl(new ReflectionClassDeclaration(String.class, typeSolver));
    }

    @Test
    void testIsAssignableBySimple() {
        assertEquals(false, tpString.isAssignableBy(tpA));
    }
}

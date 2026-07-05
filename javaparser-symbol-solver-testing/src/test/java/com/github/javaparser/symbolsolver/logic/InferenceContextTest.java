/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.logic;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.logic.InferenceContext;
import com.github.javaparser.resolution.logic.InferenceVariableType;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionInterfaceDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Federico Tomassetti
 */
class InferenceContextTest {

    private TypeSolver typeSolver;
    private ResolvedReferenceType string;
    private ResolvedReferenceType object;
    private ResolvedReferenceType listOfString;
    private ResolvedReferenceType listOfE;
    private ResolvedTypeParameterDeclaration tpE;

    @BeforeEach
    void setup() {
        typeSolver = new ReflectionTypeSolver();
        string = new ReferenceTypeImpl(new ReflectionClassDeclaration(String.class, typeSolver));
        object = new ReferenceTypeImpl(new ReflectionClassDeclaration(Object.class, typeSolver));
        listOfString = listOf(string);
        tpE = mock(ResolvedTypeParameterDeclaration.class);
        when(tpE.getName()).thenReturn("T");

        listOfE = listOf(new ResolvedTypeVariable(tpE));
    }

    private ResolvedReferenceType listOf(ResolvedType elementType) {
        return new ReferenceTypeImpl(
                new ReflectionInterfaceDeclaration(List.class, typeSolver), ImmutableList.of(elementType));
    }

    @Test
    void noVariablesArePlacedWhenNotNeeded() {
        ResolvedType result = new InferenceContext(typeSolver).addPair(object, string);
        assertEquals(object, result);
    }

    @Test
    void placingASingleVariableTopLevel() {
        ResolvedType result = new InferenceContext(typeSolver).addPair(new ResolvedTypeVariable(tpE), listOfString);
        assertEquals(new InferenceVariableType(0, typeSolver), result);
    }

    @Test
    void placingASingleVariableInside() {
        ResolvedType result = new InferenceContext(typeSolver).addPair(listOfE, listOfString);
        assertEquals(listOf(new InferenceVariableType(0, typeSolver)), result);
    }
}

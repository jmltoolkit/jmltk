/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.model.typesystem;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.model.typesystem.NullType;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.resolution.types.ResolvedVoidType;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionInterfaceDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VoidTypeTest {

    private ResolvedArrayType arrayOfBooleans;
    private ResolvedArrayType arrayOfListOfA;
    private ReferenceTypeImpl OBJECT;
    private ReferenceTypeImpl STRING;
    private TypeSolver typeSolver;

    @BeforeEach
    void setup() {
        typeSolver = new ReflectionTypeSolver();
        OBJECT = new ReferenceTypeImpl(new ReflectionClassDeclaration(Object.class, typeSolver));
        STRING = new ReferenceTypeImpl(new ReflectionClassDeclaration(String.class, typeSolver));
        arrayOfBooleans = new ResolvedArrayType(ResolvedPrimitiveType.BOOLEAN);
        arrayOfListOfA = new ResolvedArrayType(new ReferenceTypeImpl(
                new ReflectionInterfaceDeclaration(List.class, typeSolver),
                ImmutableList.of(new ResolvedTypeVariable(
                        ResolvedTypeParameterDeclaration.onType("A", "foo.Bar", Collections.emptyList())))));
    }

    @Test
    void testIsArray() {
        assertEquals(false, ResolvedVoidType.INSTANCE.isArray());
    }

    @Test
    void testIsPrimitive() {
        assertEquals(false, ResolvedVoidType.INSTANCE.isPrimitive());
    }

    @Test
    void testIsNull() {
        assertEquals(false, ResolvedVoidType.INSTANCE.isNull());
    }

    @Test
    void testIsReference() {
        assertEquals(false, ResolvedVoidType.INSTANCE.isReference());
    }

    @Test
    void testIsReferenceType() {
        assertEquals(false, ResolvedVoidType.INSTANCE.isReferenceType());
    }

    @Test
    void testIsVoid() {
        assertEquals(true, ResolvedVoidType.INSTANCE.isVoid());
    }

    @Test
    void testIsTypeVariable() {
        assertEquals(false, ResolvedVoidType.INSTANCE.isTypeVariable());
    }

    @Test
    void testAsReferenceTypeUsage() {
        assertThrows(UnsupportedOperationException.class, () -> ResolvedVoidType.INSTANCE.asReferenceType());
    }

    @Test
    void testAsTypeParameter() {
        assertThrows(UnsupportedOperationException.class, () -> ResolvedVoidType.INSTANCE.asTypeParameter());
    }

    @Test
    void testAsArrayTypeUsage() {
        assertThrows(UnsupportedOperationException.class, () -> ResolvedVoidType.INSTANCE.asArrayType());
    }

    @Test
    void testAsDescribe() {
        assertEquals("void", ResolvedVoidType.INSTANCE.describe());
    }

    @Test
    void testIsAssignableBy() {
        assertFalse(ResolvedVoidType.INSTANCE.isAssignableBy(NullType.INSTANCE));
        assertFalse(ResolvedVoidType.INSTANCE.isAssignableBy(OBJECT));
        assertFalse(ResolvedVoidType.INSTANCE.isAssignableBy(STRING));
        assertFalse(ResolvedVoidType.INSTANCE.isAssignableBy(ResolvedPrimitiveType.BOOLEAN));
        assertFalse(ResolvedVoidType.INSTANCE.isAssignableBy(ResolvedVoidType.INSTANCE));
    }
}

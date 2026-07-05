/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public interface ResolvedTypeDeclarationTest extends ResolvedDeclarationTest {

    @Override
    ResolvedTypeDeclaration createValue();

    @Test
    default void whenDeclarationIsAClassTheCallToTheMethodAsClassShouldNotThrow() {
        ResolvedTypeDeclaration resolvedDeclaration = createValue();
        if (resolvedDeclaration.isClass()) assertDoesNotThrow(resolvedDeclaration::asClass);
        else assertThrows(UnsupportedOperationException.class, resolvedDeclaration::asClass);
    }

    @Test
    default void whenDeclarationIsAInterfaceTheCallToTheMethodAsInterfaceShouldNotThrow() {
        ResolvedTypeDeclaration resolvedDeclaration = createValue();
        if (resolvedDeclaration.isInterface()) assertDoesNotThrow(resolvedDeclaration::asInterface);
        else assertThrows(UnsupportedOperationException.class, resolvedDeclaration::asInterface);
    }

    @Test
    default void whenDeclarationIsAEnumTheCallToTheMethodAsEnumShouldNotThrow() {
        ResolvedTypeDeclaration resolvedDeclaration = createValue();
        if (resolvedDeclaration.isEnum()) assertDoesNotThrow(resolvedDeclaration::asEnum);
        else assertThrows(UnsupportedOperationException.class, resolvedDeclaration::asEnum);
    }

    @Test
    default void whenDeclarationIsATypeParameterTheCallToTheMethodAsTypeParameterShouldNotThrow() {
        ResolvedTypeDeclaration resolvedDeclaration = createValue();
        if (resolvedDeclaration.isTypeParameter()) assertDoesNotThrow(resolvedDeclaration::asTypeParameter);
        else assertThrows(UnsupportedOperationException.class, resolvedDeclaration::asTypeParameter);
    }

    @Test
    default void whenDeclarationIsAReferenceTypeTheCallToTheMethodAsReferenceTypeShouldNotThrow() {
        ResolvedTypeDeclaration resolvedDeclaration = createValue();
        if (resolvedDeclaration.isReferenceType()) assertDoesNotThrow(resolvedDeclaration::asReferenceType);
        else assertThrows(UnsupportedOperationException.class, resolvedDeclaration::asReferenceType);
    }

    @Test
    default void qualifiedNameCantBeNull() {
        assertNotNull(createValue().getQualifiedName());
    }

    @Test
    default void getIdCantBeNull() {
        assertNotNull(createValue().getId());
    }

    @Test
    default void containerTypeCantBeNull() {
        assertNotNull(createValue().containerType());
    }
}

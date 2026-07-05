/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public interface ResolvedClassDeclarationTest
        extends ResolvedReferenceTypeDeclarationTest, ResolvedTypeParametrizableTest, HasAccessSpecifierTest {

    @Override
    ResolvedClassDeclaration createValue();

    @Test
    default void resolvedClassShouldBeMarkedAsClass() {
        assertTrue(createValue().isClass());
    }

    @Test
    default void getSuperClassShouldNotBeNull() {
        assertNotNull(createValue().getSuperClass());
    }

    @Test
    default void getInterfacesShouldNotBeNull() {
        assertNotNull(createValue().getInterfaces());
    }

    @Test
    default void getAllSuperClasses() {
        assertNotNull(createValue().getAllSuperClasses());
    }

    @Test
    default void getAllInterfaces() {
        assertNotNull(createValue().getAllInterfaces());
    }
}

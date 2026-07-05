/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public interface ResolvedFieldDeclarationTest extends ResolvedValueDeclarationTest, HasAccessSpecifierTest {

    /**
     * Create a new non-static {@link ResolvedFieldDeclaration}.
     *
     * @return The non-static value.
     */
    @Override
    ResolvedFieldDeclaration createValue();

    /**
     * Create a new static {@link ResolvedFieldDeclaration}.
     *
     * @return The static value.
     */
    ResolvedFieldDeclaration createStaticValue();

    @Test
    default void whenAFieldIsStaticShouldBeMarkedAsSuch() {
        assertFalse(createValue().isStatic());
        assertTrue(createStaticValue().isStatic());
    }

    @Test
    default void theDeclaringTypeCantBeNull() {
        assertNotNull(createValue().declaringType());
    }
}

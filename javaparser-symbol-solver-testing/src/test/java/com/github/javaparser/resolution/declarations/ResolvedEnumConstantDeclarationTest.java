/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public interface ResolvedEnumConstantDeclarationTest extends ResolvedValueDeclarationTest {

    @Override
    ResolvedEnumConstantDeclaration createValue();

    @Test
    default void enumConstantShouldBeMarkedAsEnum() {
        assertTrue(createValue().isEnumConstant());
    }

    @Test
    default void enumNameShouldNotBeNull() {
        assertNotNull(createValue().getName());
    }
}

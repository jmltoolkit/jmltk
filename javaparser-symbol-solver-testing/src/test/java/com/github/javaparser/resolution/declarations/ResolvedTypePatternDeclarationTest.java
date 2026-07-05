/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public interface ResolvedTypePatternDeclarationTest extends ResolvedValueDeclarationTest {

    @Override
    ResolvedTypePatternDeclaration createValue();

    @Test
    default void resolvedPatternShouldBeMarkedAsPattern() {
        assertTrue(createValue().isTypePattern());
    }

    @Test
    default void resolvedPatternShouldHaveAName() {
        assertTrue(createValue().hasName());
    }
}

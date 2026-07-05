/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public interface ResolvedTypeParametrizableTest {

    ResolvedTypeParametrizable createValue();

    @Test
    default void getTypeParametersCantBeNull() {
        assertNotNull(createValue().getTypeParameters());
    }

    // TODO: Implement the missing check
    @Disabled(value = "JavaParserTypeVariable is not throwing yet.")
    @Test
    default void findTypeParameterShouldThrowIllegalArgumentExceptionWhenNullIsProvided() {
        ResolvedTypeParametrizable typeParametrizable = createValue();
        assertThrows(IllegalArgumentException.class, () -> typeParametrizable.findTypeParameter(null));
    }
}

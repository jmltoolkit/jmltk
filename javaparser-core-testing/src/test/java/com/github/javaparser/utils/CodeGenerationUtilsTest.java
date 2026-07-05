/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.utils;

import org.junit.jupiter.api.Test;

import static com.github.javaparser.utils.CodeGenerationUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodeGenerationUtilsTest {
    @Test
    void setters() {
        assertEquals("setValue", setterName("value"));
        assertEquals("setBlue", setterName("isBlue"));
    }

    @Test
    void getters() {
        assertEquals("getValue", getterName(Object.class, "value"));
        assertEquals("isBlue", getterName(boolean.class, "isBlue"));
        assertEquals("isBlue", getterName(boolean.class, "blue"));
        assertEquals("getBlue", getterName(Boolean.class, "blue"));
        assertEquals("getIsBlue", getterName(Boolean.class, "isBlue"));
    }

    @Test
    void testGetterToPropertyName() {
        assertEquals("value", getterToPropertyName("getValue"));
        assertEquals("blue", getterToPropertyName("isBlue"));
        assertEquals("value", getterToPropertyName("hasValue"));

        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> getterToPropertyName("value"));
        assertEquals("Unexpected getterName 'value'", thrown.getMessage());
    }
}

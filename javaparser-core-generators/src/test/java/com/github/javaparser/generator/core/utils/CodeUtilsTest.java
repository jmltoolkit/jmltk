/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.generator.core.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.generator.core.utils.CodeUtils.castValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeUtilsTest {

    private static final String RETURN_VALUE = "this";

    @Test
    void castReturnValue_whenAValueMatchesTheExpectedTypeNoCastIsNeeded() {
        Type returnType = PrimitiveType.booleanType();
        Type valueType = PrimitiveType.booleanType();

        assertEquals(RETURN_VALUE, castValue(RETURN_VALUE, returnType, valueType.asString()));
    }

    @Test
    void castReturnValue_whenAValueIsNotAssignedByReturnShouldBeCasted() {
        Type returnType = StaticJavaParser.parseType("String");
        Type valueType = StaticJavaParser.parseType("Object");

        assertEquals(
                String.format("(%s) %s", returnType, RETURN_VALUE),
                castValue(RETURN_VALUE, returnType, valueType.asString()));
    }
}

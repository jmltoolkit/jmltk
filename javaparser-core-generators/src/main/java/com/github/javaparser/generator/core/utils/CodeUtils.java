/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.generator.core.utils;

import com.github.javaparser.ast.type.Type;

public final class CodeUtils {

    private CodeUtils() {
        // This constructor is used to hide the public one
    }

    /**
     * Cast the value if the current type doesn't match the required type.
     * <br>
     * Given the following example:
     * <code>
     *     int withoutCast = 1;
     *     double withCast = (double) 1;
     * </code>
     * The variable withoutCast doesn't need to be casted, since we have int as required type and int as value type.
     * While in the variable withCast we have double as required type and int as value type.
     *
     * @param value           The value to be returned.
     * @param requiredType    The expected type to be casted if needed.
     * @param valueType       The type of the value to be returned.
     *
     * @return The value casted if needed.
     */
    public static String castValue(String value, Type requiredType, String valueType) {
        String requiredTypeName = requiredType.asString();

        if (requiredTypeName.equals(valueType)) return value;
        return String.format("(%s) %s", requiredTypeName, value);
    }
}

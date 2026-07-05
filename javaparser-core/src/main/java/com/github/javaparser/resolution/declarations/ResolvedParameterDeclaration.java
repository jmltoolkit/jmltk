/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

/**
 * Declaration of a parameter.
 *
 * @author Federico Tomassetti
 */
public interface ResolvedParameterDeclaration extends ResolvedValueDeclaration {

    @Override
    default boolean isParameter() {
        return true;
    }

    /**
     * Necessary because parameters obtained through reflection could not have a name.
     */
    default boolean hasName() {
        return true;
    }

    @Override
    default ResolvedParameterDeclaration asParameter() {
        return this;
    }

    /**
     * Is this parameter declared as variadic?
     */
    boolean isVariadic();

    /**
     * Describe the type of the parameter. In practice add three dots to the type name
     * if the parameter is variadic.
     */
    default String describeType() {
        if (isVariadic()) {
            return getType().asArrayType().getComponentType().describe() + "...";
        }
        return getType().describe();
    }
}

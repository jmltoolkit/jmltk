/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

/**
 * Declaration of a field.
 *
 * @author Federico Tomassetti
 */
public interface ResolvedFieldDeclaration extends ResolvedValueDeclaration, HasAccessSpecifier {

    /**
     * Is the field static?
     */
    boolean isStatic();

    /**
     * Is the field volatile?
     */
    boolean isVolatile();

    @Override
    default boolean isField() {
        return true;
    }

    @Override
    default ResolvedFieldDeclaration asField() {
        return this;
    }

    /**
     * The type on which this field has been declared
     */
    ResolvedTypeDeclaration declaringType();
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import java.util.List;

/**
 * Declaration of an Enum.
 *
 * @author Federico Tomassetti
 */
public interface ResolvedEnumDeclaration extends ResolvedReferenceTypeDeclaration, HasAccessSpecifier {

    @Override
    default boolean isEnum() {
        return true;
    }

    @Override
    default ResolvedEnumDeclaration asEnum() {
        return this;
    }

    List<ResolvedEnumConstantDeclaration> getEnumConstants();

    default boolean hasEnumConstant(String name) {
        return getEnumConstants().stream().anyMatch(c -> c.getName().equals(name));
    }

    default ResolvedEnumConstantDeclaration getEnumConstant(final String name) {
        return getEnumConstants().stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow(() -> new IllegalArgumentException("No constant named " + name));
    }
}

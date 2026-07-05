/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import java.util.List;
import java.util.Optional;

/**
 * An entity which has type parameter.
 *
 * @author Federico Tomassetti
 */
public interface ResolvedTypeParametrizable {

    /**
     * The list of type parameters defined on this element.
     */
    List<ResolvedTypeParameterDeclaration> getTypeParameters();

    /**
     * Find the closest TypeParameterDeclaration with the given name.
     * It first look on this element itself and then on the containers.
     */
    Optional<ResolvedTypeParameterDeclaration> findTypeParameter(String name);

    default boolean isGeneric() {
        return !getTypeParameters().isEmpty();
    }
}

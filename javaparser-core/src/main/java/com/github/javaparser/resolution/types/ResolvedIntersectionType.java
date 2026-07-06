/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.types;

import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An intersection type is defined in java as list of types separates by ampersands.
 *
 * @author Federico Tomassetti
 */
public class ResolvedIntersectionType implements ResolvedType {

    private List<ResolvedType> elements;

    public ResolvedIntersectionType(Collection<ResolvedType> elements) {
        if (elements.size() < 2) {
            throw new IllegalArgumentException(
                    "An intersection type should have at least two elements. This has " + elements.size());
        }
        this.elements = new LinkedList<>(elements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolvedIntersectionType that = (ResolvedIntersectionType) o;
        return new HashSet<>(elements).equals(new HashSet<>(that.elements));
    }

    @Override
    public int hashCode() {
        return new HashSet<>(elements).hashCode();
    }

    @Override
    public String describe() {
        return String.join(" & ", elements.stream().map(ResolvedType::describe).collect(Collectors.toList()));
    }

    @Override
    public boolean isAssignableBy(ResolvedType other) {
        return elements.stream().allMatch(e -> e.isAssignableBy(other));
    }

    @Override
    public ResolvedType replaceTypeVariables(
            ResolvedTypeParameterDeclaration tp,
            ResolvedType replaced,
            Map<ResolvedTypeParameterDeclaration, ResolvedType> inferredTypes) {
        List<ResolvedType> elementsReplaced = elements.stream()
                .map(e -> e.replaceTypeVariables(tp, replaced, inferredTypes))
                .collect(Collectors.toList());
        if (elementsReplaced.equals(elements)) {
            return this;
        }
        return new ResolvedIntersectionType(elementsReplaced);
    }

    /*
     * Returns the list of the resolved types
     */
    public List<ResolvedType> getElements() {
        return elements;
    }
}

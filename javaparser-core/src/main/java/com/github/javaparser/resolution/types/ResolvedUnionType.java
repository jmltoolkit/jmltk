/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.types;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A union type is defined in java as list of types separates by pipes.
 *
 * @author Federico Tomassetti
 */
public class ResolvedUnionType implements ResolvedType {

    private List<ResolvedType> elements;

    public ResolvedUnionType(List<ResolvedType> elements) {
        if (elements.size() < 2) {
            throw new IllegalArgumentException(
                    "An union type should have at least two elements. This has " + elements.size());
        }
        this.elements = new LinkedList<>(elements);
    }

    public Optional<ResolvedReferenceType> getCommonAncestor() {
        Optional<List<ResolvedReferenceType>> reduce = elements.stream()
                .map(ResolvedType::asReferenceType)
                .map(rt -> rt.getAllAncestors(ResolvedReferenceTypeDeclaration.breadthFirstFunc))
                .reduce((a, b) -> {
                    ArrayList<ResolvedReferenceType> common = new ArrayList<>(a);
                    common.retainAll(b);
                    return common;
                });
        return reduce.orElse(new ArrayList<>()).stream().findFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolvedUnionType that = (ResolvedUnionType) o;
        return new HashSet<>(elements).equals(new HashSet<>(that.elements));
    }

    @Override
    public int hashCode() {
        return new HashSet<>(elements).hashCode();
    }

    @Override
    public String describe() {
        return String.join(" | ", elements.stream().map(ResolvedType::describe).collect(Collectors.toList()));
    }

    @Override
    public boolean isAssignableBy(ResolvedType other) {
        return elements.stream().allMatch(e -> e.isAssignableBy(other));
    }

    @Override
    public boolean isUnionType() {
        return true;
    }

    @Override
    public ResolvedUnionType asUnionType() {
        return this;
    }

    /*
     * Returns the list of the resolved types
     */
    public List<ResolvedType> getElements() {
        return elements;
    }
}

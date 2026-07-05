/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution.typeinference;

import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Federico Tomassetti
 */
public class Substitution {

    private List<ResolvedTypeParameterDeclaration> typeParameterDeclarations;
    private List<ResolvedType> types;

    private static final Substitution EMPTY = new Substitution();

    public static Substitution empty() {
        return EMPTY;
    }

    public Substitution withPair(ResolvedTypeParameterDeclaration typeParameterDeclaration, ResolvedType type) {
        Substitution newInstance = new Substitution();
        newInstance.typeParameterDeclarations.addAll(this.typeParameterDeclarations);
        newInstance.types.addAll(this.types);
        newInstance.typeParameterDeclarations.add(typeParameterDeclaration);
        newInstance.types.add(type);
        return newInstance;
    }

    private Substitution() {
        this.typeParameterDeclarations = new LinkedList<>();
        this.types = new LinkedList<>();
    }

    public ResolvedType apply(ResolvedType originalType) {
        ResolvedType result = originalType;
        for (int i = 0; i < typeParameterDeclarations.size(); i++) {
            result = result.replaceTypeVariables(typeParameterDeclarations.get(i), types.get(i));
        }
        return result;
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution.typesolvers;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A TypeSolver which only consider the TypeDeclarations provided to it.
 *
 * @author Federico Tomassetti
 */
public class MemoryTypeSolver implements TypeSolver {

    private TypeSolver parent;
    private Map<String, ResolvedReferenceTypeDeclaration> declarationMap = new HashMap<>();

    @Override
    public String toString() {
        return "MemoryTypeSolver{" + "parent=" + parent + ", declarationMap=" + declarationMap + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemoryTypeSolver)) return false;

        MemoryTypeSolver that = (MemoryTypeSolver) o;

        if (parent != null ? !parent.equals(that.parent) : that.parent != null) return false;
        return !(declarationMap != null ? !declarationMap.equals(that.declarationMap) : that.declarationMap != null);
    }

    @Override
    public int hashCode() {
        int result = parent != null ? parent.hashCode() : 0;
        result = 31 * result + (declarationMap != null ? declarationMap.hashCode() : 0);
        return result;
    }

    @Override
    public TypeSolver getParent() {
        return parent;
    }

    @Override
    public void setParent(TypeSolver parent) {
        Objects.requireNonNull(parent);
        if (this.parent != null) {
            throw new IllegalStateException("This TypeSolver already has a parent.");
        }
        if (parent == this) {
            throw new IllegalStateException("The parent of this TypeSolver cannot be itself.");
        }
        this.parent = parent;
    }

    public void addDeclaration(String name, ResolvedReferenceTypeDeclaration typeDeclaration) {
        this.declarationMap.put(name, typeDeclaration);
    }

    @Override
    public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
        if (declarationMap.containsKey(name)) {
            return SymbolReference.solved(declarationMap.get(name));
        }
        return SymbolReference.unsolved();
    }

    @Override
    public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveTypeInModule(
            String qualifiedModuleName, String simpleTypeName) {
        throw new UnsupportedOperationException("Resolving types in modules not yet supported by MemoryTypeSolver");
    }
}

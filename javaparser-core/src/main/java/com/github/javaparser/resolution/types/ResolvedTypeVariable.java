/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.types;

import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;

import java.util.List;
import java.util.Map;

/**
 * From JLS 4.4: A type variable is introduced by the declaration of a type parameter of a generic class,
 * interface, method, or constructor (§8.1.2, §9.1.2, §8.4.4, §8.8.4).
 *
 * @author Federico Tomassetti
 */
public class ResolvedTypeVariable implements ResolvedType {

    private ResolvedTypeParameterDeclaration typeParameter;

    public ResolvedTypeVariable(ResolvedTypeParameterDeclaration typeParameter) {
        this.typeParameter = typeParameter;
    }

    @Override
    public String toString() {
        return "TypeVariable {" + typeParameter.toString() + "}";
    }

    public String qualifiedName() {
        return this.typeParameter.getQualifiedName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolvedTypeVariable that = (ResolvedTypeVariable) o;
        if (!typeParameter.getName().equals(that.typeParameter.getName())) return false;
        if (typeParameter.declaredOnType() != that.typeParameter.declaredOnType()) return false;
        if (typeParameter.declaredOnMethod() != that.typeParameter.declaredOnMethod()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return typeParameter.hashCode();
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public ResolvedType replaceTypeVariables(
            ResolvedTypeParameterDeclaration tpToBeReplaced,
            ResolvedType replaced,
            Map<ResolvedTypeParameterDeclaration, ResolvedType> inferredTypes) {
        if (tpToBeReplaced.getName().equals(this.typeParameter.getName())) {
            inferredTypes.put(this.asTypeParameter(), replaced);
            return replaced;
        }
        return this;
    }

    @Override
    public boolean isReferenceType() {
        return false;
    }

    @Override
    public String describe() {
        return typeParameter.getName();
    }

    @Override
    public ResolvedTypeParameterDeclaration asTypeParameter() {
        return typeParameter;
    }

    @Override
    public ResolvedTypeVariable asTypeVariable() {
        return this;
    }

    @Override
    public boolean isTypeVariable() {
        return true;
    }

    @Override
    public boolean isAssignableBy(ResolvedType other) {
        if (other.isTypeVariable()) {
            // if we want to compare something like @{code C extends Comparable<C>} with @{code K extends Comparable<K>}
            // we have to compare the type of the bound. For the moment we are focusing solely on the first type.
            if (typeParameter.hasBound()
                    && other.asTypeVariable().asTypeParameter().hasBound()) {
                return typeParameter
                        .getBounds()
                        .get(0)
                        .getType()
                        .isAssignableBy(other.asTypeVariable()
                                .asTypeParameter()
                                .getBounds()
                                .get(0)
                                .getType());
            }
            return describe().equals(other.describe());
        }
        return true;
    }

    @Override
    public boolean mention(List<ResolvedTypeParameterDeclaration> typeParameters) {
        return typeParameters.contains(typeParameter);
    }

    // /
    // / Erasure
    // /
    // The erasure of a type variable (§4.4) is the erasure of its leftmost bound.
    // If no bound is declared for a type variable, Object is assumed.
    //
    @Override
    public ResolvedType erasure() {
        if (typeParameter.isBounded()) {
            return typeParameter.getBounds().get(0).getType();
        }
        return typeParameter.object();
    }

    /*
     * Returns the resolved type for a type variable.
     */
    @Override
    public ResolvedType solveGenericTypes(Context context) {
        return context.solveGenericType(describe()).orElse(this);
    }

    @Override
    public String toDescriptor() {
        return String.format("L%s;", qualifiedName());
    }
}

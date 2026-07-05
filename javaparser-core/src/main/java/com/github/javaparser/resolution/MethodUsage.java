/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution;

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.parametrization.ResolvedTypeParametersMap;
import com.github.javaparser.resolution.types.parametrization.ResolvedTypeParametrized;
import java.util.*;

/**
 * This is basically a MethodDeclaration with some TypeParameters defined.
 * The defined TypeParameters can comes from the Method itself or from the surrounding types.
 *
 * @author Federico Tomassetti
 */
public class MethodUsage implements ResolvedTypeParametrized {

    private ResolvedMethodDeclaration declaration;

    private List<ResolvedType> paramTypes = new ArrayList<>();

    private List<ResolvedType> exceptionTypes = new ArrayList<>();

    private ResolvedType returnType;

    private ResolvedTypeParametersMap typeParametersMap;

    public MethodUsage(ResolvedMethodDeclaration declaration) {
        this.typeParametersMap = ResolvedTypeParametersMap.empty();
        this.declaration = declaration;
        paramTypes.addAll(declaration.formalParameterTypes());
        exceptionTypes.addAll(declaration.getSpecifiedExceptions());
        returnType = declaration.getReturnType();
    }

    public MethodUsage(ResolvedMethodDeclaration declaration, List<ResolvedType> paramTypes, ResolvedType returnType) {
        this(
                declaration,
                paramTypes,
                returnType,
                declaration.getSpecifiedExceptions(),
                ResolvedTypeParametersMap.empty());
    }

    public MethodUsage(
            ResolvedMethodDeclaration declaration,
            List<ResolvedType> paramTypes,
            ResolvedType returnType,
            List<ResolvedType> exceptionTypes) {
        this(declaration, paramTypes, returnType, exceptionTypes, ResolvedTypeParametersMap.empty());
    }

    private MethodUsage(
            ResolvedMethodDeclaration declaration,
            List<ResolvedType> paramTypes,
            ResolvedType returnType,
            List<ResolvedType> exceptionTypes,
            ResolvedTypeParametersMap typeParametersMap) {
        this.declaration = declaration;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
        this.exceptionTypes = exceptionTypes;
        this.typeParametersMap = typeParametersMap;
    }

    @Override
    public String toString() {
        return "MethodUsage{" + "declaration=" + declaration + ", paramTypes=" + paramTypes + '}';
    }

    public ResolvedMethodDeclaration getDeclaration() {
        return declaration;
    }

    public String getName() {
        return declaration.getName();
    }

    public ResolvedReferenceTypeDeclaration declaringType() {
        return declaration.declaringType();
    }

    public ResolvedType returnType() {
        return returnType;
    }

    public List<ResolvedType> getParamTypes() {
        return paramTypes;
    }

    public MethodUsage replaceParamType(int i, ResolvedType replaced) {
        if (i < 0 || i >= getNoParams()) {
            throw new IllegalArgumentException();
        }
        if (paramTypes.get(i) == replaced) {
            return this;
        }
        List<ResolvedType> newParams = new LinkedList<>(paramTypes);
        newParams.set(i, replaced);
        return new MethodUsage(declaration, newParams, returnType, exceptionTypes, typeParametersMap);
    }

    public MethodUsage replaceExceptionType(int i, ResolvedType replaced) {
        if (i < 0 || i >= exceptionTypes.size()) {
            throw new IllegalArgumentException();
        }
        if (exceptionTypes.get(i) == replaced) {
            return this;
        }
        List<ResolvedType> newTypes = new LinkedList<>(exceptionTypes);
        newTypes.set(i, replaced);
        return new MethodUsage(declaration, paramTypes, returnType, newTypes, typeParametersMap);
    }

    public MethodUsage replaceReturnType(ResolvedType returnType) {
        if (returnType == this.returnType) {
            return this;
        }
        return new MethodUsage(declaration, paramTypes, returnType, exceptionTypes, typeParametersMap);
    }

    /**
     * Return the number of formal arguments accepted by this method.
     */
    public int getNoParams() {
        return paramTypes.size();
    }

    /**
     * Return the type of the formal argument at the given position.
     */
    public ResolvedType getParamType(int i) {
        return paramTypes.get(i);
    }

    public MethodUsage replaceTypeParameter(ResolvedTypeParameterDeclaration typeParameter, ResolvedType type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        // TODO if the method declaration has a type param with that name ignore this call
        MethodUsage res = new MethodUsage(
                declaration,
                paramTypes,
                returnType,
                exceptionTypes,
                typeParametersMap.toBuilder().setValue(typeParameter, type).build());
        Map<ResolvedTypeParameterDeclaration, ResolvedType> inferredTypes = new HashMap<>();
        for (int i = 0; i < paramTypes.size(); i++) {
            ResolvedType originalParamType = paramTypes.get(i);
            ResolvedType newParamType = originalParamType.replaceTypeVariables(typeParameter, type, inferredTypes);
            res = res.replaceParamType(i, newParamType);
        }
        for (int i = 0; i < exceptionTypes.size(); i++) {
            ResolvedType originalType = exceptionTypes.get(i);
            ResolvedType newType = originalType.replaceTypeVariables(typeParameter, type, inferredTypes);
            res = res.replaceExceptionType(i, newType);
        }
        ResolvedType oldReturnType = res.returnType;
        ResolvedType newReturnType = oldReturnType.replaceTypeVariables(typeParameter, type, inferredTypes);
        res = res.replaceReturnType(newReturnType);
        return res;
    }

    @Override
    public ResolvedTypeParametersMap typeParametersMap() {
        return typeParametersMap;
    }

    /**
     * The qualified signature of the method. It is composed by the qualified name of the declaring type
     * followed by the signature of the method.
     */
    public String getQualifiedSignature() {
        return getDeclaration().declaringType().getQualifiedName() + "." + getSignature();
    }

    /**
     * The signature of the method.
     */
    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append("(");
        for (int i = 0; i < getNoParams(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            ResolvedType type = getParamType(i);
            if (type.isArray() && getDeclaration().getParam(i).isVariadic()) {
                sb.append(type.asArrayType().getComponentType().describe()).append("...");
            } else {
                sb.append(type.describe());
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * The erased signature of the method.
     */
    public String getErasedSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append("(");
        for (int i = 0; i < getNoParams(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            ResolvedType type = getParamType(i).erasure();
            if (type.isArray() && getDeclaration().getParam(i).isVariadic()) {
                sb.append(type.asArrayType().getComponentType().describe()).append("...");
            } else {
                sb.append(type.describe());
            }
        }
        sb.append(")");
        return sb.toString();
    }

    public List<ResolvedType> exceptionTypes() {
        return exceptionTypes;
    }

    /*
     * Two methods or constructors, M and N, have the same signature if they have
     * the same name, the same type parameters (if any) (§8.4.4), and, after
     * adapting the formal parameter types of N to the the type parameters of M, the
     * same formal parameter types.
     * https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html
     * This method returns an approximation of this rule.
     */
    public boolean isSameSignature(MethodUsage otherMethodUsage) {
        return getSignature().equals(otherMethodUsage.getSignature());
    }

    /*
     * The signature of a method m1 is a subsignature of the signature of a method m2 if either:
     * m2 has the same signature as m1, or
     * the signature of m1 is the same as the erasure (§4.6) of the signature of m2.
     */
    public boolean isSubSignature(MethodUsage otherMethodUsage) {
        return getErasedSignature().equals(otherMethodUsage.getErasedSignature());
    }

    /*
     * A method declaration d1 with return type R1 is return-type-substitutable for another method d2 with return type R2 iff any of the following is true:
     * If R1 is void then R2 is void.
     * If R1 is a primitive type then R2 is identical to R1.
     * If R1 is a reference type then one of the following is true:
     * R1, adapted to the type parameters of d2 (§8.4.4), is a subtype of R2.
     * R1 can be converted to a subtype of R2 by unchecked conversion (§5.1.9).
     * d1 does not have the same signature as d2 (§8.4.2), and R1 = |R2|.
     */
    public boolean isReturnTypeSubstituable(MethodUsage otherMethodUsage) {
        return getDeclaration()
                .isReturnTypeSubstituable(otherMethodUsage.getDeclaration().getReturnType());
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.reflectionmodel;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.Objects;

/**
 * @author Federico Tomassetti
 */
public class ReflectionParameterDeclaration implements ResolvedParameterDeclaration {
    private Class<?> type;
    private java.lang.reflect.Type genericType;
    private TypeSolver typeSolver;
    private boolean variadic;
    private String name;

    /**
     *
     * @param type
     * @param genericType
     * @param typeSolver
     * @param variadic
     * @param name can potentially be null
     */
    public ReflectionParameterDeclaration(
            Class<?> type, java.lang.reflect.Type genericType, TypeSolver typeSolver, boolean variadic, String name) {
        this.type = type;
        this.genericType = genericType;
        this.typeSolver = typeSolver;
        this.variadic = variadic;
        this.name = name;
    }

    /**
     *
     * @return the name, which can be potentially null
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean hasName() {
        return name != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "type=" + type + ", name=" + name + '}';
    }

    @Override
    public boolean isField() {
        return false;
    }

    @Override
    public boolean isParameter() {
        return true;
    }

    @Override
    public boolean isVariadic() {
        return variadic;
    }

    @Override
    public boolean isType() {
        return false;
    }

    @Override
    public ResolvedType getType() {
        return ReflectionFactory.typeUsageFor(genericType, typeSolver);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReflectionParameterDeclaration that = (ReflectionParameterDeclaration) o;
        return variadic == that.variadic
                && Objects.equals(type, that.type)
                && Objects.equals(genericType, that.genericType)
                && Objects.equals(typeSolver, that.typeSolver)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, genericType, typeSolver, variadic, name);
    }
}

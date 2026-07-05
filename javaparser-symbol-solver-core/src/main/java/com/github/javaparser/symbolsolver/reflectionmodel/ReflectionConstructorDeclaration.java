/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.reflectionmodel;

import static com.github.javaparser.symbolsolver.logic.AbstractTypeDeclaration.isRecordType;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedType;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Fred Lefévère-Laoide
 */
public class ReflectionConstructorDeclaration implements ResolvedConstructorDeclaration {

    private Constructor<?> constructor;
    private TypeSolver typeSolver;

    public ReflectionConstructorDeclaration(Constructor<?> constructor, TypeSolver typeSolver) {
        this.constructor = constructor;
        this.typeSolver = typeSolver;
    }

    @Override
    public ResolvedReferenceTypeDeclaration declaringType() {
        if (isRecordType(constructor.getDeclaringClass())) {
            return new ReflectionRecordDeclaration(constructor.getDeclaringClass(), typeSolver);
        } else {
            return new ReflectionClassDeclaration(constructor.getDeclaringClass(), typeSolver);
        }
    }

    @Override
    public int getNumberOfParams() {
        return constructor.getParameterCount();
    }

    @Override
    public ResolvedParameterDeclaration getParam(int i) {
        if (i < 0 || i >= getNumberOfParams()) {
            throw new IllegalArgumentException(
                    String.format("No param with index %d. Number of params: %d", i, getNumberOfParams()));
        }
        boolean variadic = false;
        if (constructor.isVarArgs()) {
            variadic = i == (constructor.getParameterCount() - 1);
        }
        return new ReflectionParameterDeclaration(
                constructor.getParameterTypes()[i],
                constructor.getGenericParameterTypes()[i],
                typeSolver,
                variadic,
                constructor.getParameters()[i].getName());
    }

    @Override
    public String getName() {
        return constructor.getDeclaringClass().getSimpleName();
    }

    @Override
    public AccessSpecifier accessSpecifier() {
        return ReflectionFactory.modifiersToAccessLevel(constructor.getModifiers());
    }

    @Override
    public List<ResolvedTypeParameterDeclaration> getTypeParameters() {
        return Arrays.stream(constructor.getTypeParameters())
                .map((refTp) -> new ReflectionTypeParameter(refTp, false, typeSolver))
                .collect(Collectors.toList());
    }

    @Override
    public int getNumberOfSpecifiedExceptions() {
        return this.constructor.getExceptionTypes().length;
    }

    @Override
    public ResolvedType getSpecifiedException(int index) {
        if (index < 0 || index >= getNumberOfSpecifiedExceptions()) {
            throw new IllegalArgumentException();
        }
        return ReflectionFactory.typeUsageFor(this.constructor.getExceptionTypes()[index], typeSolver);
    }
}

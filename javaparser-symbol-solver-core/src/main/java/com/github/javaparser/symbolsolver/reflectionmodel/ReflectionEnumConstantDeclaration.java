/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.reflectionmodel;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedType;
import java.lang.reflect.Field;

public class ReflectionEnumConstantDeclaration implements ResolvedEnumConstantDeclaration {

    private Field enumConstant;
    private TypeSolver typeSolver;

    public ReflectionEnumConstantDeclaration(Field enumConstant, TypeSolver typeSolver) {
        if (!enumConstant.isEnumConstant()) {
            throw new IllegalArgumentException("The given field does not represent an enum constant");
        }
        this.enumConstant = enumConstant;
        this.typeSolver = typeSolver;
    }

    @Override
    public String getName() {
        return enumConstant.getName();
    }

    @Override
    public ResolvedType getType() {
        Class<?> enumClass = enumConstant.getDeclaringClass();
        ResolvedReferenceTypeDeclaration typeDeclaration = new ReflectionEnumDeclaration(enumClass, typeSolver);
        return new ReferenceTypeImpl(typeDeclaration);
    }
}

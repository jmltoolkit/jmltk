/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.reflectionmodel;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedTypePatternDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * WARNING: Implemented fairly blindly. Unsure if required or even appropriate. Use with extreme caution.
 *
 * @author Roger Howell
 */
public class ReflectionPatternDeclaration implements ResolvedTypePatternDeclaration {

    private Class<?> type;
    private TypeSolver typeSolver;
    private String name;

    /**
     * @param type
     * @param typeSolver
     * @param name       can potentially be null
     */
    public ReflectionPatternDeclaration(Class<?> type, TypeSolver typeSolver, String name) {
        this.type = type;
        this.typeSolver = typeSolver;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean hasName() {
        return name != null;
    }

    @Override
    public boolean isField() {
        return false;
    }

    @Override
    public boolean isParameter() {
        return false;
    }

    @Override
    public boolean isTypePattern() {
        return true;
    }

    @Override
    public boolean isType() {
        return false;
    }

    @Override
    public ResolvedType getType() {
        return ReflectionFactory.typeUsageFor(type, typeSolver);
    }
}

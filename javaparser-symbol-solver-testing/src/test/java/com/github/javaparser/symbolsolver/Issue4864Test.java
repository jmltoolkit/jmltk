/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.reflectionmodel.*;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import org.junit.jupiter.api.Test;

public class Issue4864Test {
    private final TypeSolver typeSolver = null;

    @Test
    void testReflectionInterfaceDeclarationToString() {
        ReflectionInterfaceDeclaration decl = new ReflectionInterfaceDeclaration(Runnable.class, typeSolver);
        String output = decl.toString();

        assertTrue(output.contains("ReflectionInterfaceDeclaration"));
        assertTrue(output.contains("clazz=java.lang.Runnable"));
    }

    @Test
    void testReflectionClassDeclarationToString() {
        ReflectionClassDeclaration decl = new ReflectionClassDeclaration(String.class, typeSolver);
        String output = decl.toString();

        assertTrue(output.contains("ReflectionClassDeclaration"));
        assertTrue(output.contains("clazz="));
        assertTrue(output.contains("java.lang.String"));
    }

    @Test
    void testReflectionParameterDeclarationToString() {
        ReflectionParameterDeclaration decl =
                new ReflectionParameterDeclaration(String.class, String.class, typeSolver, false, "param1");
        String output = decl.toString();

        assertTrue(output.contains("ReflectionParameterDeclaration"));
        assertTrue(output.contains("type=class java.lang.String"));
        assertTrue(output.contains("name=param1"));
    }

    @Test
    void testReflectionTypeParameterToString() {
        TypeVariable<Class<Comparable>> typeVar = Comparable.class.getTypeParameters()[0];
        ReflectionTypeParameter param = new ReflectionTypeParameter(typeVar, true, typeSolver);
        String output = param.toString();

        assertTrue(output.contains("ReflectionTypeParameter"));
        assertTrue(output.contains("typeVariable="));
        assertTrue(output.contains("T"));
    }

    @Test
    void testReflectionMethodDeclarationToString() throws NoSuchMethodException {
        Method method = String.class.getMethod("substring", int.class, int.class);
        ReflectionMethodDeclaration decl = new ReflectionMethodDeclaration(method, typeSolver);
        String output = decl.toString();

        assertTrue(output.contains("ReflectionMethodDeclaration"));
        assertTrue(output.contains("method=public java.lang.String java.lang.String.substring(int,int)"));
    }
}

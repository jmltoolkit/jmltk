/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.reflectionmodel;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReflectionMethodDeclarationTest {

    @Test
    void testParameterNameOnClassesFromTheStdLibrary() {
        TypeSolver typeResolver = new ReflectionTypeSolver();

        ResolvedClassDeclaration object = new ReflectionClassDeclaration(Object.class, typeResolver);
        ResolvedInterfaceDeclaration list = new ReflectionInterfaceDeclaration(List.class, typeResolver);

        ResolvedMethodDeclaration equals = object.getAllMethods().stream()
                .filter(m -> m.getName().equals("equals"))
                .findFirst()
                .get()
                .getDeclaration();
        ResolvedMethodDeclaration containsAll = list.getAllMethods().stream()
                .filter(m -> m.getName().equals("containsAll"))
                .findFirst()
                .get()
                .getDeclaration();
        ResolvedMethodDeclaration subList = list.getAllMethods().stream()
                .filter(m -> m.getName().equals("subList"))
                .findFirst()
                .get()
                .getDeclaration();

        assertEquals("arg0", equals.getParam(0).getName());
        assertEquals("arg0", containsAll.getParam(0).getName());
        assertEquals("arg0", subList.getParam(0).getName());
        assertEquals("arg1", subList.getParam(1).getName());
    }

    class Foo {
        void myMethod(int a, char c) {}
    }

    @Test
    void testParameterNameOnClassesFromThisProject() {
        TypeSolver typeResolver = new ReflectionTypeSolver(false);

        ResolvedClassDeclaration foo = new ReflectionClassDeclaration(Foo.class, typeResolver);

        ResolvedMethodDeclaration myMethod = foo.getAllMethods().stream()
                .filter(m -> m.getName().equals("myMethod"))
                .findFirst()
                .get()
                .getDeclaration();

        assertEquals("a", myMethod.getParam(0).getName());
        assertEquals("c", myMethod.getParam(1).getName());
    }
}

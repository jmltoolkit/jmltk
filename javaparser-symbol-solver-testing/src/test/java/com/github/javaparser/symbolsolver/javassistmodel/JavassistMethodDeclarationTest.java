/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javassistmodel;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.symbolsolver.AbstractSymbolResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class JavassistMethodDeclarationTest extends AbstractSymbolResolutionTest {

    private TypeSolver typeSolver;

    @BeforeEach
    void setup() throws IOException {
        Path pathToJar = adaptPath("src/test/resources/javassistmethoddecl/javassistmethoddecl.jar");
        typeSolver = new CombinedTypeSolver(new JarTypeSolver(pathToJar), new ReflectionTypeSolver());
    }

    @Test
    void getParam_forMethodParameterWithRawType() {
        JavassistClassDeclaration classDecl = (JavassistClassDeclaration) typeSolver.solveType("C");
        JavassistMethodDeclaration method = findMethodWithName(classDecl, "methodWithRawParameter");

        ResolvedParameterDeclaration param = method.getParam(0);

        assertThat(param.describeType(), is("java.util.List"));
    }

    @Test
    void getParam_forMethodParameterWithGenericType() {
        JavassistClassDeclaration classDecl = (JavassistClassDeclaration) typeSolver.solveType("C");
        JavassistMethodDeclaration method = findMethodWithName(classDecl, "methodWithGenericParameter");

        ResolvedParameterDeclaration param = method.getParam(0);

        assertThat(param.describeType(), is("java.util.List<java.lang.String>"));
    }

    @Test
    void getParam_forMethodParameterWithTypeParameter() {
        JavassistClassDeclaration classDecl = (JavassistClassDeclaration) typeSolver.solveType("C");
        JavassistMethodDeclaration method = findMethodWithName(classDecl, "methodWithTypeParameter");

        ResolvedParameterDeclaration param = method.getParam(0);

        assertThat(param.describeType(), is("java.util.List<S>"));
    }

    @Test
    void getParam_forGenericMethodWithTypeParameter() {
        JavassistClassDeclaration classDecl = (JavassistClassDeclaration) typeSolver.solveType("C");
        JavassistMethodDeclaration method = findMethodWithName(classDecl, "genericMethodWithTypeParameter");

        ResolvedParameterDeclaration param = method.getParam(0);

        assertThat(param.describeType(), is("java.util.List<T>"));
    }

    @Test
    void testGetExceptionsFromMethodWithoutExceptions() {
        JavassistClassDeclaration classDecl = (JavassistClassDeclaration) typeSolver.solveType("C");
        JavassistMethodDeclaration method = findMethodWithName(classDecl, "genericMethodWithTypeParameter");

        assertThat(method.getNumberOfSpecifiedExceptions(), is(0));
    }

    @Test
    void testGetExceptionsFromMethodWithExceptions() {
        JavassistClassDeclaration classDecl = (JavassistClassDeclaration) typeSolver.solveType("C");
        JavassistMethodDeclaration method = findMethodWithName(classDecl, "methodWithExceptions");

        assertThat(method.getNumberOfSpecifiedExceptions(), is(2));
        assertThat(method.getSpecifiedException(0).describe(), is("java.lang.IllegalArgumentException"));
        assertThat(method.getSpecifiedException(1).describe(), is("java.io.IOException"));
    }

    private JavassistMethodDeclaration findMethodWithName(JavassistClassDeclaration classDecl, String name) {
        return classDecl.getDeclaredMethods().stream()
                .filter(methodDecl -> methodDecl.getName().equals(name))
                .map(m -> (JavassistMethodDeclaration) m)
                .findAny()
                .get();
    }
}

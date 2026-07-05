/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.logic;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.logic.FunctionalInterfaceLogic;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionInterfaceDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FunctionInterfaceLogicTest {

    @Test
    void testGetFunctionalMethodNegativeCaseOnClass() {
        TypeSolver typeSolver = new ReflectionTypeSolver();
        ResolvedType string = new ReferenceTypeImpl(new ReflectionClassDeclaration(String.class, typeSolver));
        assertEquals(false, FunctionalInterfaceLogic.getFunctionalMethod(string).isPresent());
    }

    @Test
    void testGetFunctionalMethodPositiveCasesOnInterfaces() {
        TypeSolver typeSolver = new ReflectionTypeSolver();
        ResolvedType function = new ReferenceTypeImpl(new ReflectionInterfaceDeclaration(Function.class, typeSolver));
        assertEquals(
                true, FunctionalInterfaceLogic.getFunctionalMethod(function).isPresent());
        assertEquals(
                "apply",
                FunctionalInterfaceLogic.getFunctionalMethod(function).get().getName());
        ResolvedType consumer = new ReferenceTypeImpl(new ReflectionInterfaceDeclaration(Consumer.class, typeSolver));
        assertEquals(
                true, FunctionalInterfaceLogic.getFunctionalMethod(consumer).isPresent());
        assertEquals(
                "accept",
                FunctionalInterfaceLogic.getFunctionalMethod(consumer).get().getName());
    }

    @Test
    void testGetFunctionalMethodWith2AbstractMethodsInHierarcy() {
        TypeSolver typeSolver = new ReflectionTypeSolver();
        ResolvedType function = new ReferenceTypeImpl(new ReflectionInterfaceDeclaration(Foo.class, typeSolver));
        // By default, all methods in interface are public and abstract until we do not declare it
        // as default and properties are static and final.
        // This interface is not fonctional because it inherits two abstract methods
        // which are not members of Object and the default apply method does not override the abstract apply method
        // defined in the Function interface.
        assertEquals(
                false, FunctionalInterfaceLogic.getFunctionalMethod(function).isPresent());
    }

    public static interface Foo<S, T> extends Function<S, T> {

        T foo(S str);

        @Override
        default T apply(S str) {
            return foo(str);
        }
    }
}

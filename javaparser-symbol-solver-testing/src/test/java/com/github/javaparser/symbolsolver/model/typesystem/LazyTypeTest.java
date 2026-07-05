/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.model.typesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.model.typesystem.LazyType;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.AbstractSymbolResolutionTest;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LazyTypeTest extends AbstractSymbolResolutionTest {

    private ResolvedType foo;
    private ResolvedType bar;
    private ResolvedType baz;
    private ResolvedType lazyFoo;
    private ResolvedType lazyBar;
    private ResolvedType lazyBaz;
    private TypeSolver typeSolver;

    class Foo {}

    class Bar {}

    class Baz extends Foo {}

    @BeforeEach
    void setup() {
        typeSolver = new ReflectionTypeSolver();
        foo = new ReferenceTypeImpl(new ReflectionClassDeclaration(Foo.class, typeSolver));
        bar = new ReferenceTypeImpl(new ReflectionClassDeclaration(Bar.class, typeSolver));
        baz = new ReferenceTypeImpl(new ReflectionClassDeclaration(Baz.class, typeSolver));
        lazyFoo = lazy(foo);
        lazyBar = lazy(bar);
        lazyBaz = lazy(baz);
    }

    private ResolvedType lazy(ResolvedType type) {
        return new LazyType(v -> type);
    }

    @Test
    void testIsAssignable() {
        assertEquals(true, foo.isAssignableBy(foo));
        assertEquals(true, foo.isAssignableBy(baz));
        assertEquals(false, foo.isAssignableBy(bar));

        assertEquals(true, lazyFoo.isAssignableBy(lazyFoo));
        assertEquals(true, lazyFoo.isAssignableBy(lazyBaz));
        assertEquals(false, lazyFoo.isAssignableBy(lazyBar));

        assertEquals(true, foo.isAssignableBy(lazyFoo));
        assertEquals(true, foo.isAssignableBy(lazyBaz));
        assertEquals(false, foo.isAssignableBy(lazyBar));
    }
}

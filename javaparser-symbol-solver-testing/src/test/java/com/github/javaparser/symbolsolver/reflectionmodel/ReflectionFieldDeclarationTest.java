/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.reflectionmodel;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.AbstractSymbolResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionFieldDeclarationTest extends AbstractSymbolResolutionTest {

    private TypeSolver typeSolver = new ReflectionTypeSolver(false);

    @Test
    void verifyIsVolatileVariableDeclaration() {
        ReflectionClassDeclaration rcd = new ReflectionClassDeclaration(AtomicBoolean.class, typeSolver);
        assertTrue(rcd.getField("value").isVolatile());
    }

    @Test
    void verifyIsNotVolatileVariableDeclaration() {
        ReflectionClassDeclaration rcd = new ReflectionClassDeclaration(String.class, typeSolver);
        assertFalse(rcd.getField("serialVersionUID").isVolatile());
    }
}

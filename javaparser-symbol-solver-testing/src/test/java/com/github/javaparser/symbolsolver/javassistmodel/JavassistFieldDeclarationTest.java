/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javassistmodel;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavassistFieldDeclarationTest {

    private TypeSolver typeSolver = new ReflectionTypeSolver(false);

    @Test
    void verifyIsVolatileVariableDeclarationFromJavassist() throws NotFoundException {
        CtClass clazz = ClassPool.getDefault().getCtClass("java.util.concurrent.atomic.AtomicBoolean");
        JavassistClassDeclaration jcd = new JavassistClassDeclaration(clazz, typeSolver);
        assertTrue(jcd.getField("value").isVolatile());
    }

    @Test
    void verifyIsNotVolatileVariableDeclarationFromJavassist() throws NotFoundException {
        CtClass clazz = ClassPool.getDefault().getCtClass("java.lang.String");
        JavassistClassDeclaration jcd = new JavassistClassDeclaration(clazz, typeSolver);
        assertFalse(jcd.getField("serialVersionUID").isVolatile());
    }
}

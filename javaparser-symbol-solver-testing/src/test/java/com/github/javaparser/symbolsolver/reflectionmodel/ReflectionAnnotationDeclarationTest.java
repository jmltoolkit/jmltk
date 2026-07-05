/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.reflectionmodel;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Inherited;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@interface OuterAnnotation {
    @interface InnerAnnotation {}
}

@interface WithValue {
    String value();
}

@interface WithField {
    int FIELD_DECLARATION = 0;
}

@Inherited
@interface InheritedAnnotation {}

class ReflectionAnnotationDeclarationTest {
    private TypeSolver typeSolver = new ReflectionTypeSolver(false);

    @Test
    void isClass() {
        ReflectionAnnotationDeclaration annotation = (ReflectionAnnotationDeclaration)
                typeSolver.solveType("com.github.javaparser.symbolsolver.reflectionmodel.OuterAnnotation");
        assertFalse(annotation.isClass());
    }

    @Test
    void innerIsClass() {
        ReflectionAnnotationDeclaration annotation = (ReflectionAnnotationDeclaration) typeSolver.solveType(
                "com.github.javaparser.symbolsolver.reflectionmodel.OuterAnnotation.InnerAnnotation");
        assertFalse(annotation.isClass());
    }

    @Test
    void getInternalTypes() {
        ReflectionAnnotationDeclaration annotation = (ReflectionAnnotationDeclaration)
                typeSolver.solveType("com.github.javaparser.symbolsolver.reflectionmodel.OuterAnnotation");
        assertEquals(
                Collections.singleton("InnerAnnotation"),
                annotation.internalTypes().stream()
                        .map(ResolvedDeclaration::getName)
                        .collect(Collectors.toSet()));
    }

    @Test
    void solveMethodForAnnotationWithValue() {
        ReflectionAnnotationDeclaration annotation =
                (ReflectionAnnotationDeclaration) typeSolver.solveType(WithValue.class.getCanonicalName());
        final SymbolReference<ResolvedMethodDeclaration> symbolReference =
                annotation.solveMethod("value", Collections.emptyList(), false, null);
        assertEquals("value", symbolReference.getCorrespondingDeclaration().getName());
    }

    @Test
    void getAllFields_shouldReturnTheCorrectFields() {
        ReflectionAnnotationDeclaration annotation = (ReflectionAnnotationDeclaration)
                typeSolver.solveType("com.github.javaparser.symbolsolver.reflectionmodel.WithField");
        assertEquals(
                Collections.singleton("FIELD_DECLARATION"),
                annotation.getAllFields().stream()
                        .map(ResolvedDeclaration::getName)
                        .collect(Collectors.toSet()));
    }

    @Test
    void getClassName_shouldReturnCorrectValue() {
        ReflectionAnnotationDeclaration annotation = (ReflectionAnnotationDeclaration)
                typeSolver.solveType("com.github.javaparser.symbolsolver.reflectionmodel.WithField");
        assertEquals("WithField", annotation.getClassName());
    }

    @Test
    void isAnnotationNotInheritable() {
        ReflectionAnnotationDeclaration annotation = (ReflectionAnnotationDeclaration)
                typeSolver.solveType("com.github.javaparser.symbolsolver.reflectionmodel.OuterAnnotation");
        assertFalse(annotation.isInheritable());
    }

    @Test
    void isAnnotationInheritable() {
        ReflectionAnnotationDeclaration annotation = (ReflectionAnnotationDeclaration)
                typeSolver.solveType("com.github.javaparser.symbolsolver.reflectionmodel.InheritedAnnotation");
        assertTrue(annotation.isInheritable());
    }
}

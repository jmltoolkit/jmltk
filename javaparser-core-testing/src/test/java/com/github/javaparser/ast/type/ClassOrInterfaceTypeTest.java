/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.type;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClassOrInterfaceTypeTest {

    @Test
    void testSetName() {
        ClassOrInterfaceType classOrInterfaceType = new ClassOrInterfaceType();

        assertNotEquals("A", classOrInterfaceType.getName().toString());
        classOrInterfaceType.setName("A");
        assertEquals("A", classOrInterfaceType.getName().toString());
    }

    @Test
    void testNestedClass() {
        ClassOrInterfaceType classA = new ClassOrInterfaceType();
        classA.setName("A");
        ClassOrInterfaceType classB = new ClassOrInterfaceType(classA, "B");

        assertEquals("A.B", classB.getNameWithScope());
    }

    @Test
    void testWithGeneric() {
        ClassOrInterfaceType classA = new ClassOrInterfaceType(null, "A");
        ClassOrInterfaceType classB = new ClassOrInterfaceType(classA, new SimpleName("B"), new NodeList<>(classA));

        assertTrue(classB.getTypeArguments().isPresent());
        assertEquals(1, classB.getTypeArguments().get().size());
        assertEquals(classA, classB.getTypeArguments().get().get(0));

        assertEquals("A.B", classB.getNameWithScope());
        assertEquals("A.B<A>", classB.asString());
    }

    @Test
    void testWithAnnotations() {
        AnnotationExpr annotationExpr = StaticJavaParser.parseAnnotation("@Override");
        ClassOrInterfaceType classA =
                new ClassOrInterfaceType(null, new SimpleName("A"), null, new NodeList<>(annotationExpr));

        assertEquals(1, classA.getAnnotations().size());
        assertEquals(annotationExpr, classA.getAnnotation(0));
    }

    @Test
    void testResolveWithoutCompilationUnit() {
        ClassOrInterfaceType classA = new ClassOrInterfaceType(null, "A");
        Assertions.assertThrows(IllegalStateException.class, classA::resolve);
    }

    @Test
    void testToDescriptorWithoutCompilationUnit() {
        ClassOrInterfaceType classA = new ClassOrInterfaceType(null, "A");
        Assertions.assertThrows(IllegalStateException.class, classA::toDescriptor);
    }

    @Test
    void testToClassOrInterfaceType() {
        ClassOrInterfaceType classA = new ClassOrInterfaceType(null, "A");

        Optional<ClassOrInterfaceType> newClass = classA.toClassOrInterfaceType();
        assertTrue(newClass.isPresent());
        assertSame(classA, newClass.get());
    }

    @Test
    void testIfClassOrInterfaceTypeIsCalled() {
        ClassOrInterfaceType classA = new ClassOrInterfaceType(null, "A");
        classA.ifClassOrInterfaceType(classOrInterfaceType -> assertSame(classA, classOrInterfaceType));
    }

    @Test
    void testAsClassOrInterfaceTypeIsTheSame() {
        ClassOrInterfaceType classA = new ClassOrInterfaceType(null, "A");

        assertTrue(classA.isClassOrInterfaceType());
        assertEquals(classA, classA.asClassOrInterfaceType());
    }

    @Test
    void testCloneClass() {
        ClassOrInterfaceType classA = new ClassOrInterfaceType(null, "A");
        assertEquals(classA, classA.clone());
    }

    @Test
    void testMetaModel() {
        ClassOrInterfaceType classA = new ClassOrInterfaceType(null, "A");
        assertEquals(JavaParserMetaModel.classOrInterfaceTypeMetaModel, classA.getMetaModel());
    }

    @Test
    void testAcceptVoidVisitor() {
        ClassOrInterfaceType classA = new ClassOrInterfaceType(null, "A");
        classA.accept(
                new VoidVisitorAdapter<Object>() {
                    @Override
                    public void visit(ClassOrInterfaceType classOrInterfaceType, Object object) {
                        super.visit(classOrInterfaceType, object);

                        assertEquals(classA, classOrInterfaceType);
                    }
                },
                null);
    }
}

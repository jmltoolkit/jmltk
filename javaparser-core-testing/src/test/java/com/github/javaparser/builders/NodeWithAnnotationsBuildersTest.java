/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.builders;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeWithAnnotationsBuildersTest {
    private CompilationUnit cu = new CompilationUnit();
    private ClassOrInterfaceDeclaration testClass = cu.addClass("testClass");

    @interface hey {}

    @Test
    void testAddAnnotation() {
        NormalAnnotationExpr annotation = testClass.addAndGetAnnotation(hey.class);
        assertEquals(
                "import com.github.javaparser.builders.NodeWithAnnotationsBuildersTest.hey;",
                cu.getImport(0).toString().trim());
        assertEquals(1, testClass.getAnnotations().size());
        assertEquals(annotation, testClass.getAnnotation(0));
        assertEquals(NormalAnnotationExpr.class, testClass.getAnnotation(0).getClass());
    }

    @Test
    void testAddMarkerAnnotation() {
        testClass.addMarkerAnnotation("test");
        assertEquals(1, testClass.getAnnotations().size());
    }

    @Test
    void testAddSingleMemberAnnotation() {
        testClass.addSingleMemberAnnotation("test", "value");
        assertEquals(1, testClass.getAnnotations().size());
        assertEquals(
                "value",
                testClass
                        .getAnnotation(0)
                        .asSingleMemberAnnotationExpr()
                        .getMemberValue()
                        .toString());
    }

    @Test
    void testAddSingleMemberAnnotation2() {
        testClass.addSingleMemberAnnotation(hey.class, new NameExpr(new SimpleName("value")));
        assertEquals(1, testClass.getAnnotations().size());
        assertEquals(
                "value",
                testClass
                        .getAnnotation(0)
                        .asSingleMemberAnnotationExpr()
                        .getMemberValue()
                        .toString());
    }

    @Test
    void testIsAnnotationPresent() {
        testClass.addMarkerAnnotation(hey.class);
        assertTrue(testClass.isAnnotationPresent(hey.class));
    }

    @Test
    void testGetAnnotationByName() {
        NormalAnnotationExpr annotation = testClass.addAndGetAnnotation(hey.class);
        assertEquals(annotation, testClass.getAnnotationByName("hey").get());
    }

    @Test
    void testGetAnnotationByClass() {
        NormalAnnotationExpr annotation = testClass.addAndGetAnnotation(hey.class);
        assertEquals(annotation, testClass.getAnnotationByClass(hey.class).get());
    }
}

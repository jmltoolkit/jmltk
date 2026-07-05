/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.transformations.ast.body;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.printer.lexicalpreservation.AbstractLexicalPreservingTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.github.javaparser.ast.Modifier.DefaultKeyword.PROTECTED;
import static com.github.javaparser.ast.Modifier.DefaultKeyword.PUBLIC;
import static com.github.javaparser.ast.Modifier.createModifierList;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Transforming AnnotationDeclaration and verifying the LexicalPreservation works as expected.
 */
class AnnotationDeclarationTransformationsTest extends AbstractLexicalPreservingTest {

    @Test
    void unchangedExamples() throws IOException {
        assertUnchanged("AnnotationDeclaration_Example1");
        assertUnchanged("AnnotationDeclaration_Example3");
        assertUnchanged("AnnotationDeclaration_Example9");
    }

    // name

    @Test
    void changingName() throws IOException {
        considerExample("AnnotationDeclaration_Example1_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().setName("NewName");
        assertTransformed("AnnotationDeclaration_Example1", cu);
    }

    // modifiers

    @Test
    void addingModifiers() throws IOException {
        considerExample("AnnotationDeclaration_Example1_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().setModifiers(createModifierList(PUBLIC));
        assertTransformed("AnnotationDeclaration_Example2", cu);
    }

    @Test
    void removingModifiers() throws IOException {
        considerExample("AnnotationDeclaration_Example3_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().setModifiers(new NodeList<>());
        assertTransformed("AnnotationDeclaration_Example3", cu);
    }

    @Test
    void replacingModifiers() throws IOException {
        considerExample("AnnotationDeclaration_Example3_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().setModifiers(createModifierList(PROTECTED));
        assertTransformed("AnnotationDeclaration_Example4", cu);
    }

    // members

    @Test
    void addingMember() throws IOException {
        considerExample("AnnotationDeclaration_Example3_original");
        cu.getAnnotationDeclarationByName("ClassPreamble")
                .get()
                .addMember(new AnnotationMemberDeclaration(new NodeList<>(), PrimitiveType.intType(), "foo", null));
        assertTransformed("AnnotationDeclaration_Example5", cu);
    }

    @Test
    void removingMember() throws IOException {
        considerExample("AnnotationDeclaration_Example3_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().getMember(2).remove();
        assertTransformed("AnnotationDeclaration_Example6", cu);
    }

    @Test
    void replacingMember() throws IOException {
        considerExample("AnnotationDeclaration_Example3_original");
        cu.getAnnotationDeclarationByName("ClassPreamble")
                .get()
                .setMember(2, new AnnotationMemberDeclaration(new NodeList<>(), PrimitiveType.intType(), "foo", null));
        assertTransformed("AnnotationDeclaration_Example7", cu);
    }

    // javadoc

    @Test
    void addingJavadoc() throws IOException {
        considerExample("AnnotationDeclaration_Example3_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().setJavadocComment("Cool this annotation!");
        assertTransformed("AnnotationDeclaration_Example8", cu);
    }

    @Test
    void removingJavadoc() throws IOException {
        considerExample("AnnotationDeclaration_Example9_original");
        boolean removed = cu.getAnnotationDeclarationByName("ClassPreamble")
                .get()
                .getJavadocComment()
                .get()
                .remove();
        assertTrue(removed);
        assertTransformed("AnnotationDeclaration_Example9", cu);
    }

    @Test
    void replacingJavadoc() throws IOException {
        considerExample("AnnotationDeclaration_Example9_original");
        cu.getAnnotationDeclarationByName("ClassPreamble")
                .get()
                .setJavadocComment("Super extra cool this annotation!!!");
        assertTransformed("AnnotationDeclaration_Example10", cu);
    }
}

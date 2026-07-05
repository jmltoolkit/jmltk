/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.transformations.ast.body;

import static com.github.javaparser.ast.Modifier.DefaultKeyword.PROTECTED;
import static com.github.javaparser.ast.Modifier.DefaultKeyword.PUBLIC;
import static com.github.javaparser.ast.Modifier.createModifierList;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.printer.lexicalpreservation.AbstractLexicalPreservingTest;
import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.Test;

/**
 * Transforming FieldDeclaration and verifying the LexicalPreservation works as expected.
 */
class FieldDeclarationTransformationsTest extends AbstractLexicalPreservingTest {

    protected FieldDeclaration consider(String code) {
        considerCode("class A { " + code + " }");
        return cu.getType(0).getMembers().get(0).asFieldDeclaration();
    }

    // JavaDoc

    // Modifiers

    @Test
    void addingModifiers() {
        FieldDeclaration it = consider("int A;");
        it.setModifiers(createModifierList(PUBLIC));
        assertTransformedToString("public int A;", it);
    }

    @Test
    void removingModifiers() {
        FieldDeclaration it = consider("public int A;");
        it.setModifiers(new NodeList<>());
        assertTransformedToString("int A;", it);
    }

    @Test
    void removingModifiersFromNonPrimitiveType() {
        FieldDeclaration it = consider("public String A;");
        it.setModifiers(new NodeList<>());
        assertTransformedToString("String A;", it);
    }

    @Test
    void replacingModifiers() {
        FieldDeclaration it = consider("int A;");
        it.setModifiers(createModifierList(PROTECTED));
        assertTransformedToString("protected int A;", it);
    }

    @Test
    void changingTypes() {
        FieldDeclaration it = consider("int a, b;");
        assertTransformedToString("int a, b;", it);
        it.getVariable(0).setType("Xyz");
        assertTransformedToString(" a, b;", it);
        it.getVariable(1).setType("Xyz");
        assertTransformedToString("Xyz a, b;", it);
    }

    @Test
    public void changingNonePrimitiveTypes() {
        FieldDeclaration it = consider("String a;");
        it.getVariable(0).setType("Xyz");
        assertTransformedToString("Xyz a;", it);
    }

    // Annotations
    @Test
    void removingAnnotations() {
        FieldDeclaration it = consider(LineSeparator.SYSTEM + "@Annotation" + LineSeparator.SYSTEM + "public int A;");
        it.getAnnotationByName("Annotation").get().remove();
        assertTransformedToString("public int A;", it);
    }

    @Test
    void removingAnnotationsWithSpaces() {
        FieldDeclaration it =
                consider(LineSeparator.SYSTEM + "  @Annotation " + LineSeparator.SYSTEM + "public int A;");
        it.getAnnotationByName("Annotation").get().remove();
        assertTransformedToString("public int A;", it);
    }
}

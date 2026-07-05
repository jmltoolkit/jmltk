/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.transformations.ast.body;

import static com.github.javaparser.ast.Modifier.DefaultKeyword.PROTECTED;
import static com.github.javaparser.ast.Modifier.DefaultKeyword.PUBLIC;
import static com.github.javaparser.ast.Modifier.createModifierList;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.printer.lexicalpreservation.AbstractLexicalPreservingTest;
import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.Test;

/**
 * Transforming EnumDeclaration and verifying the LexicalPreservation works as expected.
 */
class EnumDeclarationTransformationsTest extends AbstractLexicalPreservingTest {

    protected EnumDeclaration consider(String code) {
        considerCode(code);
        return cu.getType(0).asEnumDeclaration();
    }

    // Name

    @Test
    void settingName() {
        EnumDeclaration cid = consider("enum A { E1, E2 }");
        cid.setName("B");
        assertTransformedToString("enum B { E1, E2 }", cid);
    }

    // implementedTypes

    // Modifiers

    @Test
    void addingModifiers() {
        EnumDeclaration ed = consider("enum A { E1, E2 }");
        ed.setModifiers(createModifierList(PUBLIC));
        assertTransformedToString("public enum A { E1, E2 }", ed);
    }

    @Test
    void removingModifiers() {
        EnumDeclaration ed = consider("public enum A { E1, E2 }");
        ed.setModifiers(new NodeList<>());
        assertTransformedToString("enum A { E1, E2 }", ed);
    }

    @Test
    void replacingModifiers() {
        EnumDeclaration ed = consider("public enum A { E1, E2 }");
        ed.setModifiers(createModifierList(PROTECTED));
        assertTransformedToString("protected enum A { E1, E2 }", ed);
    }

    @Test
    void addingConstants() {
        EnumDeclaration ed = consider("enum A {" + LineSeparator.SYSTEM + " E1" + LineSeparator.SYSTEM + "}");
        ed.getEntries().addNLast(new EnumConstantDeclaration("E2"));
        assertTransformedToString(
                "enum A {" + LineSeparator.SYSTEM + " E1," + LineSeparator.SYSTEM + " E2" + LineSeparator.SYSTEM + "}",
                ed);
    }

    // members

    // Annotations

    // Javadoc

}

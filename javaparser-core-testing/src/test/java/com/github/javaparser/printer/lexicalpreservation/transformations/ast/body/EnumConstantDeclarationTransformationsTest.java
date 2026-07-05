/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.transformations.ast.body;

import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.printer.lexicalpreservation.AbstractLexicalPreservingTest;
import org.junit.jupiter.api.Test;

/**
 * Transforming EnumConstantDeclaration and verifying the LexicalPreservation works as expected.
 */
class EnumConstantDeclarationTransformationsTest extends AbstractLexicalPreservingTest {

    protected EnumConstantDeclaration consider(String code) {
        considerCode("enum A { " + code + " }");
        return cu.getType(0).asEnumDeclaration().getEntries().get(0);
    }

    // Name

    @Test
    void settingName() {
        EnumConstantDeclaration ecd = consider("A");
        ecd.setName("B");
        assertTransformedToString("B", ecd);
    }

    // Annotations

    // Javadoc

}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.transformations.ast.body;

import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.printer.lexicalpreservation.AbstractLexicalPreservingTest;
import org.junit.jupiter.api.Test;

/**
 * Transforming InitializerDeclaration and verifying the LexicalPreservation works as expected.
 */
class InitializerDeclarationTransformationsTest extends AbstractLexicalPreservingTest {

    protected InitializerDeclaration consider(String code) {
        considerCode("class A { " + code + " }");
        return cu.getType(0).getMembers().get(0).asInitializerDeclaration();
    }

    // JavaDoc

    // Body

    // IsStatic

    @Test
    void instanceToStatic() {
        InitializerDeclaration it = consider("{ /*some comment*/ }");
        it.setStatic(true);
        assertTransformedToString("static { /*some comment*/ }", it);
    }

    @Test
    void staticToInstance() {
        InitializerDeclaration it = consider("static { /*some comment*/ }");
        it.setStatic(false);
        assertTransformedToString("{ /*some comment*/ }", it);
    }

    // Annotations
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.body.FieldDeclaration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue3750Test extends AbstractLexicalPreservingTest {

    @Test
    void test() {
        considerCode("public class MyClass {\n" + " String s0;\n" + " // Comment\n" + " String s1;\n" + "}");

        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        FieldDeclaration field = fields.get(0);

        String expected = "public class MyClass {\n" + " // Comment\n" + " String s1;\n" + "}";

        field.remove();

        assertEquals(expected, LexicalPreservingPrinter.print(cu));
    }
}

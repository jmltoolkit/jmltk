/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

import com.github.javaparser.ast.body.FieldDeclaration;
import java.util.List;
import org.junit.jupiter.api.Test;

public class Issue3796Test extends AbstractLexicalPreservingTest {

    @Test
    void test() {
        considerCode("public class MyClass {\n" + "	/** Comment */ \n" + "	@Rule String s0; \n" + "}");
        String expected = "public class MyClass {\n" + "\n" + "}";

        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        FieldDeclaration field = fields.get(0);

        field.remove();

        assertEqualsStringIgnoringEol(expected, LexicalPreservingPrinter.print(cu));
    }
}

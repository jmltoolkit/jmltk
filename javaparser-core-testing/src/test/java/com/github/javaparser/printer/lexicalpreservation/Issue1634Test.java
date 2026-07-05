/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import org.junit.jupiter.api.Test;

public class Issue1634Test extends AbstractLexicalPreservingTest {

    @Test
    public void testWithLexicalPreservationEnabled() {

        considerCode("package com.wangym.test;\nclass A{ }");

        String expected = "package com.wangym.test;\n" + "import lombok.Data;\n" + "\n" + "class A{ }";

        NodeList<ImportDeclaration> imports = cu.getImports();
        String str = "lombok.Data";
        imports.add(new ImportDeclaration(str, false, false));

        assertEquals(expected, LexicalPreservingPrinter.print(cu));
    }
}

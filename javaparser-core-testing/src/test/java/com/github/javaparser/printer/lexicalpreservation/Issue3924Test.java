/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

import org.junit.jupiter.api.Test;

public class Issue3924Test extends AbstractLexicalPreservingTest {

    @Test
    void test() {
        considerCode("/*\n" + " * Licensed under the Apache License, Version 2.0 (the \"License\");\n"
                + " * you may not use this file except in compliance with the License.\n"
                + " * You may obtain a copy of the License at\n"
                + " */\n"
                + "\n"
                + "@XmlSchema(\n"
                + "		xmlns = {\n"
                + "				@XmlNs(prefix = \"order\", namespaceURI = \"http://www.camel.apache.org/jaxb/example/order/1\"),\n"
                + "				@XmlNs(prefix = \"address\", namespaceURI = \"http://www.camel.apache.org/jaxb/example/address/1\")\n"
                + "		}\n"
                + ")\n"
                + "package net.revelc.code.imp;\n"
                + "\n"
                + "import net.revelc.code.imp.Something;\n"
                + "\n"
                + "@Component\n"
                + "public class UnusedImports {\n"
                + "}\n"
                + "");

        LexicalPreservingPrinter.setup(cu);
        cu.getImport(0).remove();
        String actual = LexicalPreservingPrinter.print(cu);
        String expected = "/*\r\n"
                + " * Licensed under the Apache License, Version 2.0 (the \"License\");\r\n"
                + " * you may not use this file except in compliance with the License.\r\n"
                + " * You may obtain a copy of the License at\r\n"
                + " */\r\n"
                + "\r\n"
                + "@XmlSchema(\r\n"
                + "		xmlns = {\r\n"
                + "				@XmlNs(prefix = \"order\", namespaceURI = \"http://www.camel.apache.org/jaxb/example/order/1\"),\r\n"
                + "				@XmlNs(prefix = \"address\", namespaceURI = \"http://www.camel.apache.org/jaxb/example/address/1\")\r\n"
                + "		}\r\n"
                + ")\r\n"
                + "package net.revelc.code.imp;\r\n"
                + "\r\n"
                + "@Component\r\n"
                + "public class UnusedImports {\r\n"
                + "}\n";
        assertEqualsStringIgnoringEol(expected, actual);
    }
}

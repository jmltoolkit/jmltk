/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.utils.TestUtils;
import org.junit.jupiter.api.Test;

public class Issue1766Test extends AbstractLexicalPreservingTest {

    @Test
    public void testWithLexicalPreservationEnabled() {

        considerCode("public class SimpleTestClass {\n" + "  public SimpleTestClass() {\n"
                + "    // nothing\n"
                + "  }\n"
                + "  @Override\n"
                + "  void bubber() {\n"
                + "    // nothing\n"
                + "  }\n"
                + "}");

        String expected = "public class SimpleTestClass {\n" + "  public SimpleTestClass() {\n"
                + "    // nothing\n"
                + "  }\n"
                + "  @Override\n"
                + "  void bubber() {\n"
                + "    // nothing\n"
                + "  }\n"
                + "}";

        TestUtils.assertEqualsStringIgnoringEol(expected, LexicalPreservingPrinter.print(cu));
    }

    @Test
    public void testWithLexicalPreservingPrinterSetup() {

        considerCode("public class SimpleTestClass {\n" + "  public SimpleTestClass() {\n"
                + "    // nothing\n"
                + "  }\n"
                + "  @Override\n"
                + "  void bubber() {\n"
                + "    // nothing\n"
                + "  }\n"
                + "}");

        String expected = "public class SimpleTestClass {\n" + "  public SimpleTestClass() {\n"
                + "    // nothing\n"
                + "  }\n"
                + "  @Override\n"
                + "  void bubber() {\n"
                + "    // nothing\n"
                + "  }\n"
                + "}";

        TestUtils.assertEqualsStringIgnoringEol(expected, LexicalPreservingPrinter.print(cu));
    }
}

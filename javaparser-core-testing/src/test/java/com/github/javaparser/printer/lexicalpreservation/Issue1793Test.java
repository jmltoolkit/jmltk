/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class Issue1793Test extends AbstractLexicalPreservingTest {

    @AfterEach
    public void reset() {
        StaticJavaParser.setConfiguration(new ParserConfiguration());
    }

    @Test
    void importIsAddedOnTheSameLine() {
        considerCode("public class Test {\n" + "  public void foo(Bar x, Bar y) {\n"
                + "    x.barf(); // expected to be wrapped\n"
                + "    x.bark(); // expected to be wrapped\n"
                + "    y.barf(); // expected to be wrapped\n"
                + "    y.bark(); // expected to be wrapped\n"
                + "  }\n"
                + "}");
        assertEquals(LexicalPreservingPrinter.print(cu), LexicalPreservingPrinter.print(cu.clone()));
    }
}

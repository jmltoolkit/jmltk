/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.printer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LexicalPreservingPrinter must round-trip source containing iterable-domain
 * quantifiers in JML contracts byte-for-byte.
 *
 * @author Alexander Weigl
 */
class LexicalPreservingPrinterTest {

    private static final String CODE = "class IntBag {\n"
            + "    int[] a;\n"
            + "\n"
            + "    /*@ requires a != null;\n"
            + "      @ ensures (\\forall int e : a; e >= 0);\n"
            + "      @ (\\forall int i : 0..a.length-1; a[i] <= a[i + 1]);\n"
            + "      @*/\n"
            + "    void sortedAndNonNegative() {\n"
            + "    }\n"
            + "}\n";

    @Test
    void contractWithBoundQuantifiersRoundTrips() {
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> result = parser.parse(CODE);
        assertThat(result.getProblems()).as("parse problems").isEmpty();
        CompilationUnit cu = result.getResult().get();
        LexicalPreservingPrinter.setup(cu);
        assertThat(LexicalPreservingPrinter.print(cu)).isEqualTo(CODE);
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.printer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lexical-preservation round trips for the lemma feature: a class carrying a
 * {@code lemma} declaration and a proof-script {@code assert} keeps its source
 * byte-for-byte.
 */
class JmlLemmaLexicalPreservingPrinterTest {

    private static final String SOURCE = "class Foo {\n"
            + "  //@ instance lemma Pred(int i) = i > 0;\n"
            + "  //@ lemma L(int x) \\by { use_lemma nospace(x); auto; };\n"
            + "  void m(int x) {\n"
            + "    //@ assert x > 0 \\by { auto; };\n"
            + "  }\n"
            + "}\n";

    @Test
    void lemmaAndProofScriptsRoundTripByteForByte() {
        ParseResult<CompilationUnit> result =
                new JavaParser(new ParserConfiguration().setProcessJml(true)).parse(SOURCE);
        assertThat(result.isSuccessful())
                .as("parse problems: %s", result.getProblems())
                .isTrue();
        CompilationUnit cu = result.getResult().get();
        LexicalPreservingPrinter.setup(cu);
        assertThat(LexicalPreservingPrinter.print(cu)).isEqualTo(SOURCE);
    }
}

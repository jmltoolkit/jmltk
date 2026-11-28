/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.jml.doc.JmlDoc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@link JmlDoc} stores the complete JML comment, including its delimiters
 * ({@code //@ ...} or {@code /*@ ... *&#47;}). The pretty printer must not wrap such
 * content in another JML comment, which would produce doubled markers such as
 * {@code //@ //@ requires true;} or {@code //@ /*@ nullable *&#47;}.
 */
class JmlDocPrintingTest {

    private final Printer printer = new DefaultPrettyPrinter();

    @Test
    void jmlDocContentIsPrintedVerbatim() {
        // line-comment content already starts with the markers
        assertEquals("//@ requires true;", printer.print(new JmlDoc("//@ requires true;")));
        // block-comment content already contains the delimiters
        assertEquals("/*@ nullable */", printer.print(new JmlDoc("/*@ nullable */")));
    }

    @Test
    void parsedJmlDocsAreNotDoubledMarked() {
        String code = "class Foo {\n"
                + "    /*@ nullable */ int x;\n"
                + "    //@ requires true;\n"
                + "    void bar();\n"
                + "}\n";
        JavaParser parser = new JavaParser(new ParserConfiguration()
                .setProcessJml(true)
                .setKeepJmlDocs(true)
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertTrue(result.isSuccessful(), () -> result.getProblems().toString());
        CompilationUnit cu = result.getResult().orElseThrow();

        String printed = printer.print(cu);
        assertFalse(printed.contains("//@ //@"), () -> printed);
        assertFalse(printed.contains("//@ /*@"), () -> printed);
        assertTrue(printed.contains("//@ requires true;"), () -> printed);
        assertTrue(printed.contains("/*@ nullable */"), () -> printed);
    }
}

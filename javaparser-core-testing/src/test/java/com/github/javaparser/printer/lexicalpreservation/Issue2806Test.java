/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class Issue2806Test extends AbstractLexicalPreservingTest {

    private JavaParser javaParser;

    @Test
    void importIsAddedOnTheSameLine() {
        considerCode("import java.lang.IllegalArgumentException;\n" + "\n" + "public class A {\n" + "}");
        String junit5 = "import java.lang.IllegalArgumentException;\n" + "import java.nio.file.Paths;\n"
                + "\n"
                + "public class A {\n"
                + "}";
        ImportDeclaration importDeclaration = new ImportDeclaration("java.nio.file.Paths", false, false);
        CompilationUnit compilationUnit = cu.addImport(importDeclaration);
        String out = LexicalPreservingPrinter.print(compilationUnit);
        assertThat(out, equalTo(junit5));
    }
}

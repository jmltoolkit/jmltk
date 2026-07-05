/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationSpaceTest extends AbstractLexicalPreservingTest {
    /** Tests that inserted annotations on types are followed by a space. */
    @Test
    public void test() {
        considerCode("public class Foo {\n" + "    void myMethod(String param);\n" + "}");
        // Insert the annotation onto the String parameter type.
        Optional<ClassOrInterfaceType> type = cu.findFirst(ClassOrInterfaceType.class);
        type.get().addAnnotation(new MarkerAnnotationExpr("Nullable"));
        String result = LexicalPreservingPrinter.print(cu);
        // Verify that there's a space between the annotation and the String type.
        assertThat(result).contains("@Nullable String");
    }
}

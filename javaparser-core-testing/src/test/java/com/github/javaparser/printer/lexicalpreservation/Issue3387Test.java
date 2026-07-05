/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocDescription;
import java.util.StringJoiner;
import org.junit.jupiter.api.Test;

public class Issue3387Test extends AbstractLexicalPreservingTest {

    @Test
    public void test3387() {
        considerCode(new StringJoiner("\n")
                .add("class A {")
                .add("")
                .add("\tpublic void setTheNumber(int number) {")
                .add("\t\tnumber = number;")
                .add("\t}")
                .add("")
                .add("}")
                .toString());

        String expected = "class A {\n" + "\n"
                + "\t/**\n"
                + "\t * Change Javadoc\n"
                + "\t */\n"
                + "\tpublic void setTheNumber(int number) {\n"
                + "\t\tnumber = number;\n"
                + "\t}\n"
                + "\n"
                + "}";

        MethodDeclaration md = cu.findFirst(MethodDeclaration.class).get();
        // create new javadoc comment
        Javadoc javadoc = new Javadoc(JavadocDescription.parseText("Change Javadoc"));
        md.setJavadocComment("\t", javadoc);
        System.out.println(LexicalPreservingPrinter.print(cu));
        assertEqualsStringIgnoringEol(expected, LexicalPreservingPrinter.print(cu));
    }
}

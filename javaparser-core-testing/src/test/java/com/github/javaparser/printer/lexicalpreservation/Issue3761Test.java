/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.FieldDeclaration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

public class Issue3761Test extends AbstractLexicalPreservingTest {

    @Test
    public void test() {
        considerCode("""
                class C {\s
                    static String S = "s";
                }""");

        FieldDeclaration field = cu.findAll(FieldDeclaration.class).get(0);

        List<Modifier.Keyword> kws =
                field.getModifiers().stream().map(Modifier::getKeyword).collect(Collectors.toList());
        kws.add(0, Modifier.DefaultKeyword.PROTECTED);
        field.setModifiers(kws);

        String expected = "class C { \r\n    protected static String S = \"s\";\r\n" + "}";

        assertEqualsStringIgnoringEol(expected, LexicalPreservingPrinter.print(cu));
    }
}

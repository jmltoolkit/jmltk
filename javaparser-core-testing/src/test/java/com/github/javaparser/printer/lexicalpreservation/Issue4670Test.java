/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.junit.jupiter.api.Test;

public class Issue4670Test extends AbstractLexicalPreservingTest {

    @Test
    void test() {
        // A parameter with an annotation and a final modifier
        final String code = "public interface Foo {\n" + "  void bar(final @PathVariable(\"id\") String id);\n" + "}";
        considerCode(code);
        LexicalPreservingPrinter.setup(cu);

        // Remove the final modifier from parameters.
        cu.accept(
                new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(Parameter p, Void arg) {
                        p.setFinal(false);
                    }
                },
                null);

        String actual = LexicalPreservingPrinter.print(cu);
        String expected = "public interface Foo {\n" + "  void bar(@PathVariable(\"id\") String id);\n" + "}";

        assertEqualsStringIgnoringEol(expected, actual);
    }
}

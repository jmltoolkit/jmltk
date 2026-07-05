/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.SimpleName;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

public class Issue3818Test extends AbstractLexicalPreservingTest {

    @Test
    void test() {
        String src = "public class Foo {\n" + "\n" + "    public Long[][] m(int[] a){}\n" + "}";

        String expected = "public class Foo {\n" + "\n" + "    public Long[][] m(int[] b){}\n" + "}";

        BodyDeclaration<?> cu = StaticJavaParser.parseBodyDeclaration(src);
        MethodDeclaration md = cu.findAll(MethodDeclaration.class).get(0);
        LexicalPreservingPrinter.setup(md);
        Parameter p = md.getParameter(0);
        Parameter paramExpr = new Parameter(
                p.getModifiers(),
                p.getAnnotations(),
                p.getType(),
                p.isVarArgs(),
                p.getVarArgsAnnotations(),
                new SimpleName("b"));
        md.replace(p, paramExpr);
        assertEqualsStringIgnoringEol(expected, LexicalPreservingPrinter.print(cu));
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue3278Test extends AbstractResolutionTest {

    @Test
    void test() {
        String code = "public class Foo {\n"
                + "	    public static void main(String[] args) {\n"
                + "	        A a = null;\n"
                + "	        m((a == null ? \"null\" : a.getB()));\n"
                + "	    }\n"
                + "	    void m(Comparable<? extends Comparable> obj) {}\n"
                + "	}\n"
                + "\n"
                + "	class A{\n"
                + "	    private B b;\n"
                + "	    public A(B b){\n"
                + "	        this.b = b;\n"
                + "	    }\n"
                + "	    public B getB(){\n"
                + "	        return b;\n"
                + "	    }\n"
                + "	}\n"
                + "\n"
                + "	class B implements Comparable<B>{\n"
                + "\n"
                + "		@Override\n"
                + "		public int compareTo(B o) {\n"
                + "			return 0;\n"
                + "		}\n"
                + "	}";

        JavaParserAdapter parser = JavaParserAdapter.of(createParserWithResolver(defaultTypeSolver()));

        CompilationUnit cu = parser.parse(code);

        ConditionalExpr expr = cu.findFirst(ConditionalExpr.class).get();

        assertEquals(
                "java.lang.Comparable<? extends java.lang.Comparable>",
                expr.calculateResolvedType().describe());
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import org.junit.jupiter.api.Test;

public class Issue3916Test extends AbstractResolutionTest {

    @Test
    void issue3916() {

        String code = "enum MyEnum {\n"
                + "        One;\n"
                + "    }\n"
                + "\n"
                + "    class Foo {\n"
                + "        String str;\n"
                + "\n"
                + "        public void setStr(String str) {\n"
                + "            this.str = str;\n"
                + "        }\n"
                + "\n"
                + "        void test(String str) {\n"
                + "            switch (MyEnum.One.valueOf(\"\")) {\n"
                + "            case One:\n"
                + "                setStr(str);\n"
                + "                break;\n"
                + "            }\n"
                + "        }\n"
                + "    }";

        CompilationUnit cu = JavaParserAdapter.of(createParserWithResolver(defaultTypeSolver()))
                .parse(code);

        cu.findAll(MethodCallExpr.class).forEach(mce -> {
            if (mce.getNameAsString().equals("setStr")) {
                System.out.println(mce.toString());
                assertEquals("Foo.setStr(java.lang.String)", mce.resolve().getQualifiedSignature());
            }
        });
    }
}

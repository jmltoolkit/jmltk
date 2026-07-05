/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

public class Issue3083Test extends AbstractResolutionTest {

    @Test
    void test() {
        String code = "class A {\n" + "        public void getFromMap() {\n"
                + "            final java.util.Map<String, String> expected = new java.util.HashMap<>();\n"
                + "            java.util.Map.Entry<String, String> entry = get(expected, 0);\n"
                + "        }\n"
                + "        <V, K> java.util.Map.Entry<K,V> get(java.util.Map<K,V> map, int index) {\n"
                + "            return null;\n"
                + "        }\n"
                + "        Object get(Object map, int index) {\n"
                + "            return null;\n"
                + "        }\n"
                + "    }";
        ParserConfiguration config = new ParserConfiguration();
        StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
        CompilationUnit cu = StaticJavaParser.parse(code);
        MethodCallExpr mce = cu.findFirst(MethodCallExpr.class).get();
        // this test must not throw a MethodAmbiguityException on the get(expected, 0) call
        mce.resolve();
    }
}

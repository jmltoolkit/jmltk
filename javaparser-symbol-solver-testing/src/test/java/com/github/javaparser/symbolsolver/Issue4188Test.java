/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import org.junit.jupiter.api.Test;

public class Issue4188Test extends AbstractResolutionTest {

    @Test
    public void test() {

        String code = "import java.util.Optional;\n"
                + "\n"
                + "public class MethodInvocation {\n"
                + "\n"
                + "    public void staticMethodInvocation(Foo foo) {\n"
                + "        Optional<Integer> priority = Optional.of(4);\n"
                + "        priority.map(Foo::staticConvert).orElse(\"0\");\n"
                + "    }\n"
                + "\n"
                + "    public void instanceMethodInvocation(Foo foo) {\n"
                + "        Optional<Integer> priority = Optional.of(4);\n"
                + "        priority.map(foo::convert).orElse(\"0\");\n"
                + "    }\n"
                + "\n"
                + "    public void defaultMethodInvocation(Bar bar) {\n"
                + "        Optional<Integer> priority = Optional.of(4);\n"
                + "        priority.map(bar::convert).orElse(\"0\");\n"
                + "    }\n"
                + "\n"
                + "    public static class Foo {\n"
                + "        public String convert(int priority) {\n"
                + "            return Integer.toString(priority);\n"
                + "        }\n"
                + "\n"
                + "        public static String staticConvert(int priority) {\n"
                + "            return Integer.toString(priority);\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "    public interface Bar {\n"
                + "        default String convert(int priority) {\n"
                + "            return Integer.toString(priority);\n"
                + "        }\n"
                + "    }\n"
                + "}";
        JavaParser parser = createParserWithResolver(defaultTypeSolver());
        CompilationUnit cu = parser.parse(code).getResult().get();
        cu.findAll(MethodCallExpr.class).forEach(MethodCallExpr::resolve);
    }
}

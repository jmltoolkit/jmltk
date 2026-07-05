/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue3951Test extends AbstractResolutionTest {

    @Test
    void test() {
        final String code = String.join(
                System.lineSeparator(),
                "package test;",
                "import java.util.HashMap;",
                "import java.util.Map;",
                "interface Foo {",
                "    String getFoo();",
                "    String getBar();",
                "}",
                "class FooImpl implements Foo {",
                "    String getFoo() { return \"foo\"; } ",
                "    String getBar() { return \"bar\"; } ",
                "}",
                "public class Application {",
                "    public static void main() {",
                "        Foo f = new FooImpl();",
                "        Map<Foo, Object> m = new HashMap<>();",
                "        assertThat(m.containsKey(f));",
                "    }",
                "    public static void assertThat(Object m) {",
                "        assert m != null;",
                "    }",
                "}");

        CompilationUnit cu = JavaParserAdapter.of(createParserWithResolver(new ReflectionTypeSolver()))
                .parse(code);

        MethodCallExpr getOrDefaultCall = cu.findAll(MethodCallExpr.class).stream()
                .filter(m -> m.getNameAsString().equals("assertThat"))
                .findFirst()
                .get();

        assertEquals("test.Application.assertThat", getOrDefaultCall.resolve().getQualifiedName());
    }
}

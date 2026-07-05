/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.List;
import org.junit.jupiter.api.Test;

public class Issue2062Test extends AbstractSymbolResolutionTest {

    @Test
    public void test() {

        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver(false)));
        StaticJavaParser.setConfiguration(config);

        String s = "import java.util.Optional;\n" + "public class Base{\n"
                + "    class Derived extends Base{\n"
                + "    }\n"
                + "    \n"
                + "    public void bar(Optional<Base> o) {\n"
                + "    }\n"
                + "    public void foo() {\n"
                + "        bar(Optional.of(new Derived()));\n"
                + "    }\n"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(s);
        List<MethodCallExpr> mces = cu.findAll(MethodCallExpr.class);
        assertEquals("bar(Optional.of(new Derived()))", mces.get(0).toString());
        assertEquals("Base.bar(java.util.Optional<Base>)", mces.get(0).resolve().getQualifiedSignature());
    }
}

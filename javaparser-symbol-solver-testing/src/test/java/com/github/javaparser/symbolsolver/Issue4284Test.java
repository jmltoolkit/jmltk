/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import org.junit.jupiter.api.Test;

public class Issue4284Test extends AbstractResolutionTest {

    @Test
    void test() {

        String code = "public class SampleCode {\n"
                + "    public static void main(String... args) {\n"
                + "        char ch = args[0].charAt(0);\n"
                + "        int result = switch (ch) {\n"
                + "            default -> System.out.println(ch);\n"
                + "        };\n"
                + "    }\n"
                + "}";

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                .setSymbolResolver(symbolResolver(defaultTypeSolver()));

        CompilationUnit cu =
                JavaParserAdapter.of(new JavaParser(parserConfiguration)).parse(code);

        cu.findAll(MethodCallExpr.class).forEach(MethodCallExpr::resolve);
    }
}

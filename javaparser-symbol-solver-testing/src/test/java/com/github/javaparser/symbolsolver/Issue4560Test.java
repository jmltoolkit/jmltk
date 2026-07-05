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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class Issue4560Test extends AbstractResolutionTest {

    @Test
    void test() {

        String code = "public class MyExample {\n"
                + "\n"
                + "  public static void main(String[] args) {\n"
                + "    \"\"\"\n"
                + "        Hello multiple %s!\n"
                + "        \"\"\".format(\"world\");\n"
                + "  }\n"
                + "}";

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_15)
                .setSymbolResolver(symbolResolver(defaultTypeSolver()));

        CompilationUnit cu =
                JavaParserAdapter.of(new JavaParser(parserConfiguration)).parse(code);

        assertDoesNotThrow(() -> cu.findAll(MethodCallExpr.class).forEach(MethodCallExpr::resolve));
    }
}

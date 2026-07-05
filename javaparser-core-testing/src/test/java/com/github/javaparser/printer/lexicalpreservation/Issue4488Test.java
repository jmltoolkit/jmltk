/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue4488Test {
    @Test
    void cannotChangeMethodNameInLambda() {
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setLexicalPreservationEnabled(true);
        StaticJavaParser.setConfiguration(parserConfiguration);

        CompilationUnit cu =
                StaticJavaParser.parse("class Test {\n" + "	private Map<String, String> dummyMap = new HashMap<>();\n"
                        + "	public String dummyFunction(String name) {\n"
                        + "		return dummyMap.computeIfAbsent(name,\n"
                        + "			(Function<String, String>) s -> SomeFunction.withAMethodHere(\"test\").build());\n"
                        + "	}\n"
                        + "}");

        cu.accept(
                new ModifierVisitor() {
                    @Override
                    public Visitable visit(MethodCallExpr mc, Object arg) {
                        if (mc.getNameAsString().equals("withAMethodHere")) {
                            return mc.setName("replacedMethodHere");
                        }
                        return super.visit(mc, arg);
                    }
                },
                null);

        assertEquals(
                "class Test {\n" + "	private Map<String, String> dummyMap = new HashMap<>();\n"
                        + "	public String dummyFunction(String name) {\n"
                        + "		return dummyMap.computeIfAbsent(name,\n"
                        + "			(Function<String, String>) s -> SomeFunction.replacedMethodHere(\"test\").build());\n"
                        + "	}\n"
                        + "}",
                LexicalPreservingPrinter.print(cu));
    }
}

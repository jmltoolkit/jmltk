/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

/**
 * Solving generic types that are of type java.lang.Object
 * @see <a href="https://github.com/javaparser/javaparser/issues/1370">https://github.com/javaparser/javaparser/issues/1370</a>
 */
public class Issue1370Test {

    @Test
    public void test() {
        final String source = String.join(
                System.lineSeparator(),
                "package graph;",
                "class Vertex<Data> {",
                "    private final Data data;",
                "    public Vertex(Data data) { this.data = data; }",
                "    public Data getData() { return this.data; }",
                "}",
                "",
                "public class Application {",
                "    public static void main(String[] args) {",
                "        System.out.println(new Vertex<>(42).getData().equals(42));",
                "    }",
                "}");

        final JavaParserFacade facade = JavaParserFacade.get(new ReflectionTypeSolver(false));

        StaticJavaParser.parse(source)
                .accept(
                        new VoidVisitorAdapter<Void>() {
                            @Override
                            public void visit(final MethodCallExpr n, final Void arg) {
                                super.visit(n, arg);

                                try {
                                    System.out.printf("Node: %s, solved Type: %s%n", n, facade.solve(n));
                                } catch (RuntimeException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        null);
    }
}

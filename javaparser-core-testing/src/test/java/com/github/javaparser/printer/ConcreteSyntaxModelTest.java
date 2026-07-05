/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.StaticJavaParser.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcreteSyntaxModelTest {

    private String print(Node node) {
        return ConcreteSyntaxModel.genericPrettyPrint(node);
    }

    @Test
    void printSimpleClassExpr() {
        ClassExpr expr = parseExpression("Foo.class");
        assertEquals("Foo.class", print(expr));
    }

    @Test
    void printArrayClassExpr() {
        ClassExpr expr = parseExpression("Foo[].class");
        assertEquals("Foo[].class", print(expr));
    }

    @Test
    void printGenericClassExpr() {
        ClassExpr expr = parseExpression("Foo<String>.class");
        assertEquals("Foo<String>.class", print(expr));
    }

    @Test
    void printSimplestClass() {
        Node node = parse("class A {}");
        assertEquals("class A {" + LineSeparator.SYSTEM + "}" + LineSeparator.SYSTEM, print(node));
    }

    @Test
    void printAClassWithField() {
        Node node = parse("class A { int a; }");
        assertEquals(
                "class A {" + LineSeparator.SYSTEM
                        + LineSeparator.SYSTEM + "    int a;"
                        + LineSeparator.SYSTEM + "}"
                        + LineSeparator.SYSTEM,
                print(node));
    }

    @Test
    void printParameters() {
        Node node = parseBodyDeclaration("int x(int y, int z) {}");
        assertEquals("int x(int y, int z) {" + LineSeparator.SYSTEM + "}", print(node));
    }

    @Test
    void printReceiverParameter() {
        Node node = parseBodyDeclaration("int x(X A.B.this, int y, int z) {}");
        assertEquals("int x(X A.B.this, int y, int z) {" + LineSeparator.SYSTEM + "}", print(node));
    }

    @Test
    void printAnEmptyInterface() {
        Node node = parse("interface A {}");
        assertEquals("interface A {" + LineSeparator.SYSTEM + "}" + LineSeparator.SYSTEM, print(node));
    }

    @Test
    void printAnEmptyInterfaceWithModifier() {
        Node node = parse("public interface A {}");
        assertEquals("public interface A {" + LineSeparator.SYSTEM + "}" + LineSeparator.SYSTEM, print(node));
    }
}

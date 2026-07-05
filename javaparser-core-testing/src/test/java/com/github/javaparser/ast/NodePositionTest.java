/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParseStart;
import com.github.javaparser.Providers;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodePositionTest {

    private List<Node> getAllNodes(Node node) {
        List<Node> nodes = new LinkedList<>();
        nodes.add(node);
        node.getChildNodes().forEach(c -> nodes.addAll(getAllNodes(c)));
        return nodes;
    }

    @Test
    void packageProtectedClassShouldHavePositionSet() throws IOException {
        ensureAllNodesHaveValidBeginPosition("class A { }");
    }

    @Test
    void packageProtectedInterfaceShouldHavePositionSet() throws IOException {
        ensureAllNodesHaveValidBeginPosition("interface A { }");
    }

    @Test
    void packageProtectedEnumShouldHavePositionSet() throws IOException {
        ensureAllNodesHaveValidBeginPosition("enum A { }");
    }

    @Test
    void packageProtectedAnnotationShouldHavePositionSet() throws IOException {
        ensureAllNodesHaveValidBeginPosition("@interface A { }");
    }

    @Test
    void packageProtectedFieldShouldHavePositionSet() throws IOException {
        ensureAllNodesHaveValidBeginPosition("public class A { int i; }");
    }

    @Test
    void packageProtectedMethodShouldHavePositionSet() throws IOException {
        ensureAllNodesHaveValidBeginPosition("public class A { void foo() {} }");
    }

    @Test
    void packageProtectedConstructorShouldHavePositionSet() throws IOException {
        ensureAllNodesHaveValidBeginPosition("public class A { A() {} }");
    }

    private void ensureAllNodesHaveValidBeginPosition(final String code) {
        ParseResult<CompilationUnit> res =
                new JavaParser().parse(ParseStart.COMPILATION_UNIT, Providers.provider(code));
        assertTrue(res.getProblems().isEmpty());

        CompilationUnit cu = res.getResult().get();
        getAllNodes(cu).forEach(n -> {
            assertNotNull(
                    n.getRange(),
                    String.format(
                            "There should be no node without a range: %s (class: %s)",
                            n, n.getClass().getCanonicalName()));
            if (n.getBegin().get().line == 0 && !n.toString().isEmpty()) {
                throw new IllegalArgumentException("There should be no node at line 0: " + n + " (class: "
                        + n.getClass().getCanonicalName() + ")");
            }
        });
    }
}

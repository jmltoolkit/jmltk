/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.utils;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import java.util.ArrayList;
import java.util.List;

public final class ExtractingVisitors {

    private static <N extends Node> List<N> extract(Node node, GenericVisitor<Void, List<N>> extractCharLiteralExprs) {
        List<N> list = new ArrayList<>();
        node.accept(extractCharLiteralExprs, list);
        return list;
    }

    public static List<CharLiteralExpr> extractCharLiteralExprs(Node node) {
        return extract(node, new GenericVisitorAdapter<Void, List<CharLiteralExpr>>() {
            @Override
            public Void visit(CharLiteralExpr n, List<CharLiteralExpr> accumulator) {
                accumulator.add(n);
                return super.visit(n, accumulator);
            }
        });
    }
}

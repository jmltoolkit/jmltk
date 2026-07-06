/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import static com.github.javaparser.StaticJavaParser.parseExpression;

/**
 * A node that has an expression in it.
 */
public interface NodeWithExpression<N extends Node> {

    Expression getExpression();

    N setExpression(Expression expression);

    default N setExpression(String expression) {
        return setExpression(parseExpression(expression));
    }
}

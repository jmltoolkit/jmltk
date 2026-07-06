/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import java.util.Optional;

/**
 * Represents a node which has an optional scope expression eg. method calls (object.method()).
 */
public interface NodeWithOptionalScope<N extends Node> extends NodeWithTraversableScope {

    Optional<Expression> getScope();

    N setScope(Expression scope);

    N removeScope();

    default Optional<Expression> traverseScope() {
        return getScope();
    }
}

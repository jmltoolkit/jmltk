/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.expr.Expression;

import java.util.Optional;

/**
 * Represents a node which has a scope expression that can be traversed/walked.
 * This unifies scope access for NodeWithScope and NodeWithOptionalScope.
 */
public interface NodeWithTraversableScope {

    /**
     * @return the scope of this node, regardless of optionality.
     * An optional scope is returned directly.
     * A required scope is returned in an "Optional", but will never be empty.
     */
    Optional<Expression> traverseScope();
}

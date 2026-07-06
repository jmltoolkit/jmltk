/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import static com.github.javaparser.utils.Utils.assertNonEmpty;

/**
 * A node with a name.
 * <p>
 * The main reason for this interface is to permit users to manipulate homogeneously all nodes with a getName method.
 */
public interface NodeWithSimpleName<N extends Node> {

    SimpleName getName();

    N setName(SimpleName name);

    @SuppressWarnings("unchecked")
    default N setName(String name) {
        assertNonEmpty(name);
        return setName(new SimpleName(name));
    }

    default String getNameAsString() {
        return getName().getIdentifier();
    }

    default NameExpr getNameAsExpression() {
        return new NameExpr(getName());
    }
}

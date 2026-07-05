/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import static com.github.javaparser.StaticJavaParser.parseName;
import static com.github.javaparser.utils.Utils.assertNonEmpty;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Name;

/**
 * A node with a (qualified) name.
 * <p>
 * The main reason for this interface is to permit users to manipulate homogeneously all nodes with a getName method.
 *
 * @since 2.0.1
 */
public interface NodeWithName<N extends Node> {

    Name getName();

    N setName(Name name);

    @SuppressWarnings("unchecked")
    default N setName(String name) {
        assertNonEmpty(name);
        return setName(parseName(name));
    }

    default String getNameAsString() {
        return getName().asString();
    }
}

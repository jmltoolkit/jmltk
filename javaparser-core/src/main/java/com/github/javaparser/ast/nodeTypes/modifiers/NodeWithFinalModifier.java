/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes.modifiers;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;

import static com.github.javaparser.ast.Modifier.DefaultKeyword.FINAL;

/**
 * A node that can be final.
 */
public interface NodeWithFinalModifier<N extends Node> extends NodeWithModifiers<N> {

    /**
     * @return true, if the modifier {@code final} is explicitly added to this node. If the node is implicitly final
     * without an explicit modifier (e.g. records, and components of a record), this method should be overridden.
     */
    default boolean isFinal() {
        return hasModifier(FINAL);
    }

    default N setFinal(boolean set) {
        return setModifier(FINAL, set);
    }
}

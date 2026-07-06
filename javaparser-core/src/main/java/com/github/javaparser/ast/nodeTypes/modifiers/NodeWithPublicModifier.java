/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes.modifiers;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import static com.github.javaparser.ast.Modifier.DefaultKeyword.PUBLIC;

/**
 * A node that can be public.
 */
public interface NodeWithPublicModifier<N extends Node> extends NodeWithModifiers<N> {

    default boolean isPublic() {
        return hasModifier(PUBLIC);
    }

    @SuppressWarnings("unchecked")
    default N setPublic(boolean set) {
        return setModifier(PUBLIC, set);
    }
}

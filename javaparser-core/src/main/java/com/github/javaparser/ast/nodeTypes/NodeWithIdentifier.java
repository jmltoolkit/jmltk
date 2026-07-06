/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Node;
import static com.github.javaparser.utils.Utils.assertNonEmpty;

public interface NodeWithIdentifier<N extends Node> {

    String getIdentifier();

    N setIdentifier(String identifier);

    default String getId() {
        return getIdentifier();
    }

    default N setId(String identifier) {
        assertNonEmpty(identifier);
        return setIdentifier(identifier);
    }
}

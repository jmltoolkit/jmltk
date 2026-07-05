/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.Node;

class TextElementMatchers {

    static TextElementMatcher byTokenType(int tokenType) {
        return textElement -> textElement.isToken(tokenType);
    }

    static TextElementMatcher byNode(final Node node) {
        return new TextElementMatcher() {

            @Override
            public boolean match(TextElement textElement) {
                return textElement.isNode(node);
            }

            @Override
            public String toString() {
                return "match node " + node;
            }
        };
    }
}

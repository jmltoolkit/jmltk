/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.jml2java;

import com.github.javaparser.ast.Node;

public class VerifyState {
    public static void verify(Node n) {
        for (Node c : n.getChildNodes()) {
            verify(c);
            if (c.getParentNode() == null || c.getParentNode().get() != n) {
                throw new IllegalStateException("Broken parent relation for node: " + c);
            }
        }
    }
}

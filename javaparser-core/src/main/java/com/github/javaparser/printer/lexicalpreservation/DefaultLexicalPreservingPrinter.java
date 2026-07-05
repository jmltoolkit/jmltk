/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.Printer;

public class DefaultLexicalPreservingPrinter implements Printer {

    /**
     * Print a Node into a String, preserving the lexical information.
     */
    @Override
    public String print(Node node) {
        LexicalPreservingVisitor visitor = new LexicalPreservingVisitor();
        final NodeText nodeText = LexicalPreservingPrinter.getOrCreateNodeText(node);
        nodeText.getElements().forEach(element -> element.accept(visitor));
        return visitor.toString();
    }
}

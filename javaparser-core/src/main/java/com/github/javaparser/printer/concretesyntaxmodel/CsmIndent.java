/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.concretesyntaxmodel;

import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.SourcePrinter;
import com.github.javaparser.printer.lexicalpreservation.TextElement;
import com.github.javaparser.printer.lexicalpreservation.TokenTextElement;

public class CsmIndent implements CsmElement {

    @Override
    public void prettyPrint(Node node, SourcePrinter printer) {
        printer.indent();
    }

    /*
     * Verifies if the content of the {@code CsmElement} is the same as the provided {@code TextElement}
     */
    @Override
    public boolean isCorrespondingElement(TextElement textElement) {
        return (textElement instanceof TokenTextElement) && ((TokenTextElement) textElement).isSpaceOrTab();
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CsmIndent;
    }
}

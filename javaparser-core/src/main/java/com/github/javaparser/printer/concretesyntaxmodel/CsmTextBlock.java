/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.concretesyntaxmodel;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.printer.SourcePrinter;

public class CsmTextBlock implements CsmElement {

    private final ObservableProperty property;

    public CsmTextBlock(ObservableProperty property) {
        this.property = property;
    }

    public ObservableProperty getProperty() {
        return property;
    }

    @Override
    public void prettyPrint(Node node, SourcePrinter printer) {
        printer.print("\"\"\"");
        // Per https://openjdk.java.net/jeps/378#1--Line-terminators, any 'CRLF' and 'CR' are turned into 'LF' before
        // interpreting the text
        printer.println();
        // TODO: Confirm if we need to force this to use {@code \n} separators
        printer.print(property.getValueAsStringAttribute(node));
        printer.print("\"\"\"");
    }

    @Override
    public String toString() {
        return String.format("%s(property:%s)", this.getClass().getSimpleName(), getProperty());
    }
}

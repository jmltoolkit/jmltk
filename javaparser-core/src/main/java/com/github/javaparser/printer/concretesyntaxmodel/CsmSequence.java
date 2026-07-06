/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.concretesyntaxmodel;

import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.SourcePrinter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CsmSequence implements CsmElement {

    private List<CsmElement> elements;

    public CsmSequence(List<CsmElement> elements) {
        if (elements == null) {
            throw new NullPointerException();
        }
        if (elements.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Null element in the sequence");
        }
        this.elements = elements;
    }

    public List<CsmElement> getElements() {
        return elements;
    }

    @Override
    public void prettyPrint(Node node, SourcePrinter printer) {
        elements.forEach(e -> e.prettyPrint(node, printer));
    }

    @Override
    public String toString() {
        return elements.stream().map(e -> e.toString()).collect(Collectors.joining(",", "CsmSequence[", "]"));
    }
}

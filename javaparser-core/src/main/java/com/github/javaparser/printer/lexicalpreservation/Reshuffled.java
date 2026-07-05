/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.printer.concretesyntaxmodel.CsmElement;
import com.github.javaparser.printer.concretesyntaxmodel.CsmMix;
import com.github.javaparser.printer.concretesyntaxmodel.CsmToken;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elements in a CsmMix have been reshuffled. It could also mean that
 * some new elements have been added or removed to the mix.
 */
public class Reshuffled implements DifferenceElement {

    private final CsmMix previousOrder;

    private final CsmMix nextOrder;

    Reshuffled(CsmMix previousOrder, CsmMix nextOrder) {
        this.previousOrder = previousOrder;
        this.nextOrder = nextOrder;
    }

    @Override
    public String toString() {
        return "Reshuffled{" + nextOrder + ", previous=" + previousOrder + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reshuffled that = (Reshuffled) o;
        if (!previousOrder.equals(that.previousOrder)) return false;
        return nextOrder.equals(that.nextOrder);
    }

    @Override
    public int hashCode() {
        int result = previousOrder.hashCode();
        result = 31 * result + nextOrder.hashCode();
        return result;
    }

    @Override
    public CsmMix getElement() {
        return nextOrder;
    }

    public CsmMix getPreviousOrder() {
        return previousOrder;
    }

    public CsmMix getNextOrder() {
        return nextOrder;
    }

    @Override
    public boolean isAdded() {
        return false;
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    @Override
    public boolean isKept() {
        return false;
    }

    /*
     * If the {@code DifferenceElement} wraps an EOL token then this method returns a new {@code DifferenceElement}
     * with all eof token replaced by the specified line separator. The line separator parameter must be a CsmToken with a valid line separator.
     */
    @Override
    public DifferenceElement replaceEolTokens(CsmElement lineSeparator) {
        CsmMix modifiedNextOrder = new CsmMix(replaceTokens(nextOrder.getElements(), lineSeparator));
        CsmMix modifiedPreviousOrder = new CsmMix(replaceTokens(previousOrder.getElements(), lineSeparator));
        return new Reshuffled(modifiedPreviousOrder, modifiedNextOrder);
    }

    /*
     * Replaces all eol tokens in the list by the specified line separator token
     */
    private List<CsmElement> replaceTokens(List<CsmElement> elements, CsmElement lineSeparator) {
        return elements.stream()
                .map(element -> isNewLineToken(element) ? lineSeparator : element)
                .collect(Collectors.toList());
    }

    /*
     * Return true if the wrapped {@code CsmElement} is a new line token
     */
    private boolean isNewLineToken(CsmElement element) {
        return isToken(element) && ((CsmToken) element).isNewLine();
    }

    private boolean isToken(CsmElement element) {
        return element instanceof CsmToken;
    }
}

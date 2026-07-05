/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.printer.concretesyntaxmodel.CsmElement;

public interface DifferenceElement {

    static DifferenceElement added(CsmElement element) {
        return new Added(element);
    }

    static DifferenceElement removed(CsmElement element) {
        return new Removed(element);
    }

    static DifferenceElement kept(CsmElement element) {
        return new Kept(element);
    }

    /**
     * Return the CsmElement considered in this DifferenceElement.
     */
    CsmElement getElement();

    boolean isAdded();

    boolean isRemoved();

    boolean isKept();

    default boolean isChild() {
        return getElement() instanceof LexicalDifferenceCalculator.CsmChild;
    }

    /*
     * If the {@code DifferenceElement} wraps an EOL token then this method returns a new wrapped {@code CsmElement}
     * with the specified line separator. The line separator parameter must be a {@code CsmToken} with a valid line
     * separator. By default this method returns the instance itself.
     */
    default DifferenceElement replaceEolTokens(CsmElement lineSeparator) {
        return this;
    }
}

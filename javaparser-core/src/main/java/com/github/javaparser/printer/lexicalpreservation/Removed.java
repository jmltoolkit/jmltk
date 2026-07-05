/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.printer.concretesyntaxmodel.CsmElement;
import com.github.javaparser.printer.concretesyntaxmodel.CsmToken;

public class Removed implements DifferenceElement {

    private final CsmElement element;

    Removed(CsmElement element) {
        this.element = element;
    }

    @Override
    public String toString() {
        return "Removed{" + element + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Removed removed = (Removed) o;
        return element.equals(removed.element);
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }

    @Override
    public CsmElement getElement() {
        return element;
    }

    public Node getChild() {
        if (isChild()) {
            LexicalDifferenceCalculator.CsmChild csmChild = (LexicalDifferenceCalculator.CsmChild) element;
            return csmChild.getChild();
        }
        throw new IllegalStateException(
                "Removed is not a " + LexicalDifferenceCalculator.CsmChild.class.getSimpleName());
    }

    public int getTokenType() {
        if (isToken()) {
            CsmToken csmToken = (CsmToken) element;
            return csmToken.getTokenType();
        }
        throw new IllegalStateException("Removed is not a " + CsmToken.class.getSimpleName());
    }

    @Override
    public boolean isAdded() {
        return false;
    }

    @Override
    public boolean isRemoved() {
        return true;
    }

    @Override
    public boolean isKept() {
        return false;
    }

    public boolean isToken() {
        return element instanceof CsmToken;
    }

    public boolean isPrimitiveType() {
        if (isChild()) {
            LexicalDifferenceCalculator.CsmChild csmChild = (LexicalDifferenceCalculator.CsmChild) element;
            return csmChild.getChild() instanceof PrimitiveType;
        }
        return false;
    }

    public boolean isWhiteSpace() {
        if (isToken()) {
            CsmToken csmToken = (CsmToken) element;
            return csmToken.isWhiteSpace();
        }
        return false;
    }

    public boolean isWhiteSpaceNotEol() {
        if (isToken()) {
            CsmToken csmToken = (CsmToken) element;
            return csmToken.isWhiteSpaceNotEol();
        }
        return false;
    }

    public boolean isNewLine() {
        if (isToken()) {
            CsmToken csmToken = (CsmToken) element;
            return csmToken.isNewLine();
        }
        return false;
    }
}

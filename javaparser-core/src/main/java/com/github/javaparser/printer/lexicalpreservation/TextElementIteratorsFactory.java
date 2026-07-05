/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

class TextElementIteratorsFactory {

    static class CascadingIterator<E> implements Iterator<E> {

        interface Provider<E> {

            Iterator<E> provide();
        }

        private final Provider<E> nextProvider;

        private Iterator<E> current;

        private Iterator<E> next;

        private boolean lastReturnedFromCurrent = false;

        private boolean lastReturnedFromNext = false;

        public CascadingIterator(Iterator<E> current, Provider<E> nextProvider) {
            this.nextProvider = nextProvider;
            this.current = current;
        }

        @Override
        public boolean hasNext() {
            if (current.hasNext()) {
                return true;
            }
            if (next == null) {
                next = nextProvider.provide();
            }
            return next.hasNext();
        }

        @Override
        public E next() {
            if (current.hasNext()) {
                lastReturnedFromCurrent = true;
                lastReturnedFromNext = false;
                return current.next();
            }
            if (next == null) {
                next = nextProvider.provide();
            }
            lastReturnedFromCurrent = false;
            lastReturnedFromNext = true;
            return next.next();
        }

        @Override
        public void remove() {
            if (lastReturnedFromCurrent) {
                current.remove();
                return;
            }
            if (lastReturnedFromNext) {
                next.remove();
                return;
            }
            throw new IllegalArgumentException();
        }
    }

    static class EmptyIterator<E> implements Iterator<E> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public E next() {
            throw new IllegalArgumentException();
        }
    }

    private static class SingleElementIterator<E> implements Iterator<E> {

        private final E element;

        private boolean returned;

        SingleElementIterator(E element) {
            this.element = element;
        }

        @Override
        public boolean hasNext() {
            return !returned;
        }

        @Override
        public E next() {
            returned = true;
            return element;
        }

        @Override
        public void remove() {}
    }

    static class ComposedIterator<E> implements Iterator<E> {

        private final List<Iterator<E>> elements;

        private int currIndex;

        ComposedIterator(List<Iterator<E>> elements) {
            this.elements = elements;
            currIndex = 0;
        }

        @Override
        public boolean hasNext() {
            if (currIndex >= elements.size()) {
                return false;
            }
            if (elements.get(currIndex).hasNext()) {
                return true;
            }
            currIndex++;
            return hasNext();
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new IllegalArgumentException();
            }
            return elements.get(currIndex).next();
        }

        @Override
        public void remove() {
            elements.get(currIndex).remove();
        }
    }

    private static Iterator<TokenTextElement> reverseIterator(NodeText nodeText, int index) {
        TextElement textElement = nodeText.getTextElement(index);
        if (textElement instanceof TokenTextElement) {
            return new SingleElementIterator<TokenTextElement>((TokenTextElement) textElement) {

                @Override
                public void remove() {
                    nodeText.removeElement(index);
                }
            };
        }
        if (textElement instanceof ChildTextElement) {
            ChildTextElement childTextElement = (ChildTextElement) textElement;
            NodeText textForChild = childTextElement.getNodeTextForWrappedNode();
            return reverseIterator(textForChild);
        }
        throw new IllegalArgumentException();
    }

    public static Iterator<TokenTextElement> reverseIterator(NodeText nodeText) {
        return partialReverseIterator(nodeText, nodeText.numberOfElements() - 1);
    }

    public static Iterator<TokenTextElement> partialReverseIterator(NodeText nodeText, int fromIndex) {
        List<Iterator<TokenTextElement>> elements = new LinkedList<>();
        for (int i = fromIndex; i >= 0; i--) {
            elements.add(reverseIterator(nodeText, i));
        }
        return new ComposedIterator<>(elements);
    }
}

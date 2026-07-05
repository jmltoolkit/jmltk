/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.changes;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.observer.ObservableProperty;
import java.util.Optional;

/**
 * The removal of an element from a list.
 */
public class ListRemovalChange implements Change {

    private final ObservableProperty observableProperty;

    private final int index;

    public ListRemovalChange(ObservableProperty observableProperty, int index) {
        this.observableProperty = observableProperty;
        this.index = index;
    }

    @Override
    public Object getValue(ObservableProperty property, Node node) {
        if (property == observableProperty) {
            Object currentRawValue = new NoChange().getValue(property, node);
            if (currentRawValue instanceof Optional) {
                Optional<?> optional = (Optional<?>) currentRawValue;
                currentRawValue = optional.orElse(null);
            }
            if (!(currentRawValue instanceof NodeList)) {
                throw new IllegalStateException(
                        "Expected NodeList, found " + currentRawValue.getClass().getCanonicalName());
            }
            NodeList<Node> currentNodeList = (NodeList<Node>) currentRawValue;
            // Note: When adding to a node list children get assigned the list's parent, thus we must set the list's
            // parent before adding children (#2592).
            NodeList<Node> newNodeList = new NodeList<>();
            // fix #2187 set the parent node in the new list
            newNodeList.setParentNode(currentNodeList.getParentNodeForChildren());
            // Here we want to obtain a sub-list that does not contain an element.
            // It is important not to implement this by first adding all the elements in the
            // list and then deleting the element to be removed, as this involves event
            // propagation mechanisms, particularly for lexical preservation,
            // which deletes the relationship between a node and its parent node.
            // This relationship is necessary to reinforce indentation, for example when
            // deleting a node, as indentation can be carried by the parent node.
            currentNodeList.stream()
                    .filter(n -> !isSameNode(currentNodeList.get(index), n))
                    .forEach(selectedNode -> newNodeList.add(selectedNode));
            return newNodeList;
        }
        return new NoChange().getValue(property, node);
    }

    private boolean isSameNode(Node n1, Node n2) {
        return n1.equals(n2) && isSameRange(n1, n2);
    }

    private boolean isSameRange(Node n1, Node n2) {
        return (!n1.hasRange() && !n2.hasRange())
                || (n1.hasRange()
                        && n2.hasRange()
                        && isSameRange(n1.getRange().get(), n2.getRange().get()));
    }

    private boolean isSameRange(Range r1, Range r2) {
        return r1.equals(r2);
    }

    @Override
    public ObservableProperty getProperty() {
        return observableProperty;
    }
}

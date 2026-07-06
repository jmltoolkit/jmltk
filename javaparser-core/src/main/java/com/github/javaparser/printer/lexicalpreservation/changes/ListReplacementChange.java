/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.changes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.observer.ObservableProperty;
import java.util.Optional;

/**
 * The replacement of an element in a list.
 */
public class ListReplacementChange implements Change {

    private final ObservableProperty observableProperty;

    private final int index;

    private final Node newValue;

    public ListReplacementChange(ObservableProperty observableProperty, int index, Node newValue) {
        this.observableProperty = observableProperty;
        this.index = index;
        this.newValue = newValue;
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
                throw new IllegalStateException("Expected NodeList, found " + currentRawValue.getClass().getCanonicalName());
            }
            NodeList<Node> currentNodeList = (NodeList<Node>) currentRawValue;
            // Note: When adding to a node list children get assigned the list's parent, thus we must set the list's
            // parent before adding children (#2592).
            NodeList<Node> newNodeList = new NodeList<>();
            newNodeList.setParentNode(currentNodeList.getParentNodeForChildren());
            newNodeList.addAll(currentNodeList);
            // Perform modification -- replace an item in the list
            newNodeList.set(index, newValue);
            return newNodeList;
        }
        return new NoChange().getValue(property, node);
    }

    @Override
    public ObservableProperty getProperty() {
        return observableProperty;
    }
}

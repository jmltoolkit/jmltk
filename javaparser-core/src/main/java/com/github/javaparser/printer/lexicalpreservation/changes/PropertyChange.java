/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.changes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.observer.ObservableProperty;

/**
 * The change in value of a property.
 */
public class PropertyChange implements Change {

    private final ObservableProperty property;

    private final Object oldValue;

    private final Object newValue;

    public PropertyChange(ObservableProperty property, Object oldValue, Object newValue) {
        this.property = property;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public ObservableProperty getProperty() {
        return property;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    @Override
    public Object getValue(ObservableProperty property, Node node) {
        if (property == this.property) {
            return newValue;
        }
        return property.getRawValue(node);
    }
}

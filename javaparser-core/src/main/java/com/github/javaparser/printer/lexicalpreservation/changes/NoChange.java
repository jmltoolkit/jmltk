/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.changes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.observer.ObservableProperty;

/**
 * No change. The Node is not mutated.
 */
public class NoChange implements Change {

    @Override
    public Object getValue(ObservableProperty property, Node node) {
        return property.getRawValue(node);
    }

    @Override
    public ObservableProperty getProperty() {
        return null;
    }
}

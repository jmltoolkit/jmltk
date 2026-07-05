/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.concretesyntaxmodel;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.printer.SourcePrinter;
import java.util.Arrays;
import java.util.List;

public class CsmConditional implements CsmElement {

    private final Condition condition;

    private final List<ObservableProperty> properties;

    private final CsmElement thenElement;

    private final CsmElement elseElement;

    public Condition getCondition() {
        return condition;
    }

    public ObservableProperty getProperty() {
        if (properties.size() > 1) {
            throw new IllegalStateException();
        }
        return properties.get(0);
    }

    public List<ObservableProperty> getProperties() {
        return properties;
    }

    public CsmElement getThenElement() {
        return thenElement;
    }

    public CsmElement getElseElement() {
        return elseElement;
    }

    public enum Condition {
        IS_EMPTY {

            @Override
            boolean evaluate(Node node, ObservableProperty property) {
                NodeList<? extends Node> value = property.getValueAsMultipleReference(node);
                return value == null || value.isEmpty();
            }
        },
        IS_NOT_EMPTY {

            @Override
            boolean evaluate(Node node, ObservableProperty property) {
                NodeList<? extends Node> value = property.getValueAsMultipleReference(node);
                return value != null && !value.isEmpty();
            }
        },
        IS_PRESENT {

            @Override
            boolean evaluate(Node node, ObservableProperty property) {
                return !property.isNullOrNotPresent(node);
            }
        },
        FLAG {

            @Override
            boolean evaluate(Node node, ObservableProperty property) {
                return property.getValueAsBooleanAttribute(node);
            }
        };

        abstract boolean evaluate(Node node, ObservableProperty property);
    }

    public CsmConditional(
            ObservableProperty property, Condition condition, CsmElement thenElement, CsmElement elseElement) {
        this.properties = Arrays.asList(property);
        this.condition = condition;
        this.thenElement = thenElement;
        this.elseElement = elseElement;
    }

    public CsmConditional(
            List<ObservableProperty> properties, Condition condition, CsmElement thenElement, CsmElement elseElement) {
        if (properties.size() < 1) {
            throw new IllegalArgumentException();
        }
        this.properties = properties;
        this.condition = condition;
        this.thenElement = thenElement;
        this.elseElement = elseElement;
    }

    public CsmConditional(ObservableProperty property, Condition condition, CsmElement thenElement) {
        this(property, condition, thenElement, new CsmNone());
    }

    @Override
    public void prettyPrint(Node node, SourcePrinter printer) {
        boolean test = false;
        for (ObservableProperty prop : properties) {
            test = test || condition.evaluate(node, prop);
        }
        if (test) {
            thenElement.prettyPrint(node, printer);
        } else {
            elseElement.prettyPrint(node, printer);
        }
    }
}

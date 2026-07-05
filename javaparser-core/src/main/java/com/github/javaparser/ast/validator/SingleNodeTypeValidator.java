/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import com.github.javaparser.ast.Node;

/**
 * Runs a validator on all nodes of a certain type.
 */
public class SingleNodeTypeValidator<N extends Node> implements Validator {

    private final Class<N> type;

    private final TypedValidator<N> validator;

    public SingleNodeTypeValidator(Class<N> type, TypedValidator<N> validator) {
        this.type = type;
        this.validator = validator;
    }

    @Override
    public void accept(Node node, ProblemReporter problemReporter) {
        if (type.isInstance(node)) {
            validator.accept(type.cast(node), problemReporter);
        }
        node.findAll(type).forEach(n -> validator.accept(n, problemReporter));
    }
}

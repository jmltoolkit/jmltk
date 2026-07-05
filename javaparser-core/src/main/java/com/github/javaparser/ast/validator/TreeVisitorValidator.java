/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import com.github.javaparser.ast.Node;

/**
 * A validator that walks the whole tree, visiting every node.
 */
public class TreeVisitorValidator implements Validator {

    private final Validator validator;

    public TreeVisitorValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public final void accept(Node node, ProblemReporter reporter) {
        validator.accept(node, reporter);
        for (Node child : node.getChildNodes()) {
            accept(child, reporter);
        }
    }
}

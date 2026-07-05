/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;

/**
 * Validates that "record" cannot be used as identifier for type declarations (e.g., classes, enums, and records).
 * For details, see <a href="https://openjdk.java.net/jeps/395">JEP 395</a>
 */
public class RecordAsTypeIdentifierNotAllowed extends VisitorValidator {

    private final String error;

    public RecordAsTypeIdentifierNotAllowed() {
        error = "'record' is a restricted identifier and cannot be used for type declarations";
    }

    @Override
    public void visit(Name n, ProblemReporter arg) {
        if ("record".equals(n.getIdentifier()) && !validUsage(n)) {
            arg.report(n, error);
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(SimpleName n, ProblemReporter arg) {
        if ("record".equals(n.getIdentifier()) && !validUsage(n)) {
            arg.report(n, error);
        }
        super.visit(n, arg);
    }

    private boolean validUsage(Node node) {
        if (!node.getParentNode().isPresent()) {
            return true;
        }
        Node parent = node.getParentNode().get();
        return !(parent instanceof TypeDeclaration);
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.validator.SingleNodeTypeValidator;

/**
 * This validator validates according to Java 7 syntax rules.
 */
public class Java7Validator extends Java6Validator {

    final SingleNodeTypeValidator<TryStmt> tryWithLimitedResources =
            new SingleNodeTypeValidator<>(TryStmt.class, (n, reporter) -> {
                if (n.getCatchClauses().isEmpty()
                        && n.getResources().isEmpty()
                        && !n.getFinallyBlock().isPresent()) {
                    reporter.report(n, "Try has no finally, no catch, and no resources.");
                }
                for (Expression resource : n.getResources()) {
                    if (!resource.isVariableDeclarationExpr()) {
                        reporter.report(n, "Try with resources only supports variable declarations.");
                    }
                }
            });

    private final SingleNodeTypeValidator<UnionType> multiCatch =
            new SingleNodeTypeValidator<>(UnionType.class, (n, reporter) -> {
                // Case "0 elements" is caught elsewhere.
                if (n.getElements().size() == 1) {
                    reporter.report(n, "Union type (multi catch) must have at least two elements.");
                }
            });

    public Java7Validator() {
        super();
        remove(genericsWithoutDiamondOperator);
        replace(tryWithoutResources, tryWithLimitedResources);
        remove(noBinaryIntegerLiterals);
        remove(noUnderscoresInIntegerLiterals);
        replace(noMultiCatch, multiCatch);
    }
}

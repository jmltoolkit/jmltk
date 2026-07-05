/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations.chunks;

import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.validator.ProblemReporter;
import com.github.javaparser.ast.validator.VisitorValidator;

public class NoUnderscoresInIntegerLiteralsValidator extends VisitorValidator {

    @Override
    public void visit(IntegerLiteralExpr n, ProblemReporter arg) {
        validate(n, arg);
        super.visit(n, arg);
    }

    @Override
    public void visit(LongLiteralExpr n, ProblemReporter arg) {
        validate(n, arg);
        super.visit(n, arg);
    }

    private static void validate(LiteralStringValueExpr n, ProblemReporter arg) {
        if (n.getValue().contains("_")) {
            arg.report(n, "Underscores in literal values are not supported.");
        }
    }
}

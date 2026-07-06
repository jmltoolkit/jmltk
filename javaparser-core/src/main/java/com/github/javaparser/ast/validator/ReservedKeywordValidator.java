/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;

import static com.github.javaparser.utils.CodeGenerationUtils.f;

/**
 * Validates that identifiers are not keywords - this for the few keywords that the parser
 * accepts because they were added after Java 1.0.
 */
public class ReservedKeywordValidator extends VisitorValidator {

    private final String keyword;

    private final String error;

    public ReservedKeywordValidator(String keyword) {
        this.keyword = keyword;
        error = f("'%s' cannot be used as an identifier as it is a keyword.", keyword);
    }

    @Override
    public void visit(Name n, ProblemReporter arg) {
        if (n.getIdentifier().equals(keyword)) {
            arg.report(n, error);
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(SimpleName n, ProblemReporter arg) {
        if (n.getIdentifier().equals(keyword)) {
            arg.report(n, error);
        }
        super.visit(n, arg);
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations.chunks;

import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.validator.ProblemReporter;
import com.github.javaparser.ast.validator.TypedValidator;

public class SwitchExprValidator implements TypedValidator<SwitchExpr> {

    @Override
    public void accept(SwitchExpr node, ProblemReporter reporter) {
        validateHasResultExpressions(node, reporter);
    }

    /**
     * "It is a compile-time error if a switch expression has no result expressions." (JLS 15.28.1)
     * A result expression is a non-throwing switch rule - if all switch rules throw,
     * there are no result expressions.
     */
    private void validateHasResultExpressions(SwitchExpr n, ProblemReporter reporter) {
        boolean allThrow =
                n.getEntries().stream().allMatch(entry -> entry.getType() == SwitchEntry.Type.THROWS_STATEMENT);
        if (allThrow) {
            reporter.report(n, "Switch expression does not have any result expressions.");
        }
    }
}

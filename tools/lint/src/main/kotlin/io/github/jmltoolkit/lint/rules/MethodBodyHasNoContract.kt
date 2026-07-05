/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.validator.ProblemReporter
import com.github.javaparser.ast.validator.VisitorValidator

class MethodBodyHasNoContract : VisitorValidator() {
    override fun visit(n: MethodDeclaration, arg: ProblemReporter) {
        if (n.body.isPresent && !n.body.get().contracts.isEmpty()) {
            arg.report(n, "Body of method has a block contract.")
        }
        super.visit(n, arg)
    }
}

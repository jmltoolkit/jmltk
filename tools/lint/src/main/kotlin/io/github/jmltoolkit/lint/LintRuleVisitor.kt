/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

/**
 * @author Alexander Weigl
 * @version 1 (13.10.22)
 */
abstract class LintRuleVisitor :
    VoidVisitorAdapter<LintProblemReporter>(),
    LintRule {
    /**
     * A validator that uses a visitor for validation.
     * This class is the visitor too.
     * Implement the "visit" methods you want to use for validation.
     */
    override fun accept(node: Node, problemReporter: LintProblemReporter, config: JmlLintingConfig) {
        reset()
        node.accept(this, problemReporter)
    }

    protected open fun reset() {
    }
}

/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint

import com.github.javaparser.ast.Node

/**
 * @author Alexander Weigl
 * @version 1 (12/29/21)
 */
interface LintRule {
    fun accept(node: Node, problemReporter: LintProblemReporter, config: JmlLintingConfig)

    companion object {
        const val HINT: String = "HINT"
        const val WARN: String = "WARN"
        const val ERROR: String = "ERROR"
    }
}

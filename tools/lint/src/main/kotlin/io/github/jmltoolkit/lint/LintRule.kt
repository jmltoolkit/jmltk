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
abstract class LintRule {
    protected lateinit var problemReporter: LintProblemReporter
    protected lateinit var config: JmlLintingConfig

    fun init(problemReporter: LintProblemReporter, config: JmlLintingConfig) {
        this.problemReporter = problemReporter
        this.config = config
        customInitialization()
    }

    open fun customInitialization() {}

    abstract fun accept(node: Node)

    companion object {
        const val HINT: String = "HINT"
        const val WARN: String = "WARN"
        const val ERROR: String = "ERROR"
    }
}

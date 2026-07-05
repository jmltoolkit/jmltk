/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import io.github.jmltoolkit.lint.LintRuleVisitor

/**
 * @author Alexander Weigl
 * @version 1 (12/29/21)
 */
object LocationSetValidator : LintRuleVisitor() {
    const val ASSIGNABLE_ARRAY_ONLY: String = "You can only use '[*]' on arrays"
    const val ASSIGNABLE_CLASS_ONLY: String = "You can only use '.*' on classes and interfaces"
}

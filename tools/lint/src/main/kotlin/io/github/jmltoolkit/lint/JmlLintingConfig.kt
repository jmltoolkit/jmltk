/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint

import io.github.jmltoolkit.lint.rules.DefaultNullity

/**
 * @author Alexander Weigl
 * @version 1 (12/29/21)
 */
data class JmlLintingConfig(
    val checkNameClashes: Boolean = true,
    val checkMissingNames: Boolean = true,
    /**
     * The default nullity for a top-level class (JML Reference Manual, §3.4).
     * The standard default is `non_null_by_default`, but it may be altered by tools.
     */
    val topLevelNullity: DefaultNullity = DefaultNullity.NON_NULL,
) {
    fun isDisabled(lintRule: LintRule): Boolean = false
}

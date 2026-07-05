/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint

import com.github.javaparser.TokenRange

/**
 * @author Alexander Weigl
 * @version 1 (13.10.22)
 */
data class LintProblem(
    val level: String,
    val message: String,
    val location: TokenRange?,
    val cause: Throwable?,
    val category: String?,
    val ruleId: String
) {
    constructor(level: String, message: String, location: TokenRange?, ruleId: String) : this(
        level, message, location,
        null, null, ruleId
    )
}

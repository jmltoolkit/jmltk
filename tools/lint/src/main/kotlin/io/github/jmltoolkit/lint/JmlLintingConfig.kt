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
    /**
     * Whether the JSpecify nullness annotations (`@NullMarked`, `@NullUnmarked`,
     * `@NonNullApi`, `@NonNullFields`) should be taken into account by the nullity
     * lint rules, with their semantics. Disabled by default; when disabled, JSpecify
     * annotations are ignored and only the JML default nullity modifiers and their
     * annotation forms are checked.
     */
    val checkJspecifyNullness: Boolean = false,
    /**
     * Whether the visibility of names referenced in JML specifications should be
     * checked against the privacy level of the specification context: an expression
     * in a context of a given privacy level may only refer to names at that level
     * or more visible, and only where Java visibility rules also permit it
     * (e.g. a public invariant may not mention a private field).
     */
    val checkSpecVisibility: Boolean = true,
) {
    fun isDisabled(lintRule: LintRule): Boolean = false
}

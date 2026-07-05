/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.redux

/**
 * Configuration for the Redux transformation pipeline.
 */
class ReduxConfig {
    private val disabled: MutableSet<String> = HashSet()

    fun enable(clazz: Class<*>) {
        disabled.remove(clazz.toString())
    }

    fun disable(clazz: Class<*>) {
        disabled.add(clazz.toString())
    }

    fun getDisabled(): Set<String> = disabled

    fun isEnabled(clazz: String): Boolean = !disabled.contains(clazz)
}

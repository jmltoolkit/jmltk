/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.smt

/**
 *
 * @author Alexander Weigl
 * @version 1 (28.01.24)
 */
object Z3 {
    private var z3Installed: Boolean? = null
    fun z3Installed(): Boolean {
        if (z3Installed != null) return z3Installed!!
        z3Installed = try {
            // `sh`/`which` do not exist on Windows; probe the platform PATH
            // directly (`where z3`) instead of going through a shell.
            val probe = if (isWindows()) listOf("where", "z3") else listOf("sh", "-c", "which z3")
            ProcessBuilder(probe).start().waitFor() == 0
        } catch (e: Exception) {
            false
        }
        return z3Installed!!
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name", "").lowercase().contains("win")
}

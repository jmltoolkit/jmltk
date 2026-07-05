/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

/**
 * @author Alexander Weigl
 * @version 1 (09.04.23)
 */
class LintCommand : CliktCommand(name = "lint") {
    override fun help(context: Context): String =
        "Submit usage for a given customer and subscription, accepts one usage item"

    override fun run() {
        TODO("Not yet implemented")
    }
}

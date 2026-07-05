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

class XPathCommand : CliktCommand(name = "xpath") {
    override fun help(context: Context): String = "Evaluate XPath queries on a set of Java/JML files."

    override fun run() {
        TODO("Not yet implemented")
    }
}

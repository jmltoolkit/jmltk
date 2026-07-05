/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.cli

import com.github.ajalt.clikt.core.Context
import com.github.javaparser.ParserConfiguration
import io.github.jmltoolkit.wd.WDVisitor
import java.util.*

/**
 * @author Alexander Weigl
 * @version 1 (09.04.23)
 */
class WdCommand : FileBasedCommand(name = "wd") {
    override fun help(context: Context): String = "Well-definedness check for JML files."

    override fun run() {
        val config = ParserConfiguration()
        config.setProcessJml(true)
        config.setKeepJmlDocs(true)
        for (activeJmlKey in activeJmlKeys) {
            val activeJmlKeys = activeJmlKey.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            config.jmlKeys.add(Arrays.asList(*activeJmlKeys))
        }

        if (activeJmlKeys.isEmpty()) {
            config.jmlKeys.add(listOf("key"))
        }
        val wd = WDVisitor()
    }
}

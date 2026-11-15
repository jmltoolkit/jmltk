/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.parse
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val command = PpCommand(name = "jmltk-pp")
    try {
        command.parse(args)
    } catch (e: UsageError) {
        System.err.println(e.message ?: "Usage error")
        exitProcess(2)
    } catch (e: CliktError) {
        e.message?.let { message ->
            if (e.printError) System.err.println(message) else println(message)
        }
        exitProcess(e.statusCode)
    }
}

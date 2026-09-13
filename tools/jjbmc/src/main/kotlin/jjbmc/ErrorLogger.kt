/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc

object ErrorLogger {
    private val START_TIME = System.currentTimeMillis()
    const val RESET: String = "\u001b[0m" // Text Reset
    const val RED_BOLD: String = "\u001b[1;31m" // RED
    const val GREEN_BOLD: String = "\u001b[1;32m" // GREEN
    const val YELLOW_BOLD: String = "\u001b[1;33m" // YELLOW

    private var debugOn = false

    private fun print(fmt: String, level: Level?, vararg args: Any?) {
        if (level == Level.DEBUG && !debugOn) return

        System.out.printf("%05d \u001b[1;%dm[%s]\u001b[0m ", System.currentTimeMillis() - START_TIME, 32, level)
        System.out.printf(fmt, *args)
        println()
    }

    fun debug(fmt: String, vararg args: Any?) {
        print(fmt, Level.DEBUG, *args)
    }

    fun debug(obj: Throwable?) {
        print("%s", Level.DEBUG, obj)
    }

    fun info(fmt: Any, vararg args: Any?) {
        print(fmt.toString(), Level.INFO, *args)
    }

    fun fatal(fmt: String, vararg args: Any?) {
        print(fmt, Level.FATAL, *args)
    }

    fun error(fmt: String, vararg args: Any?) {
        print(fmt, Level.ERROR, *args)
    }

    fun warn(fmt: String, vararg args: Any?) {
        print(fmt, Level.WARN, *args)
    }

    fun setDebugOn() {
        debugOn = true
    }
}

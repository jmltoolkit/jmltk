/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import com.github.ajalt.clikt.core.main

fun main(args: Array<String>) = PpCommand(name = "jmltk-pp").main(args)
